/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.manual.upgrade

import android.content.Context
import android.preference.PreferenceManager
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * TODO
 */
interface ManualUpgradeStarter {
    fun maybeShow(context: Context)
}

class DoNotShowUpgradeStarter : ManualUpgradeStarter {
    override fun maybeShow(context: Context) { /* Noop */ }
}

// TODO use different time library
private const val UPGRADE_REQUEST_LAST_SHOWN_AT = "UPGRADE_REQUEST_LAST_SHOWN_AT"
private val UTC = ZoneOffset.UTC
private const val SHOW_REQUEST_EVERY_X_DAYS = 5L

class RequestUpgradeStarter : ManualUpgradeStarter {

    override fun maybeShow(context: Context) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val lastShownAtEpochSecond = preferences.getLong(UPGRADE_REQUEST_LAST_SHOWN_AT, 0)
        val lastShownAt = LocalDateTime.ofEpochSecond(lastShownAtEpochSecond, 0, ZoneOffset.UTC)
        val now = LocalDateTime.now(UTC)

        if (lastShownAt.plusDays(SHOW_REQUEST_EVERY_X_DAYS) >= now) {
            preferences.edit()
                .putLong(UPGRADE_REQUEST_LAST_SHOWN_AT, now.toEpochSecond())
                .apply()

            context.startActivity(/* RequestUpgradeActivity */)
        }
    }
}

class ForceUpgradeStarter : ManualUpgradeStarter {
    override fun maybeShow(context: Context) {
        context.startActivity(/* ForceUpgradeActivity */)
    }
}
