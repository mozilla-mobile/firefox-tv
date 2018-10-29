/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.SystemClock
import android.support.annotation.UiThread
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.mozilla.tv.firefox.utils.BuildConfigDerivables
import java.util.concurrent.TimeUnit

private val CACHE_UPDATE_FREQUENCY_MILLIS = TimeUnit.MINUTES.toMillis(45)

sealed class PocketRepoState {
    data class LoadComplete(val videos: List<PocketFeedItem>) : PocketRepoState()
    object Loading : PocketRepoState()
    object NoKey : PocketRepoState()
    object FetchFailed : PocketRepoState()
}

/**
 * Manages backing state for Pocket data, as well as any logic related to
 * retrieving or storing that data.
 */
open class PocketRepo(private val pocketEndpoint: PocketEndpoint, buildConfigDerivables: BuildConfigDerivables) {

    companion object {
        fun initAndFetch(pocketEndpoint: PocketEndpoint, buildConfigDerivables: BuildConfigDerivables): PocketRepo {
            return PocketRepo(pocketEndpoint, buildConfigDerivables).apply {
                update()
            }
        }
    }

    private val mutableState = MutableLiveData<PocketRepoState>().apply {
        // mutableState.value should always be initialized at the top of init, because we treat
        // it as non-null
        value = buildConfigDerivables.initialPocketRepoState
    }
    open val state: LiveData<PocketRepoState> = mutableState

    @Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR") // Private properties generate fields so method annotations can't apply.
    @get:UiThread
    @set:UiThread
    private var backgroundUpdates: Job? = null
    @Volatile private var lastUpdateMillis = -1L
    private val nextUpdateMillis get() = lastUpdateMillis + CACHE_UPDATE_FREQUENCY_MILLIS

    fun update() {
        launch {
            val fetchedState = requestVideos()
            updateState(fetchedState)
        }
    }

    @UiThread // update backgroundUpdates.
    fun startBackgroundUpdates() {
        backgroundUpdates?.cancel(CancellationException("Cancelling unexpectedly active job to ensure only one is running"))
        backgroundUpdates = startBackgroundUpdatesInner()
    }

    @UiThread // update backgroundUpdates.
    fun stopBackgroundUpdates() {
        backgroundUpdates?.cancel(CancellationException("Stopping background updates"))
        backgroundUpdates = null
    }

    private fun startBackgroundUpdatesInner() = launch {
        while (true) {
            val delayForMillis = nextUpdateMillis - SystemClock.elapsedRealtime()
            if (delayForMillis > 0) {
                delay(delayForMillis, TimeUnit.MILLISECONDS)
            }

            val newState = requestVideos()
            // Delay next request if the previous was successful, otherwise try again immediately
            if (newState is PocketRepoState.LoadComplete) lastUpdateMillis = SystemClock.elapsedRealtime()
            updateState(newState)
        }
    }

    private suspend fun requestVideos(): PocketRepoState {
        val videos = withContext(DefaultDispatcher) { pocketEndpoint.getRecommendedVideos() }

        return videos?.let { PocketRepoState.LoadComplete(it) } ?: PocketRepoState.FetchFailed
    }

    private fun updateState(newState: PocketRepoState) {
        val oldState = mutableState.value!! // State is initialized at the top of init

        val computedState = PocketRepoStateMachine(newState, oldState).computedState()

        if (oldState != computedState) {
            mutableState.postValue(computedState)
        }
    }
}
