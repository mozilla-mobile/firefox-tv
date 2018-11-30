/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.SystemClock
import android.support.annotation.UiThread
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.mozilla.tv.firefox.utils.BuildConfigDerivables
import java.util.concurrent.TimeUnit
import kotlin.math.min

private val CACHE_UPDATE_FREQUENCY_MILLIS = TimeUnit.MINUTES.toMillis(45)
private const val BASE_RETRY_TIME = 1_000L

/**
 * Manages backing state for Pocket data, as well as any logic related to
 * retrieving or storing that data.
 */
open class PocketVideoRepo(
    private val pocketEndpoint: PocketEndpoint,
    private val pocketFeedStateMachine: PocketFeedStateMachine,
    private val localeIsEnglish: () -> Boolean,
    buildConfigDerivables: BuildConfigDerivables
) {

    sealed class FeedState {
        data class LoadComplete(val videos: List<PocketViewModel.FeedItem>) : FeedState()
        object Loading : FeedState()
        object NoAPIKey : FeedState()
        object FetchFailed : FeedState()
        object Inactive : FeedState()
    }

    private val _feedState = MutableLiveData<FeedState>().apply {
        // _feedState.value should always be initialized at the top of init,
        // because we treat it as non-null
        value = buildConfigDerivables.initialPocketRepoState
    }
    open val feedState: LiveData<FeedState> = _feedState

    @Suppress("ANNOTATION_TARGETS_NON_EXISTENT_ACCESSOR") // Private properties generate fields so method annotations can't apply.
    @get:UiThread
    @set:UiThread
    private var backgroundUpdates: Job? = null
    @Volatile private var lastSuccessfulUpdateMillis = -1L
    @Volatile private var lastUpdateAttemptMillis = -1L
    @Volatile private var retryDelayMillis = BASE_RETRY_TIME
    @Volatile private var lastAttemptWasSuccessful = false

    fun update() {
        retryDelayMillis = BASE_RETRY_TIME
        GlobalScope.launch { updateInner() }
    }

    @UiThread // update backgroundUpdates.
    fun startBackgroundUpdates() {
        backgroundUpdates?.cancel() // Cancelling unexpectedly active Pocket update job to ensure only one is running
        backgroundUpdates = startBackgroundUpdatesInner()
    }

    // When we the app is not in use, we don't want to hit the network for no reason, so we cancel updates
    @UiThread // stop updating backgroundUpdates.
    fun stopBackgroundUpdates() {
        backgroundUpdates?.cancel()
        backgroundUpdates = null
    }

    private suspend fun updateInner() {

        suspend fun requestVideos(): List<PocketViewModel.FeedItem>? = pocketEndpoint.getRecommendedVideos()

        fun List<PocketViewModel.FeedItem>?.wasSuccessful() = this?.isNotEmpty() == true

        fun updateRequestTimers(requestSuccessful: Boolean) {
            lastAttemptWasSuccessful = requestSuccessful
            val now = SystemClock.elapsedRealtime()
            lastUpdateAttemptMillis = now
            when (requestSuccessful) {
                true -> {
                    lastSuccessfulUpdateMillis = now
                    retryDelayMillis = BASE_RETRY_TIME
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

        fun shortIfNonEnglish() {
            if (!localeIsEnglish.invoke()) {
                postState(FeedState.Inactive)
                return
            }
        }

        shortIfNonEnglish()
        postState(FeedState.Loading)
        val response = requestVideos()
        updateRequestTimers(response.wasSuccessful())
        postState(response.toRepoState())
    }

    private fun startBackgroundUpdatesInner() = GlobalScope.launch {
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
                delay(delayForMillis)
            }
            updateInner()
        }
    }
}
