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
 */
class PocketVideoFetchWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    // TODO: en-US only.
    // todo: does Sentry catch crashes in here?
    // todo: what happens to thrown exceptions?

    private val pocketEndpointRaw = appContext.serviceLocator.pocketEndpointRaw
    private val store = appContext.serviceLocator.pocketVideoStore

    override fun doWork(): Result {
        val rawJSONStr = runBlocking { pocketEndpointRaw.getGlobalVideoRecommendations() }
        if (rawJSONStr == null) {
            // If the connection to the server failed, we should try to reconnect.
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
