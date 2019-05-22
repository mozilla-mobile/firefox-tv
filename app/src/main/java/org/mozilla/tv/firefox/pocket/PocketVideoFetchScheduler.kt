/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
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
class PocketVideoFetchScheduler : LifecycleObserver {

    @OnLifecycleEvent(ON_START)
    fun onStart() {
        schedulePocketBackgroundFetch(WorkManager.getInstance())
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun schedulePocketBackgroundFetch(workManager: WorkManager) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // TODO: why do we want this from a UX perspective.
        // todo: this explanation sucks.
        // Our fetch scheduling scheme: when the user foregrounds the app, we schedule an update for the next calendar
        // day (i.e. if today is the 2nd, we schedule for the 3rd) at a random time inside our fetch interval.
        // it's still "tonight" but already after midnight, we
        // attempt to schedule one for the following night.
        val saveRequest = OneTimeWorkRequestBuilder<PocketVideoFetchWorker>()
            .setConstraints(constraints)
            .setInitialDelay(getDelayUntilUpcomingNightFetchMillis(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(FETCH_UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, saveRequest)
    }

    @VisibleForTesting(otherwise = PRIVATE)
    fun getDelayUntilUpcomingNightFetchMillis(
        now: Calendar = Calendar.getInstance(),
        randInt: (Int) -> Int = { Random.nextInt(it) }
    ): Long {
        val nextFetchIntervalStartTime = now.cloneCalendar().apply {
            set(Calendar.HOUR_OF_DAY, FETCH_START_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // A time after midnight is one calendar day after now.
            add(Calendar.DATE, 1)
        }

        val fetchOffsetSeconds = randInt(FETCH_INTERVAL_DURATION_SECONDS)
        val userFetchTime = nextFetchIntervalStartTime.cloneCalendar().apply {
            add(Calendar.SECOND, fetchOffsetSeconds)
        }

        return userFetchTime.timeInMillis - now.timeInMillis
    }

    companion object {
        @VisibleForTesting(otherwise = PRIVATE) const val FETCH_START_HOUR = 3 // am
        @VisibleForTesting(otherwise = PRIVATE) const val FETCH_END_HOUR = 5L
        private val FETCH_INTERVAL_DURATION_SECONDS =
            TimeUnit.HOURS.toSeconds(FETCH_END_HOUR - FETCH_START_HOUR).toInt()
    }
}

// We keep this private because we don't know if cloning Calendars handles all the edge cases.
private fun Calendar.cloneCalendar(): Calendar = clone() as Calendar
