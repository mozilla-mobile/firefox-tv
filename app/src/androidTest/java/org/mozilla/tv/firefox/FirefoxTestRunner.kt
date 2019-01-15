/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.support.test.runner.AndroidJUnitRunner
import org.mozilla.tv.firefox.helpers.FakePocketVideoRepoProvider
import org.mozilla.tv.firefox.ui.PocketBasicUserFlowTest
import org.mozilla.tv.firefox.ui.screenshots.PocketErrorTest

class FirefoxTestRunner : AndroidJUnitRunner() {

    private lateinit var app: Application

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, FirefoxTestApplication::class.java.name, context).also {
            app = it
        }
    }

    override fun onCreate(arguments: Bundle?) {
        when (arguments.extractClass()) {
            // Set up class specific dependencies to be used in the ServiceLocator
            PocketBasicUserFlowTest::class.java.name -> {
                TestDependencyProvider.pocketVideoRepo = FakePocketVideoRepoProvider.fakedPocketRepo
            }
            PocketErrorTest::class.java.name -> {
                TestDependencyProvider.pocketVideoRepo = FakePocketVideoRepoProvider.fakedPocketRepo
            }
        }
        super.onCreate(arguments)
    }
}

/**
 * Retrieves the class being tested from Espresso args
 *
 * Note that this is an implementation detail of Espresso, and could be a likely
 * point of failure on version updates
 */
private fun Bundle?.extractClass(): String {
    // The class is stored in the format of {class}#{test}
    // e.g., org.mozilla.tv.firefox.ui.PocketBasicUserFlowTest#pocketBasicUserFlowTest
    return this!!.getString("class")!!.split("#").first()
}
