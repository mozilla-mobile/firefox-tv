/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.pm.PackageManager
import android.support.v4.util.ArrayMap
import android.support.v4.view.ViewCompat
import android.view.View
import android.webkit.WebView

import org.mozilla.focus.R
import org.mozilla.focus.locale.Locales
import org.mozilla.focus.utils.HtmlLoader

object LocalizedContent {
    // We can't use "about:" because webview silently swallows about: pages, hence we use
    // a custom scheme.
    const val URL_ABOUT = "firefox:about"

    @JvmStatic
    fun handleInternalContent(url: String, webView: WebView): Boolean {
        if (URL_ABOUT == url) {
            loadAbout(webView)
            return true
        }
        return false
    }

    /**
     * Load the content for focus:about
     */
    @Suppress("LongMethod") // This doesn't change much.
    private fun loadAbout(webView: WebView) {
        val context = webView.context
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

        putLayoutDirectionIntoMap(webView, substitutionMap)

        val data = HtmlLoader.loadResourceFile(context, R.raw.about, substitutionMap)
        // We use a file:/// base URL so that we have the right origin to load file:/// css and image resources.
        webView.loadDataWithBaseURL("file:///android_asset/about.html", data, "text/html", "UTF-8", null)
    }

    private fun putLayoutDirectionIntoMap(webView: WebView, substitutionMap: MutableMap<String, String>) {
        ViewCompat.setLayoutDirection(webView, View.LAYOUT_DIRECTION_LOCALE)
        val layoutDirection = ViewCompat.getLayoutDirection(webView)

        val direction: String

        if (layoutDirection == View.LAYOUT_DIRECTION_LTR) {
            direction = "ltr"
        } else if (layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            direction = "rtl"
        } else {
            direction = "auto"
        }

        substitutionMap["%dir%"] = direction
    }
}
