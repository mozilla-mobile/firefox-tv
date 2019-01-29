/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.support.test.runner.AndroidJUnitRunner
import org.mozilla.tv.firefox.utils.ServiceLocator
import kotlin.reflect.full.companionObjectInstance

class FirefoxTestRunner : AndroidJUnitRunner() {

    private lateinit var app: Application
    private lateinit var classLoader: ClassLoader

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, FirefoxTestApplication::class.java.name, context).also {
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
