/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.os.StrictMode
import org.mozilla.tv.firefox.helpers.PocketRepoFaker
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.utils.ServiceLocator

class FirefoxTestApplication : FirefoxApplication() {

    override fun createServiceLocator() = object : ServiceLocator(this) {
        override val pocketRepo = PocketRepoFaker.fakedPocketRepo
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

    fun pushPocketRepoState(state: PocketVideoRepo.FeedState) = PocketRepoFaker.fakedPocketRepoState.postValue(state)
}
