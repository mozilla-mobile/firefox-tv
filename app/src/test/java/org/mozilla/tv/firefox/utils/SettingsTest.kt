package org.mozilla.tv.firefox.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.components.locale.LocaleManager
import org.mozilla.tv.firefox.pocket.PocketOnboardingActivity

class SettingsTest {

    @MockK lateinit var context: Context
    @MockK lateinit var preferences: SharedPreferences
    @MockK lateinit var resources: Resources
    @MockK lateinit var localeManager: LocaleManager

    lateinit var settings: Settings

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        every { context.applicationContext } answers { context }
        mockkStatic(PreferenceManager::class)
        every { PreferenceManager.getDefaultSharedPreferences(context) } answers { preferences }
        every { context.resources } answers { resources }

        settings = Settings.getInstance(context)
    }

    @Test
    fun `GIVEN current language is english AND onboarding has not been shown WHEN shouldShowPocketOnboarding is called THEN it should return true`() {
        every { preferences.getBoolean(PocketOnboardingActivity.POCKET_ONBOARDING_SHOWN_PREF, false) } answers { false }
        every { localeManager.currentLanguageIsEnglish(context) } answers { true }

        assertTrue(settings.shouldShowPocketOnboarding(localeManager, context))
    }

    @Test
    fun `GIVEN current language is not english AND onboarding has not been shown WHEN shouldShowPocketOnboarding is called THEN it should return false`() {
        every { preferences.getBoolean(PocketOnboardingActivity.POCKET_ONBOARDING_SHOWN_PREF, false) } answers { false }
        every { localeManager.currentLanguageIsEnglish(context) } answers { false }

        assertFalse(settings.shouldShowPocketOnboarding(localeManager, context))
    }
}
