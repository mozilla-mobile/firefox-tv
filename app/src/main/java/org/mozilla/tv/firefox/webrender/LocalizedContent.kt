/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.content.Context
import android.content.pm.PackageManager
import androidx.collection.ArrayMap
import android.view.View
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.components.locale.Locales
import org.mozilla.tv.firefox.utils.BuildConstants
import org.mozilla.tv.firefox.utils.HtmlLoader
import org.mozilla.tv.firefox.utils.URLs

object LocalizedContent {
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
            val versionName = packageInfo.versionName
            val engineVersion = BuildConstants.getEngineVersion(context)
            aboutVersion = "$versionName (Build #$engineVersion)"
        } catch (e: PackageManager.NameNotFoundException) {
            // Nothing to do if we can't find the package name.
            // TODO: Should we add an sentry log here? Why would we ever end up here
        }

        substitutionMap["%about-version%"] = aboutVersion

        val appName = context.resources.getString(R.string.app_name)
        val mplUrl = "https://www.mozilla.org/en-US/MPL/"
        val trademarkPolicyUrl = "https://www.mozilla.org/foundation/trademarks/policy/"
        val trackingProtectionUrl = "https://wiki.mozilla.org/Security/Tracking_protection#Lists"

        val yourRights = resources.getString(R.string.your_rights)
        substitutionMap["%your-rights%"] = yourRights

        val content1 = resources.getString(R.string.your_rights_content1, appName)
        substitutionMap["%your-rights-content1%"] = content1

        val content2 = resources.getString(R.string.your_rights_content2, appName, mplUrl)
        substitutionMap["%your-rights-content2%"] = content2

        val content3 = resources.getString(R.string.your_rights_content3, appName, trademarkPolicyUrl)
        substitutionMap["%your-rights-content3%"] = content3

        val content4 = resources.getString(R.string.your_rights_content4, appName, URLs.URL_LICENSES)
        substitutionMap["%your-rights-content4%"] = content4

        val content5 = resources.getString(R.string.your_rights_content5, appName, URLs.URL_GPL, trackingProtectionUrl)
        substitutionMap["%your-rights-content5%"] = content5

        substitutionMap["%dir%"] = when (context.resources.configuration.layoutDirection) {
            View.LAYOUT_DIRECTION_LTR -> "ltr"
            View.LAYOUT_DIRECTION_RTL -> "rtl"
            else -> "auto"
        }

        substitutionMap["%css%"] = HtmlLoader.loadResourceFile(context, R.raw.style, null)

        return HtmlLoader.loadResourceFile(context, R.raw.about, substitutionMap)
    }

    fun generatePage(context: Context, page: Int): String {
        val substitutionMap = ArrayMap<String, String>()

        /** See comment in [ErrorPage] for why we need to load css this way. */
        substitutionMap["%css%"] = HtmlLoader.loadResourceFile(context, R.raw.style, null)

        return HtmlLoader.loadResourceFile(context, page, substitutionMap)
    }
}
