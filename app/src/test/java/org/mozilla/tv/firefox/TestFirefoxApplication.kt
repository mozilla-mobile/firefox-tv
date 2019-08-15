/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import mozilla.components.service.glean.Glean
import androidx.work.testing.WorkManagerTestInitHelper
import mozilla.components.concept.engine.utils.EngineVersion
import org.mozilla.tv.firefox.helpers.EngineVariantFunctionality
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner

/**
 * The application class used by the [FirefoxRobolectricTestRunner]: this class may modified to run additional
 * test setup code during off-device Robolectric tests.
 *
 * This class should not be confused with [FirefoxTestApplication] which is used for on-device tests.
 * Unfortunately, this class name is hardcoded into the [FirefoxRobolectricTestRunner] so we can't rename it to
 * better distinguish it from the other test application.
 */
@Suppress("unused") // Robolectric hardcodes this class name.
class TestFirefoxApplication : FirefoxApplication() {

    init {
        EngineVariantFunctionality.preventCrashFromComponentsDefaultUserAgent(getSystemUserAgent())
    }

    /**
     * Retrieves the system user agent for testing. The production implementation calls into `WebSettings` which throws
     * an UnsupportedOperationException during Robolectric tests. We can't mock static methods so we stub the method.
     * Note that we must also override similar behavior in android-components: see
     * [EngineVariantFunctionality.preventCrashFromDefaultUserAgent].
     *
     * We have to override two implementations because the a-c user agent implementation doesn't allow us to modify the
     * SystemEngine default user agent when using the Gecko implementation: see
     * https://github.com/mozilla-mobile/android-components/pull/931#issuecomment-498449477 for details.
     */
    override fun getSystemUserAgent(): String =
            "Mozilla/5.0 (Linux; Android 7.1.2) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Focus/2.2.0.2 Chrome/59.0.3071.125 Mobile Safari/537.36"

    /**
     * Retrieves the system user agent for testing. The production implementation calls into `WebSettings` which throws
     * an UnsupportedOperationException during Robolectric tests. We can't mock static methods so we stub the method.
     */
    override fun getEngineViewVersion() = EngineVersion(1, 1, 1, "dummyVersion")

    /**
     * This is used to disable ping upload when running tests.
     */
    override fun setGleanUpload() {
        WorkManagerTestInitHelper.initializeTestWorkManager(applicationContext)
        Glean.setUploadEnabled(false)
    }
}
