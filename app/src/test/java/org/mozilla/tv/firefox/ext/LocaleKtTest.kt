package org.mozilla.tv.firefox.ext

import org.junit.Assert.*
import org.junit.Test
import java.util.*

class LocaleKtTest {

    @Test
    fun `WHEN no country was passed AND language is correct THEN should return true`() {
        val allowed = Locale("en")

        assertTrue(Locale.US.languageAndMaybeCountryMatch(arrayOf(allowed)))
        assertTrue(Locale.CANADA.languageAndMaybeCountryMatch(arrayOf(allowed)))
        assertTrue(Locale.UK.languageAndMaybeCountryMatch(arrayOf(allowed)))
    }

    @Test
    fun `WHEN no country was passed AND language is incorrect THEN should return false`() {
        val allowed = Locale("es")

        assertFalse(Locale.US.languageAndMaybeCountryMatch(arrayOf(allowed)))
        assertFalse(Locale.CANADA.languageAndMaybeCountryMatch(arrayOf(allowed)))
        assertFalse(Locale.UK.languageAndMaybeCountryMatch(arrayOf(allowed)))
    }

    @Test
    fun `WHEN the correct language is passed AND country is incorrect THEN should return false`() {
        val allowed = Locale("en", "CA")

        assertFalse(Locale.US.languageAndMaybeCountryMatch(arrayOf(allowed)))
        assertFalse(Locale.UK.languageAndMaybeCountryMatch(arrayOf(allowed)))
    }

    @Test
    fun `WHEN the correct language is passed AND country is correct THEN should return true`() {
        val allowed = Locale("en", "CA")

        assertTrue(Locale.CANADA.languageAndMaybeCountryMatch(arrayOf(allowed)))
    }
    
    @Test
    fun `WHEN an incorrect country AND an incorrect language are passed THEN should return false`() {
        val allowed = Locale.GERMANY

        assertFalse(Locale.CANADA.languageAndMaybeCountryMatch(arrayOf(allowed)))
        assertFalse(Locale.CHINA.languageAndMaybeCountryMatch(arrayOf(allowed)))
    }

    @Test
    fun `WHEN the correct country is passed AND the incorrect language is passed THEN should return false`() {
        val allowed = Locale("es", "US")

        assertFalse(Locale.US.languageAndMaybeCountryMatch(arrayOf(allowed)))
        assertFalse(Locale("fr", "US").languageAndMaybeCountryMatch(arrayOf(allowed)))
        assertFalse(Locale("de", "US").languageAndMaybeCountryMatch(arrayOf(allowed)))
    }

    @Test
    fun `WHEN first passed locale is incorrect AND second passed locale is correct THEN should return true`() {
        val allowed = Locale.US

        assertTrue(Locale.US.languageAndMaybeCountryMatch(arrayOf(Locale.CANADA, allowed)))
    }
}
