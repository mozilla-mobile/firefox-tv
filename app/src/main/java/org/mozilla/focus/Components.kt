/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.content.Context
import android.util.AttributeSet
import mozilla.components.browser.session.SessionManager
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineSession
import mozilla.components.concept.engine.EngineView
import mozilla.components.concept.engine.Settings

/**
 * Helper class for lazily instantiating and keeping references to components needed by the
 * application.
 */
class Components {
    val sessionManager by lazy { SessionManager(DummyEngine()) }
}

/**
 * We are not using an "Engine" implementation yet. Therefore we create this dummy that we pass to
 * the [SessionManager] for now.
 */
private class DummyEngine : Engine {
    override val settings = object : Settings {}

    override fun createSession(private: Boolean): EngineSession {
        throw NotImplementedError()
    }

    override fun createView(context: Context, attrs: AttributeSet?): EngineView {
        throw NotImplementedError()
    }

    override fun name(): String {
        throw NotImplementedError()
    }
}
