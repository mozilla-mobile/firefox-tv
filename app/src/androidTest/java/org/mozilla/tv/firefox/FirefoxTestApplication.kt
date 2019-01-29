/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.os.StrictMode
import org.mozilla.tv.firefox.TestDependencyProvider.serviceLocator
import org.mozilla.tv.firefox.utils.ServiceLocator

class FirefoxTestApplication : FirefoxApplication() {

    override fun createServiceLocator(): ServiceLocator {
        return TestDependencyProvider.serviceLocator ?: super.createServiceLocator()
    }

    override fun enableStrictMode() {
        // This method duplicates some code, but due to 1) the quantity of code
        // required to build a clean solution compared to the few lines
        // duplicated, and 2) the low risk nature of test only code, duplication
        // was determined to be a better solution in this instance
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll()

        // Log instead of showing a dialog during Espresso tests. This is because
        // dialogs present issues when automating test runs, and OkHttp causes
        // StrictMode violations on some devices.  See #1362
        threadPolicyBuilder.penaltyLog()
        vmPolicyBuilder.penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}

/**
 * Used to provide fake dependencies to the Application at startup.
 *
 * [FirefoxTestApplication.createServiceLocator] checks here for a custom
 * [ServiceLocator], then returns super if it is null. This allows individual
 * tests to create a service locator, thus substituting their own fakes into
 * tests.
 *
 * Note that Application#onCreate is called by Espresso before any code in an
 * instance of any test class is run. [serviceLocator] must be provisioned from
 * [FirefoxTestRunner] for it to be used.
 *
 * *IMPORTANT NOTE:* as currently implemented, multiple tests declared within the
 * same class will share dependencies. We generally do not write more than one
 * test in each Espresso file, so this is an acceptable limitation at this time.
 * Should this requirement change, update code in [FirefoxTestRunner]. *Do not
 * declare multiple tests in the same class* when using [TestDependencyProvider].
 */
object TestDependencyProvider {
    var serviceLocator: ServiceLocator? = null
}
