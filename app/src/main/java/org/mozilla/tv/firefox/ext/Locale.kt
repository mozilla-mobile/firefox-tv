/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import org.mozilla.tv.firefox.architecture.KillswitchLocales
import java.util.Locale

/**
 * Returns true if the following is true for _any_ of the elements in [allowedLocales]:
 * 1) Language == [this] language
 * 2) No country selected || [this] country is selected country
 */
fun Locale.languageAndMaybeCountryMatch(allowedLocales: Array<out Locale>?): Boolean {
    allowedLocales ?: return false
    return allowedLocales.any { allowed ->
        val languageMatches = allowed.language == this.language
        val countryMatches = allowed.country.isEmpty() ||
                allowed.country == this.country
        return@any languageMatches && countryMatches
    }
}

/**
 * @see Locale.languageAndMaybeCountryMatch
 */
fun Locale.languageAndMaybeCountryMatch(allowedLocales: KillswitchLocales): Boolean {
    return when (allowedLocales) {
        is KillswitchLocales.All -> true
        is KillswitchLocales.ActiveIn -> this.languageAndMaybeCountryMatch(allowedLocales.locales)
    }
}
