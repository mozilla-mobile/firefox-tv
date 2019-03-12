/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import androidx.annotation.VisibleForTesting
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.pow

private const val CACHE_UPDATE_FREQUENCY_MINUTES = 45L
private val CACHE_UPDATE_FREQUENCY_SECONDS = TimeUnit.MINUTES.toSeconds(CACHE_UPDATE_FREQUENCY_MINUTES)

/**
 * Simple interface for making a network request.
 *
 * This may operate on any thread, and consumers should take care to schedule
 * it off of the main thread.
 */
interface Endpoint<T> {
    fun request(): Single<Response<T>>
}

sealed class Response<T> {
    data class Success<T>(val data: T) : Response<T>()
    class Failure<T> : Response<T>()
}

/**
 * Used to make long term, reoccurring network requests.
 *
 * Will make an initial request followed by periodic updates, with exponentially
 * backing off retry attempts upon failure.
 */
class PeriodicRequester<T>(private val endpoint: Endpoint<T>) {

    /**
     * Makes one network request upon subscription. If this fails, more requests
     * will be made on an exponential backoff.
     *
     * Follow up requests will be made every 45 minutes, regardless of previous
     * success or failure.
     *
     * Repeated calls to this method will create separate [Observable]s,
     * potentially duplicating calls.
     */
    fun start(): Observable<Response<T>> = normalTimer()
        .flatMap {
            Observable.concat(requestAsynchronously().toObservable(), backoffRequests())
                .takeUntil { it is Response.Success }
        }

    /**
     * Generates an observable of [Long]s that together represent an
     * exponential backoff. Values summed will be smaller than
     * [CACHE_UPDATE_FREQUENCY_SECONDS]
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun backoffTimes(): Observable<Long> = Observable.range(1, Integer.MAX_VALUE)
        .map { 2.toDouble().pow(it).toLong() }
        .takeWhile { it < (CACHE_UPDATE_FREQUENCY_SECONDS / 2) }

    /**
     * Emits one value upon subscription, and another every 45 minutes
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun normalTimer(): Observable<Long> = Observable
        .interval(0, CACHE_UPDATE_FREQUENCY_SECONDS, TimeUnit.SECONDS)

    /**
     * Waits for the duration of each value in seconds, emits it, then
     * continues
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun backoffTimer(): Observable<Long> = backoffTimes()
        .concatMap {
            Observable.timer(it, TimeUnit.SECONDS)
        }

    /**
     * Makes a series of network requests, according to the schedule set by [backoffTimer]
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun backoffRequests(): Observable<Response<T>> = backoffTimer()
        .flatMapSingle { requestAsynchronously() }

    private fun requestAsynchronously(): Single<Response<T>> = endpoint.request()
        .subscribeOn(Schedulers.io())
}
