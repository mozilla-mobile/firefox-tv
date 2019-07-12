package org.mozilla.tv.firefox.ext

import java.util.*


fun Locale.languageAndMaybeCountryMatch(allowedLocales: Array<out Locale>?): Boolean {
    allowedLocales ?: return false
    return allowedLocales.any { allowed ->
        val languageMatches = allowed.language == this.language
        val countryMatches = allowed.country.isEmpty() ||
                allowed.country == this.country
        return languageMatches && countryMatches
    }
}
