/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import org.mozilla.tv.firefox.ext.webRenderComponents

/**
 * Facade hiding the ceremony needed to setEnabled Turbo Mode.
 *
 * We are trying to keep our setting and the state of the engine synchronized.
 */
class TurboMode(private val app: Application) {

    fun isEnabled() = Settings.getInstance(app).isBlockingEnabled

    fun setEnabled(enabled: Boolean) {
        val settings = Settings.getInstance(app)
        settings.isBlockingEnabled = enabled

        // Update TrackingProtectionPolicy for both current session and EngineSettings
        val engineSettings = app.webRenderComponents.engine.settings
        val engineSession = app.webRenderComponents.sessionManager.getOrCreateEngineSession()
        if (enabled) {
            engineSettings.trackingProtectionPolicy = settings.trackingProtectionPolicy
            engineSession.enableTrackingProtection(settings.trackingProtectionPolicy)
        } else {
            engineSettings.trackingProtectionPolicy = null
            engineSession.disableTrackingProtection()
        }
        _observable.postValue(enabled)
    }

    private val _observable = MutableLiveData<Boolean>()
    val observable: LiveData<Boolean> = _observable
}
