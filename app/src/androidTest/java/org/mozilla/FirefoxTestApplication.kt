/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla

import org.mozilla.tv.firefox.FirefoxApplication
import org.mozilla.tv.firefox.utils.ServiceLocator

class FirefoxTestApplication : FirefoxApplication() {

    override var serviceLocator: ServiceLocator = ServiceLocator(this)

}
