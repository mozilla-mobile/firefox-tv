/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.os.StrictMode
import org.mozilla.tv.firefox.ext.toUri
import org.mozilla.tv.firefox.pocket.PocketEndpoint
import org.mozilla.tv.firefox.pocket.PocketFeedStateMachine
import org.mozilla.tv.firefox.pocket.PocketVideoRepo
import org.mozilla.tv.firefox.pocket.PocketViewModel
import org.mozilla.tv.firefox.settings.SettingsRepo
import org.mozilla.tv.firefox.utils.BuildConfigDerivables
import org.mozilla.tv.firefox.utils.ServiceLocator

class FirefoxTestApplication : FirefoxApplication() {

    private val pocketEndpoint = object : PocketEndpoint("VERSION", "www.mock.com".toUri()) {
        override suspend fun getRecommendedVideos(): List<PocketViewModel.FeedItem.Video>? {
            return PocketViewModel.noKeyPlaceholders
        }
    }

    private val pocketVideoRepoState = MutableLiveData<PocketVideoRepo.FeedState>()

    val localeIsEnglish: () -> Boolean = { true }
    val settingsRepo: SettingsRepo by lazy { serviceLocator.settingsRepo }

    private val pocketVideoRepo = object : PocketVideoRepo(
        pocketEndpoint,
        PocketFeedStateMachine(),
        localeIsEnglish,
        BuildConfigDerivables(localeIsEnglish)
    ) {
        override val feedState: LiveData<FeedState>
            get() = pocketVideoRepoState
    }

    override fun createServiceLocator() = object : ServiceLocator(this) {
        override val pocketRepo = pocketVideoRepo
    }

    fun pushPocketRepoState(state: PocketVideoRepo.FeedState) = pocketVideoRepoState.postValue(state)

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
