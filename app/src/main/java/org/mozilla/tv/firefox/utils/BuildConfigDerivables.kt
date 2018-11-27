/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.net.Uri
import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.pocket.PocketVideoRepo

private const val POCKET_PARAM_API_KEY = "consumer_key"

/**
 * Computes information that must be derived from the [BuildConfig].
 *
 * This logic is often simple but noisy, so pulling it out of client code improves readability.
 */
class BuildConfigDerivables {
    @Suppress("SENSELESS_COMPARISON") // Values of BuildConfig can change but the compiler doesn't know that
    val initialPocketRepoState = when {
        BuildConfig.POCKET_KEY == null -> PocketVideoRepo.FeedState.NoAPIKey
        else -> PocketVideoRepo.FeedState.Loading
    }

    // Pocket key can be null if it was not included in the build.  In this case we
    // know that any calls to Pocket will fail, and so we do not provide the
    // endpoint at all to prevent unnecessary requests.
    @Suppress("UselessCallOnNotNull")
    val globalPocketVideoEndpoint: Uri? = when {
        BuildConfig.POCKET_KEY.isNullOrEmpty() -> null
        else -> Uri.parse("https://getpocket.cdn.mozilla.net/v3/firefox/global-video-recs")
            .buildUpon()
            .appendQueryParameter(POCKET_PARAM_API_KEY, BuildConfig.POCKET_KEY)
            .appendQueryParameter("version", "2")
            .appendQueryParameter("authors", "1")
            .build()
    }
}
