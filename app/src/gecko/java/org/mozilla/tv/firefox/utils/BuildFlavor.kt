/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import org.mozilla.tv.firefox.BuildConfig

/**
 * Provides build flavor specific tasks
 */
class BuildFlavor {
    fun getDebugLogStr(isDevBuild: Boolean): String? {
        if (isDevBuild) {
            var debugLogStr = "DEBUG / " +
                    "FLAVOR: ${BuildConfig.FLAVOR} / VERSION: ${BuildConfig.VERSION_NAME}"

            if (BuildConstants.isGeckoBuild) {
                debugLogStr += " / GECKO: ${BuildConfig.GECKOVIEW_VERSION}"
            }
            return debugLogStr
        }

        return null
    }
}
