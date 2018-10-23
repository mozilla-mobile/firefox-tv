/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

/**
 * The application class used by the robolectric test runner. This class may be used to add
 * additional test setup code.
 */
class TestFirefoxApplication : FirefoxApplication() {

    /**
     * A function to retrieve the system user agent. The default implementation, used by the
     * production app, calls a static method that throws a NullPointerException during robolectric
     * tests. We can't mock static methods so we override the the method.
     */
    override fun getSystemUserAgent(): String =
            "Mozilla/5.0 (Linux; Android 7.1.2) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/2.2.0.2 Chrome/59.0.3071.125 Mobile Safari/537.36"
}
