/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import mozilla.components.browser.engine.system.SystemEngine
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.DefaultSettings
import mozilla.components.concept.engine.Engine
import mozilla.components.feature.session.SessionUseCases
import org.mozilla.focus.engine.CustomContentRequestInterceptor
import org.mozilla.focus.utils.Settings

/**
 * Helper class for lazily instantiating and keeping references to components needed by the
 * application.
 */
class Components(applicationContext: Context) {
    val engine: Engine by lazy {
        SystemEngine(DefaultSettings(
                trackingProtectionPolicy = Settings.getInstance(applicationContext).trackingProtectionPolicy,
                requestInterceptor = CustomContentRequestInterceptor(applicationContext),

                displayZoomControls = false,
                loadWithOverviewMode = true, // To respect the html viewport

                // We don't have a reason for users to access local files; assets can still
                // be loaded via file:///android_asset/
                allowFileAccess = false,
                allowContentAccess = false,

                mediaPlaybackRequiresUserGesture = false // Allows auto-play (which improves YouTube experience).
        ))
    }

    val sessionManager by lazy { SessionManager(engine) }

    val sessionUseCases by lazy { SessionUseCases(sessionManager) }
}
