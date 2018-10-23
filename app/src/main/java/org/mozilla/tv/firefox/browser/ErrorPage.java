/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.tv.firefox.browser;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import org.mozilla.tv.firefox.R;
import org.mozilla.tv.firefox.utils.HtmlLoader;

import java.util.Map;

import mozilla.components.browser.errorpages.ErrorType;

public class ErrorPage {

    public static String loadErrorPage(@NonNull final Context context,
                                       @NonNull final String desiredURL,
                                       @NonNull final ErrorType errorType) {

        // This is quite hacky: ideally we'd just load the css file directly using a '<link rel="stylesheet"'.
        // However WebView thinks it's still loading the original page, which can be an https:// page.
        // If mixed content blocking is enabled (which is probably what we want in Focus), then webkit
        // will block file:///android_res/ links from being loaded - which blocks our css from being loaded.
        // We could hack around that by enabling mixed content when loading an error page (and reenabling it
        // once that's loaded), but doing that correctly and reliably isn't particularly simple. Loading
        // the css data and stuffing it into our html is much simpler, especially since we're already doing
        // string substitutions.
        // As an added bonus: file:/// URIs are broken if the app-ID != app package, see:
        // https://code.google.com/p/android/issues/detail?id=211768 (this breaks loading css via file:///
        // references when running debug builds, and probably klar too) - which means this wouldn't
        // be possible even if we hacked around the mixed content issues.
        final String cssString = HtmlLoader.loadResourceFile(context, R.raw.errorpage_style, null);

        final Map<String, String> substitutionMap = new ArrayMap<>();

        final Resources resources = context.getResources();

        substitutionMap.put("%page-title%", resources.getString(R.string.errorpage_title));
        substitutionMap.put("%button%", resources.getString(R.string.errorpage_refresh));
        substitutionMap.put("%messageShort%", resources.getString(errorType.getTitleRes()));
        substitutionMap.put("%messageLong%", resources.getString(errorType.getMessageRes(), desiredURL));
        substitutionMap.put("%css%", cssString);

        return HtmlLoader.loadResourceFile(context, R.raw.errorpage, substitutionMap);
    }
}
