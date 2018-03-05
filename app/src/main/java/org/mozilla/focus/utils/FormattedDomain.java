/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.text.TextUtils;
import android.util.Patterns;

import org.mozilla.focus.utils.publicsuffix.PublicSuffix;

import java.net.URI;

/** Methods for formatting a domain URI. */
public class FormattedDomain {

    private FormattedDomain() {}

    /**
     * Returns the domain for the given URI, formatted by the other available parameters.
     *
     * A public suffix is a top-level domain. For the input, "https://github.com", you can specify
     * {@code shouldIncludePublicSuffix}:
     * - true: "github.com"
     * - false: "github"
     *
     * The subdomain count is the number of subdomains you want to include; the domain will always be included. For
     * the input, "https://m.blog.github.io/", excluding the public suffix and the subdomain count:
     * - 0: "github"
     * - 1: "blog.github.com"
     * - 2: "m.blog.github.com"
     *
     * ipv4 & ipv6 urls will return the address directly.
     *
     * This implementation is influenced by Firefox iOS and can be used in place of some URI formatting functions:
     * - hostSLD [1]: exclude publicSuffix, 0 subdomains
     * - baseDomain [2]: include publicSuffix, 0 subdomains
     *
     * Expressing the method this way (instead of separate baseDomain & hostSLD methods) is more flexible if we want to
     * change the subdomain count.
     *
     * [1]: https://github.com/mozilla-mobile/firefox-ios/blob/deb9736c905cdf06822ecc4a20152df7b342925d/Shared/Extensions/NSURLExtensions.swift#L152
     * [2]: https://github.com/mozilla-mobile/firefox-ios/blob/deb9736c905cdf06822ecc4a20152df7b342925d/Shared/Extensions/NSURLExtensions.swift#L205
     *
     * @param context the Activity context.
     * @param uri the URI whose host we should format.
     * @param shouldIncludePublicSuffix true if the public suffix should be included, false otherwise.
     * @param subdomainCount The number of subdomains to include.
     *
     * @return the formatted domain, or the empty String if the host cannot be found.
     */
    @NonNull
    @WorkerThread // calls PublicSuffix methods.
    public static String format(@NonNull final Context context, @NonNull final URI uri,
                                final boolean shouldIncludePublicSuffix, @IntRange(from = 0) final int subdomainCount) {
        if (context == null) { throw new NullPointerException("Expected non-null Context argument"); }
        if (uri == null) { throw new NullPointerException("Expected non-null uri argument"); }
        if (subdomainCount < 0) { throw new IllegalArgumentException("Expected subdomainCount >= 0."); }

        final String host = uri.getHost();
        if (TextUtils.isEmpty(host)) {
            return ""; // There's no host so there's no domain to retrieve.
        }

        if (isIPv4(host) ||
                isIPv6(uri) ||
                !host.contains(".")) { // If this is just a hostname and not a FQDN, use the entire hostname.
            return host;
        }

        final String domainStr = PublicSuffix.getPublicSuffix(context, host, subdomainCount + 1);
        if (TextUtils.isEmpty(domainStr)) {
            // There is no public suffix found so we assume the whole host is a domain.
            return stripSubdomains(host, subdomainCount);
        }

        if (!shouldIncludePublicSuffix) {
            // We could be slightly more efficient if we wrote a new algorithm rather than using PublicSuffix twice
            // but I don't think it's worth the time and it'd complicate the code with more independent branches.
            return PublicSuffix.stripPublicSuffix(context, domainStr);
        }
        return domainStr;
    }

    /** Strips any subdomains from the host over the given limit. */
    private static String stripSubdomains(String host, final int desiredSubdomainCount) {
        int includedSubdomainCount = 0;
        for (int i = host.length() - 1; i >= 0; --i) {
            if (host.charAt(i) == '.') {
                if (includedSubdomainCount >= desiredSubdomainCount) {
                    return host.substring(i + 1, host.length());
                }

                includedSubdomainCount += 1;
            }
        }

        // There are fewer subdomains than the total we'll accept so return them all!
        return host;
    }

    @VisibleForTesting static boolean isIPv4(final String host) {
        return Patterns.IP_ADDRESS.matcher(host).matches();
    }

    // impl via FFiOS: https://github.com/mozilla-mobile/firefox-ios/blob/deb9736c905cdf06822ecc4a20152df7b342925d/Shared/Extensions/NSURLExtensions.swift#L292
    private static boolean isIPv6(final URI uri) {
        final String host = uri.getHost();
        return !TextUtils.isEmpty(host) && host.contains(":");
    }
}
