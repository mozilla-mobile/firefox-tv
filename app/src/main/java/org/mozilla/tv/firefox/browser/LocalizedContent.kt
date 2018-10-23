/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.browser

import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.util.ArrayMap
import android.view.View
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.locale.Locales
import org.mozilla.tv.firefox.utils.HtmlLoader

object LocalizedContent {
    // We can't use "about:" because webview silently swallows about: pages, hence we use
    // a custom scheme.
    const val URL_ABOUT = "firefox:about"

    /**
     * Load the content for firefox:about
     */
    @Suppress("LongMethod") // This doesn't change much.
    fun generateAboutPage(context: Context): String {
        val resources = Locales.getLocalizedResources(context)

        val substitutionMap = ArrayMap<String, String>()

        val appNameExtended = resources.getString(R.string.app_name_extended_fire)
        substitutionMap["%about-title%"] = appNameExtended

        var aboutVersion = ""
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            aboutVersion = String.format("%s", packageInfo.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            // Nothing to do if we can't find the package name.
        }

        substitutionMap["%about-version%"] = aboutVersion

        val appName = context.resources.getString(R.string.app_name)
        val mplUrl = "https://www.mozilla.org/en-US/MPL/"
        val trademarkPolicyUrl = "https://www.mozilla.org/foundation/trademarks/policy/"
        val gplUrl = "gpl.html"
        val trackingProtectionUrl = "https://wiki.mozilla.org/Security/Tracking_protection#Lists"
        val licensesUrl = "licenses.html"

        val yourRights = resources.getString(R.string.your_rights)
        substitutionMap["%your-rights%"] = yourRights

        val content1 = resources.getString(R.string.your_rights_content1, appName)
        substitutionMap["%your-rights-content1%"] = content1

        val content2 = resources.getString(R.string.your_rights_content2, appName, mplUrl)
        substitutionMap["%your-rights-content2%"] = content2

        val content3 = resources.getString(R.string.your_rights_content3, appName, trademarkPolicyUrl)
        substitutionMap["%your-rights-content3%"] = content3

        val content4 = resources.getString(R.string.your_rights_content4, appName, licensesUrl)
        substitutionMap["%your-rights-content4%"] = content4

        val content5 = resources.getString(R.string.your_rights_content5, appName, gplUrl, trackingProtectionUrl)
        substitutionMap["%your-rights-content5%"] = content5

        substitutionMap["%dir%"] = when (context.resources.configuration.layoutDirection) {
            View.LAYOUT_DIRECTION_LTR -> "ltr"
            View.LAYOUT_DIRECTION_RTL -> "rtl"
            else -> "auto"
        }

        // We use a file:/// base URL so that we have the right origin to load file:/// css and image resources.
        return HtmlLoader.loadResourceFile(context, R.raw.about, substitutionMap)
    }
}
