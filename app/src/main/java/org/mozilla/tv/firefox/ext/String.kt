/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.net.Uri
import org.mozilla.tv.firefox.utils.UrlUtils
import java.net.URI
import java.net.URISyntaxException

// Extension functions for the String class

/**
 * Beautify a URL by truncating it in a way that highlights important parts of the URL.
 *
 * Spec: https://github.com/mozilla-mobile/focus-android/issues/1231#issuecomment-326237077
 */
fun String.beautifyUrl(): String {
    if (isNullOrEmpty() || !UrlUtils.isHttpOrHttps(this)) {
        return this
    }

    val beautifulUrl = StringBuilder()

    val uri = Uri.parse(this)

    // Use only the truncated host name

    val truncatedHost = uri.truncatedHost()
    if (truncatedHost.isNullOrEmpty()) {
        return this
    }

    beautifulUrl.append(truncatedHost)

    // Append the truncated path

    val truncatedPath = uri.truncatedPath()
    if (!truncatedPath.isNullOrEmpty()) {
        beautifulUrl.append(truncatedPath)
    }

    // And then append (only) the first query parameter

    val query = uri.query
    if (!query.isNullOrEmpty()) {
        beautifulUrl.append("?")
        beautifulUrl.append(query.split("&").first())
    }

    // We always append a fragment if there's one

    val fragment = uri.fragment
    if (!fragment.isNullOrEmpty()) {
        beautifulUrl.append("#")
        beautifulUrl.append(fragment)
    }

    return beautifulUrl.toString()
}

fun String?.toUri(): Uri? = if (this == null) {
    null
} else {
    Uri.parse(this)
}

fun String?.toJavaURI(): URI? = if (this == null) {
    null
} else {
    try {
        URI(this)
    } catch (e: URISyntaxException) {
        null
    }
}

// TODO: move to Uri class android-components#648
// This algorithm is imperfect but we've used it for a while.
val String.isUriYouTubeTV: Boolean
    get() = toLowerCase().contains("youtube.com/tv") ||
            toLowerCase().contains("ftv.cdn.mozilla.net/ytht")

val String.isUriYouTubeTvVideo: Boolean
    get() = this.isUriYouTubeTV &&
            this.contains("/watch/")

val String.isUriMozSignIn: Boolean
    get() = this.startsWith("https://accounts.firefox.com/authorization")
