/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import io.mockk.mockkStatic
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mozilla.tv.firefox.architecture.KillswitchLocales
import java.util.*

private const val LOCALE_EXT_FILE = "org.mozilla.tv.firefox.ext.LocaleKt"

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

    @Test
    fun `GIVEN all killswitch locale is allowed WHEN any locale is passed THEN should return true`() {
        val allowed = KillswitchLocales.All

        assertTrue(Locale.US.languageAndMaybeCountryMatch(allowed))
        assertTrue(Locale.CANADA.languageAndMaybeCountryMatch(allowed))
        assertTrue(Locale.UK.languageAndMaybeCountryMatch(allowed))
        assertTrue(Locale.GERMANY.languageAndMaybeCountryMatch(allowed))
        assertTrue(Locale.CHINA.languageAndMaybeCountryMatch(allowed))

        assertTrue(Locale.ENGLISH.languageAndMaybeCountryMatch(allowed))
        assertTrue(Locale.GERMAN.languageAndMaybeCountryMatch(allowed))
        assertTrue(Locale.CHINESE.languageAndMaybeCountryMatch(allowed))
    }

    @Test
    fun `WHEN KillswitchLocale ActiveIn is passed THEN overload should be called`() {
        // Mock extension functions in this class
        // If this test fails, check if this class has been moved, causing this string to be incorrect
        mockkStatic(LOCALE_EXT_FILE)

        val allowed = KillswitchLocales.ActiveIn(Locale.US)

        val locales = listOf(
                Locale.US,
                Locale.CANADA,
                Locale.UK,
                Locale.GERMANY,
                Locale.CHINA,
                Locale.ENGLISH,
                Locale.GERMAN,
                Locale.CHINESE
        )

        locales.forEach { it.languageAndMaybeCountryMatch(allowed) }

        // Verify that languageAndMaybeCountryMatch is called with an array of locales (i.e., the overload)
        locales.forEach { verify(exactly = 1) { it.languageAndMaybeCountryMatch(any<Array<Locale>>()) } }
    }
}
