/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.support.test.runner.AndroidJUnitRunner

class FirefoxTestRunner : AndroidJUnitRunner() {

    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application {
        return super.newApplication(cl, FirefoxTestApplication::class.java.name, context)
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
