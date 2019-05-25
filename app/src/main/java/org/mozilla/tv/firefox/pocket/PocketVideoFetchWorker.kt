/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.pocket

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking
import org.mozilla.tv.firefox.ext.serviceLocator

/**
 * A background task that fetches the Pocket video recommendations and stores them to disk, if valid.
 *
 * IMPORTANT: to avoid overloading the server, job requests using this worker are expected to:
 * - Avoid scheduling jobs for many clients at the same time (e.g. all clients at refresh at 3am)
 * - Add randomness to their setBackoffCriteria timing (for errors)
 */
class PocketVideoFetchWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    // todo: does Sentry catch crashes in here?
    // todo: what happens to thrown exceptions?

    private val pocketEndpointRaw = appContext.serviceLocator.pocketEndpointRaw
    private val store = appContext.serviceLocator.pocketVideoStore

    override fun doWork(): Result {
        val rawJSONStr = runBlocking { pocketEndpointRaw.getGlobalVideoRecommendations() }
        if (rawJSONStr == null) {
            // If the connection to the server failed, we should try to reconnect. Beyond typical
            // scheduling delays, WorkManager does not add randomness to this retry logic: as
            // mentioned in the class kdoc, job requests are expected to add their own randomness.
            return Result.retry()
        }

        val wasSaveSuccessful = store.save(rawJSONStr)
        if (!wasSaveSuccessful) {
            // We only can't save if the JSON is invalid so the server returned invalid JSON.
            // The server is probably in a bad state: we should wait before connecting again.
            return Result.failure()
        }

        store.save(rawJSONStr)
        return Result.success()
    }
}
