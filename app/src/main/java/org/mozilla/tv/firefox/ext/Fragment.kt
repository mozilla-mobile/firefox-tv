/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.support.v4.app.Fragment
import org.mozilla.tv.firefox.webrender.WebRenderComponents

/**
 * Get the components of this application's context.
 *
 * @throws IllegalStateException if not currently associated with a context.
 */
val Fragment.requireWebRenderComponents: WebRenderComponents
    get() = requireContext().webRenderComponents
