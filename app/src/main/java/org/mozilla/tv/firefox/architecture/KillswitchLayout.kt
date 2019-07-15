/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.architecture

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.VisibleForTesting
import org.mozilla.tv.firefox.components.locale.LocaleManager
import org.mozilla.tv.firefox.ext.languageAndMaybeCountryMatch
import java.util.Locale

sealed class KillswitchLocales {
    object All : KillswitchLocales()
    class ActiveIn(vararg val locales: Locale) : KillswitchLocales()
}

/**
 * Can be used to wrap some other [View] in order to prevent it from ever being shown if
 * certain locale and experiment requirements are not met.
 *
 * This is done to prevent bugs similar to #2133. In that bug, a view was set to visibility
 * GONE when disabled, however its animation toggled that visibility back to VISIBLE.
 * [KillswitchLayout] allows us to be sure that something will not be displayed if
 * requirements are not set by intercepting [setVisibility] calls.
 */
class KillswitchLayout : FrameLayout {
    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    // This is the most recent visibility that client code attempted to set on
    // this view, whether or not it was actually applied
    private var desiredVisibility = this.visibility

    private var isAllowedByCurrentExperiment: Boolean? = null
    private var allowedInLocales: KillswitchLocales? = null

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var localeManager = LocaleManager.getInstance()

    /**
     * Allows visibility to be set if the language and country of the current locale
     * matches any one of the allowed locales.
     *
     * An allowed locale with no country set (represented by an empty string, see
     * javadoc on [Locale.getCountry]) will match with any country, so long as the
     * language is correct.
     */
    fun setRequirements(isAllowedByCurrentExperiment: Boolean, allowedInLocales: KillswitchLocales) {
        this.isAllowedByCurrentExperiment = isAllowedByCurrentExperiment
        this.allowedInLocales = allowedInLocales
        visibility = desiredVisibility
    }

    /**
     * Sets the visibility to gone if any of the following are true:
     * 1) Locale/Experiment requirements have not been set
     * 2) Current locale is not allowed
     * 3) Client is not in a required experiment
     *
     * Otherwise, sets visibility as usual
     */
    override fun setVisibility(visibility: Int) {
        // Note that this method duplicates some logic found in Settings.shouldShowTVOnboarding.
        // Make sure that any changes made in one place are reflected in the other as well.

        this.desiredVisibility = visibility
        val context = context ?: return
        val isAllowedByCurrentExperiment = this.isAllowedByCurrentExperiment
        val allowedInLocales = this.allowedInLocales

        if (isAllowedByCurrentExperiment == null || allowedInLocales == null) {
            return super.setVisibility(View.GONE)
        }

        if (!isAllowedByCurrentExperiment) {
            return super.setVisibility(View.GONE)
        }

        if (allowedInLocales == KillswitchLocales.All) {
            return super.setVisibility(visibility)
        }

        val allowedLocales = (allowedInLocales as? KillswitchLocales.ActiveIn)?.locales
        val currentLocale = localeManager.getCurrentLocale(context)

        val isCurrentLocaleAllowed = currentLocale.languageAndMaybeCountryMatch(allowedLocales)

        if (!isCurrentLocaleAllowed) {
            return super.setVisibility(View.GONE)
        }

        super.setVisibility(visibility)
    }
}
