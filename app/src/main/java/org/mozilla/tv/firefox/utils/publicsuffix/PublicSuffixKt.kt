/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils.publicsuffix

import android.content.Context
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** A helper to allow [PublicSuffix] to call Kotlin code: converting the whole file didn't seem right. */
internal object PublicSuffixKt {

    // We want to execute this on our existing common pool, which only Kotlin has access to so we
    // define the code here. The common pool is defined by ForkJoinPool.commonPool() on API 24+ but
    // it's created by Kotlin on lower API levels (we support 22+).
    @JvmStatic
    fun init(context: Context) {
        // We don't care for the result: we just want to call this method so it caches the file from disk.
        GlobalScope.launch { PublicSuffixPatterns.getExactSet(context) }
    }
}
