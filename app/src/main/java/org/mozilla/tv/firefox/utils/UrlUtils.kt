/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import org.mozilla.tv.firefox.webrender.BrowserFragment

import org.mozilla.tv.firefox.components.search.SearchEngineManager

import java.net.URI
import java.net.URISyntaxException

object UrlUtils {
    @JvmStatic
    fun normalize(input: String): String {
        val trimmedInput = input.trim { it <= ' ' }
        var uri = Uri.parse(trimmedInput)

        uri.scheme.let { scheme ->
            if (scheme == null || scheme.isEmpty()) {
                uri = Uri.parse("http://$trimmedInput")
            }
        }
        return uri.toString()
    }

    /**
     * Is the given string a URL or should we perform a search?
     *
     * TODO: This is a super simple and probably stupid implementation.
     */
    @JvmStatic
    fun isUrl(url: String): Boolean {
        val trimmedUrl = url.trim { it <= ' ' }
        return if (trimmedUrl.contains(" ")) {
            false
        } else trimmedUrl.contains(".") || trimmedUrl.contains(":")
    }

    @JvmStatic
    fun isValidSearchQueryUrl(url: String): Boolean {
        var trimmedUrl = url.trim { it <= ' ' }
        if (!trimmedUrl.matches("^.+?://.+?".toRegex())) {
            // UI hint url doesn't have http scheme, so add it if necessary
            trimmedUrl = "http://$trimmedUrl"
        }

        if (!URLUtil.isNetworkUrl(trimmedUrl)) {
            return false
        }

        return trimmedUrl.matches(".*%s$".toRegex())
    }

    @JvmStatic
    fun isHttpOrHttps(url: String?): Boolean {
        return if (url == null || url.isEmpty()) {
            false
        } else url.startsWith("http:") || url.startsWith("https:")
    }

    @JvmStatic
    fun isSearchQuery(text: String): Boolean {
        return text.contains(" ")
    }

    @JvmStatic
    fun createSearchUrl(context: Context, searchTerm: String): String {
        val searchEngine = SearchEngineManager.getInstance()
                .getDefaultSearchEngine(context)

        return searchEngine.buildSearchUrl(searchTerm)
    }

    @JvmStatic
    fun stripUserInfo(url: String?): String {
        if (url == null || url.isEmpty()) return ""

        try {
            var uri = URI(url)

            uri.userInfo ?: return url

            // Strip the userInfo to minimise spoofing ability. This only affects what's shown
            // during browsing, this information isn't used when we start editing the URL:
            uri = URI(uri.scheme, null, uri.host, uri.port, uri.path, uri.query, uri.fragment)

            return uri.toString()
        } catch (e: URISyntaxException) {
            // We might be trying to display a user-entered URL (which could plausibly contain errors),
            // in this case its safe to just return the raw input.
            // There are also some special cases that URI can't handle, such as "http:" by itself.
            return url
        }
    }

    @JvmStatic
    fun isPermittedResourceProtocol(scheme: String?): Boolean {
        return scheme != null && (scheme.startsWith("http") ||
                scheme.startsWith("https") ||
                scheme.startsWith("file") ||
                scheme.startsWith("data"))
    }

    @JvmStatic
    fun isSupportedProtocol(scheme: String?): Boolean {
        return scheme != null && (isPermittedResourceProtocol(scheme) || scheme.startsWith("error"))
    }

    @JvmStatic
    fun isInternalErrorURL(url: String): Boolean {
        return "data:text/html;charset=utf-8;base64," == url
    }

    @JvmStatic
    fun urlsMatchExceptForTrailingSlash(url1: String, url2: String): Boolean {
        val lengthDifference = url1.length - url2.length

        if (lengthDifference == 0) {
            // The simplest case:
            return url1.equals(url2, ignoreCase = true)
        } else if (lengthDifference == 1) {
            // url1 is longer:
            return url1[url1.length - 1] == '/' && url1.regionMatches(0, url2, 0, url2.length, ignoreCase = true)
        } else if (lengthDifference == -1) {
            return url2[url2.length - 1] == '/' && url2.regionMatches(0, url1, 0, url1.length, ignoreCase = true)
        }

        return false
    }

    @JvmStatic
    fun stripCommonSubdomains(host: String?): String? {
        if (host == null) {
            return null
        }

        // In contrast to desktop, we also strip mobile subdomains,
        // since its unlikely users are intentionally typing them
        var start = 0

        when {
            host.startsWith("www.") -> start = 4
            host.startsWith("mobile.") -> start = 7
            host.startsWith("m.") -> start = 2
        }

        return host.substring(start)
    }

    /**
     * Accepts a url in String form and returns whatever text should be displayed
     * in the url bar EditText
     */
    @JvmStatic
    fun toUrlBarDisplay(url: String): String {
        return when (url) {
            BrowserFragment.APP_URL_HOME -> "" // Empty string forces the EditText to show hint text
            else -> url
        }
    }
}
