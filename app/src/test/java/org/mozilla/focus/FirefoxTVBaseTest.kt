/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import org.robolectric.annotation.Config

/** A base test for Robolectric tests: it defines a config for all tests. */
@Config(
        // Robolectric doesn't merge the main assets with the test assets so we have to specify
        // the path explicitly: https://github.com/robolectric/robolectric/issues/2647
        assetDir = "app/src/main/assets"
)
open class FirefoxTVBaseTest