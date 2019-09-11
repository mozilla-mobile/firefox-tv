/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.manual.upgrade

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import java.util.Calendar
import java.util.TimeZone

// TODO This file can be removed from master as soon as it has been released. See #2794

/**
 * This class determines whether or not an upgrade dialog should be shown. If yes, it shows
 * the dialog.
 *
 * Example of the strategy pattern.
 */
interface ManualUpgradeStarter {
    /**
     * Returns true if onboarding was shown
     */
    fun maybeShow(context: Context): Boolean
}

class DoNotShowUpgradeStarter : ManualUpgradeStarter {
    override fun maybeShow(context: Context): Boolean { return false }
}

private const val UPGRADE_REQUEST_LAST_SHOWN_AT = "UPGRADE_REQUEST_LAST_SHOWN_AT"
private val UTC = TimeZone.getTimeZone("UTC")
private const val SHOW_REQUEST_EVERY_X_DAYS = 5

class RequestUpgradeStarter : ManualUpgradeStarter {

    override fun maybeShow(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val lastShownEpochTime = preferences.getLong(UPGRADE_REQUEST_LAST_SHOWN_AT, 0)
        val lastShownAt = Calendar.getInstance().apply {
            this.timeInMillis = lastShownEpochTime
        }
        val now = Calendar.getInstance(UTC)

        lastShownAt.add(Calendar.DAY_OF_YEAR, SHOW_REQUEST_EVERY_X_DAYS)
        if (lastShownAt.before(now)) {
            preferences.edit()
                .putLong(UPGRADE_REQUEST_LAST_SHOWN_AT, now.timeInMillis)
                .apply()

            context.startActivity(Intent(context, RequestUpgradeActivity::class.java))
            return true
        }
        return false
    }
}

class ForceUpgradeStarter : ManualUpgradeStarter {
    override fun maybeShow(context: Context): Boolean {
        context.startActivity(Intent(context, ForceUpgradeActivity::class.java))
        // finish the MainActivity to prevent it from being accessed
        (context as Activity).finish()
        return true
    }
}
