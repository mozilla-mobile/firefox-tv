/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.SystemClock
import android.support.annotation.UiThread
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import org.mozilla.tv.firefox.utils.BuildConfigDerivables
import java.util.concurrent.TimeUnit
import kotlin.math.min

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
open class PocketRepo(
    private val pocketEndpoint: PocketEndpoint,
    private val pocketFeedStateMachine: PocketFeedStateMachine,
    buildConfigDerivables: BuildConfigDerivables
) {

    private val mutableState = MutableLiveData<PocketRepoState>().apply {
        // mutableState.value should always be initialized at the top of init,
        // because we treat it as non-null
        value = buildConfigDerivables.initialPocketRepoState
    }
    open val state: LiveData<PocketRepoState> = mutableState

    @Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR") // Private properties generate fields so method annotations can't apply.
    @get:UiThread
    @set:UiThread
    private var backgroundUpdates: Job? = null
    @Volatile private var lastSuccessfulUpdateMillis = -1L
    @Volatile private var lastUpdateAttemptMillis = -1L
    @Volatile private var retryDelayMillis = 1_000L
    @Volatile private var lastAttemptWasSuccessful = false

    fun update() {
        launch { updateInner() }
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

    private suspend fun updateInner() {

        suspend fun requestVideos(): List<PocketFeedItem>? = pocketEndpoint.getRecommendedVideos()

        fun List<PocketFeedItem>?.wasSuccessful() = this?.isNotEmpty() == true

        fun updateRequestTimers(requestSuccessful: Boolean) {
            lastAttemptWasSuccessful = requestSuccessful
            val now = SystemClock.elapsedRealtime()
            lastUpdateAttemptMillis = now
            when (requestSuccessful) {
                true -> {
                    lastSuccessfulUpdateMillis = now
                    retryDelayMillis = 1_000L
                }
                // Exponential backoff on failure
                false -> {
                    val doubled = retryDelayMillis * 2
                    // Do nothing on overflow
                    if (doubled > retryDelayMillis) retryDelayMillis = doubled
                }
            }
        }

        fun postState(newState: PocketVideoRepo.FeedState) {
            val computed = pocketFeedStateMachine.computeNewState(newState, _feedState.value)
            if (_feedState.value !== computed) {
                _feedState.postValue(computed)
            }
        }

        fun List<PocketViewModel.FeedItem>?.toRepoState(): FeedState =
            if (this?.isNotEmpty() == true) FeedState.LoadComplete(this)
            else FeedState.FetchFailed

        postState(FeedState.Loading)
        val response = requestVideos()
        updateRequestTimers(response.wasSuccessful())
        postState(response.toRepoState())
    }

    private fun startBackgroundUpdatesInner() = launch {
        while (true) {
            val nextScheduledUpdateMillis = lastUpdateAttemptMillis + CACHE_UPDATE_FREQUENCY_MILLIS
            val nextRetryUpdateMillis = lastUpdateAttemptMillis + retryDelayMillis
            val nextUpdateMillis = if (lastAttemptWasSuccessful) {
                nextScheduledUpdateMillis
            } else {
                min(nextScheduledUpdateMillis, nextRetryUpdateMillis)
            }

            val delayForMillis = nextUpdateMillis - SystemClock.elapsedRealtime()
            if (delayForMillis > 0) {
                delay(delayForMillis, TimeUnit.MILLISECONDS)
            }
            updateInner()
        }
    }
}
