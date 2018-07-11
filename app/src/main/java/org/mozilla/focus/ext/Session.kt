/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.os.Bundle
import mozilla.components.browser.session.Session
import org.mozilla.focus.session.Source
import java.util.WeakHashMap

// Extension methods on the Session class. This is used for additional session data that is not part
// of the upstream browser-session component yet.

private val extensions = WeakHashMap<Session, SessionExtension>()

private fun getOrPutExtension(session: Session): SessionExtension {
    extensions[session]?.let { return it }

    return SessionExtension().also {
        extensions[session] = it
    }
}

private class SessionExtension {
    var savedWebViewState: Bundle? = null
    var source: Source = Source.NONE
    var isBlockingEnabled: Boolean = true
}

/**
 * Saving the state attached ot a session.
 *
 * Temporary solution until we can use the browser-engine component.
 *
 * Component upstream issue:
 * https://github.com/mozilla-mobile/android-components/issues/408
 */
var Session.savedWebViewState: Bundle?
    get() = getOrPutExtension(this).savedWebViewState
    set(value) { getOrPutExtension(this).savedWebViewState = value }

/**
 * The source of a session: How was this session created? E.g. did we receive an Intent or did the
 * user start this session?
 *
 * Component upstream issue:
 * https://github.com/mozilla-mobile/android-components/issues/407
 */
var Session.source: Source
    get() = getOrPutExtension(this).source
    set(value) { getOrPutExtension(this).source = value }

/**
 * Whether blocking is enabled for this session.
 *
 * Component upstream issue:
 * https://github.com/mozilla-mobile/android-components/issues/352
 */
var Session.isBlockingEnabled: Boolean
    get() = getOrPutExtension(this).isBlockingEnabled
    set(value) { getOrPutExtension(this).isBlockingEnabled = value }
