/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.content.Context
import org.mozilla.tv.firefox.ext.webRenderComponents

/**
 * Facade hiding the ceremony needed to setEnabled Turbo Mode.
 *
 * We are trying to keep our setting and the state of the engine synchronized.
 */
object TurboMode {
    /**
     * Is Turbo Mode enabled?
     */
    fun isEnabled(context: Context) = Settings.getInstance(context).isBlockingEnabled

    /**
     * Toggle turbo mode on or off. This will update the setting and the engine at the same time.
     */
    fun setEnabled(context: Context, enabled: Boolean) {
        val settings = Settings.getInstance(context)
        settings.isBlockingEnabled = enabled

        // Update TrackingProtectionPolicy for both current session and EngineSettings
        val engineSettings = context.webRenderComponents.engine.settings
        val engineSession = context.webRenderComponents.sessionManager.getOrCreateEngineSession()
        if (enabled) {
            engineSettings.trackingProtectionPolicy = settings.trackingProtectionPolicy
            engineSession.enableTrackingProtection(settings.trackingProtectionPolicy)
        } else {
            engineSettings.trackingProtectionPolicy = null
            engineSession.disableTrackingProtection()
        }
    }
}
