/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val FETCH_UNIQUE_WORK_NAME = "PocketFetch"

/**
 * Schedules background fetches of the Pocket video data.
 */
class PocketVideoFetchScheduler(
    private val isPocketEnabledByLocale: () -> Boolean,
    private val context: Context
) : LifecycleObserver {
    private var qaFetchDelayOverrideMillis: Long? = null
    private var isQABuild = false
    init { resetQAFetchDelayOverrides() } // Replace initial values above to avoid duplication.

    @OnLifecycleEvent(ON_START)
    fun onStart() {
        schedulePocketBackgroundFetch(WorkManager.getInstance(context))
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun schedulePocketBackgroundFetch(
        workManager: WorkManager,
        now: Calendar = Calendar.getInstance(),
        randLong: (Long, Long) -> Long = { from, until -> Random.nextLong(from, until) }
    ) {
        fun getBackoffDelayMillisWithRandomness(): Long {
            return randLong(BACKOFF_DELAY_MIN_MILLIS, BACKOFF_DELAY_MAX_MILLIS)
        }

        // This may not be the best place to early return based on locale - e.g. it duplicates state with the UI -
        // but we're short on time.
        if (!isPocketEnabledByLocale()) {
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // This sets the custom fetch delay for testing purposes.
        // The custom delay will only run once.
        val delay: Long
        val workPolicy: ExistingWorkPolicy
        if (isQABuild) {
            delay = checkNotNull(qaFetchDelayOverrideMillis) { "Fetch delay value must be set" }
            workPolicy = ExistingWorkPolicy.REPLACE
            resetQAFetchDelayOverrides()
        } else {
            delay = getDelayUntilUpcomingNightFetchMillis(now, randLong)
            workPolicy = ExistingWorkPolicy.KEEP
        }

        // When the user foregrounds the app, we schedule a one time update for a random time inside our fetch interval
        // (currently overnight). This will ensure that users always see fresh content every morning and that the content
        // is only refreshing at times when the user is most likely not using the app.
        // The request will always be scheduled for the next night, so if the user foregrounds the app after
        // midnight but before the fetch interval, we schedule for the following night.
        // We only schedule a fetch if there isn't one already scheduled.
        val saveRequest = OneTimeWorkRequestBuilder<PocketVideoFetchWorker>()
            .setConstraints(constraints)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)

            // Here exponential means the first backoff is the given delay, the second backoff is the given delay * 2,
            // the third backoff is the given delay * 2 * 2, etc. Note that WorkManager does not introduce randomness
            // in its backoff algorithm so we must supply our own. This prevents the case where many clients hit the
            // server all at once, overloading the server, then they all back-off and make new requests all at the same
            // time, again overloading the server, and repeat.
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, getBackoffDelayMillisWithRandomness(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(FETCH_UNIQUE_WORK_NAME, workPolicy, saveRequest)
    }

    private fun getDelayUntilUpcomingNightFetchMillis(
        now: Calendar,
        randLong: (Long, Long) -> Long
    ): Long {
        val nextFetchIntervalStartTime = now.cloneCalendar().apply {
            set(Calendar.HOUR_OF_DAY, FETCH_START_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // A time after midnight is one calendar day after now.
            add(Calendar.DATE, 1)
        }

        val fetchOffsetSeconds = randLong(0L, FETCH_INTERVAL_DURATION_SECONDS).toInt()
        val userFetchTime = nextFetchIntervalStartTime.cloneCalendar().apply {
            add(Calendar.SECOND, fetchOffsetSeconds)
        }

        return userFetchTime.timeInMillis - now.timeInMillis
    }

    /**
     * Sets the fetch delay to a specified number of seconds. This makes it easier to manually test.
     */
    fun setQAFetchDelayOverride(seconds: Long) {
        isQABuild = true
        qaFetchDelayOverrideMillis = TimeUnit.SECONDS.toMillis(seconds)
    }

    /**
     * Resets the fetch delay values to production values. We do this so we do not spam the Pocket servers.
     */
    private fun resetQAFetchDelayOverrides() {
        isQABuild = false
        qaFetchDelayOverrideMillis = null
    }

    companion object {
        @VisibleForTesting(otherwise = PRIVATE) const val FETCH_START_HOUR = 3 // am
        @VisibleForTesting(otherwise = PRIVATE) const val FETCH_END_HOUR = 5L
        private val FETCH_INTERVAL_DURATION_SECONDS =
            TimeUnit.HOURS.toSeconds(FETCH_END_HOUR - FETCH_START_HOUR)

        // Since this is a background job, we're in no rush and can wait a while.
        @VisibleForTesting(otherwise = PRIVATE) val BACKOFF_DELAY_MIN_MILLIS = TimeUnit.SECONDS.toMillis(30)
        @VisibleForTesting(otherwise = PRIVATE) val BACKOFF_DELAY_MAX_MILLIS = TimeUnit.SECONDS.toMillis(60)
    }
}

// We keep this private because we don't know if cloning Calendars handles all the edge cases.
private fun Calendar.cloneCalendar(): Calendar = clone() as Calendar
