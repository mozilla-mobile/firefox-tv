/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.app.Application
import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.test.runner.AndroidJUnitRunner
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import org.mozilla.tv.firefox.utils.ServiceLocator
import kotlin.reflect.full.companionObjectInstance
import android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage

/**
 * A JUnit test runner that initializes the test environment for our on-device tests.
 */
class FirefoxOnDeviceTestRunner : AndroidJUnitRunner() {

    private lateinit var app: Application
    private lateinit var classLoader: ClassLoader

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, FirefoxOnDeviceTestApplication::class.java.name, context).also {
            app = it
            classLoader = cl!!
        }
    }

    /**
     * See [TestDependencyProvider] doc for an explanation of why we do this
     */
    override fun onCreate(arguments: Bundle?) {
        fun ServiceLocator?.swapIfNotNull() {
            this ?: return
            TestDependencyProvider.serviceLocator = this
        }

        val classString = arguments?.extractClass()
        val testClass = classString?.let { classLoader.loadClass(it).kotlin }
        val dependencyProvider = testClass?.companionObjectInstance as? TestDependencyFactory
        val fakeServiceLocator = dependencyProvider?.createServiceLocator(app)
        fakeServiceLocator.swapIfNotNull()

        super.onCreate(arguments)

    /**
    * This flag sets a preferences to keep the screen on between tests which addresses timing issues seen on the device.
    */
        ActivityLifecycleMonitorRegistry.getInstance().addLifecycleCallback(object : ActivityLifecycleCallback {
            override fun onActivityLifecycleChanged(activity: Activity, stage: Stage) {
                if (stage === Stage.PRE_ON_CREATE) {
                    activity.window.addFlags(FLAG_KEEP_SCREEN_ON)
                }
            }
        })
    }
}

/**
 * Tests that require faked dependencies should include companion objects that
 * implement this interface
 */
interface TestDependencyFactory {
    fun createServiceLocator(app: Application): ServiceLocator
}

/**
 * Retrieves the class being tested from Espresso args
 *
 * Note that this is an implementation detail of Espresso, and could be a likely
 * point of failure on version updates
 */
private fun Bundle.extractClass(): String? {
    // The class is stored in the format of {class}#{test}
    // e.g., org.mozilla.tv.firefox.ui.PocketBasicUserFlowTest#pocketBasicUserFlowTest
    return this.getString("class")?.split("#")?.firstOrNull()
    }
