/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Executes the given network call on the OkHttp client's dispatcher as a suspension function.
 *
 * This technique to wrap callbacks is taken from the official docs:
 * https://github.com/Kotlin/kotlin-coroutines/blob/master/kotlin-coroutines-informal.md#wrapping-callbacks
 *
 * @throws [IOException] on call failure, as provided by OkHttp.
 */
suspend fun Call.executeAndAwait(): Response = suspendCoroutine { cont ->
    enqueue(ResumeContinuationCallback(cont))
}

private class ResumeContinuationCallback(private val continuation: Continuation<Response>) : Callback {
    override fun onResponse(call: Call, response: okhttp3.Response) {
        continuation.resume(response)
    }

    override fun onFailure(call: Call, e: IOException) {
        continuation.resumeWithException(e)
    }
}
