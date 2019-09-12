/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.tv.firefox.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.mozilla.tv.firefox.utils.URLs.SEARCH_AMZN_MARKET_BY_PACKAGE

/**
 * Handles the boilerplate of construction, conversion, sending/receiving, etc of [Intent]s
 */
object IntentUtils {

    fun openFftvStorePage(context: Context) {
        // We use this instead of the value in BuildConfig so that it doesn't change between
        // debug and release builds
        val releasePackageId = "org.mozilla.tv.firefox"

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(SEARCH_AMZN_MARKET_BY_PACKAGE + releasePackageId)
        )
        context.startActivity(intent)
    }
}
