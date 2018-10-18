/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus

import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.annotation.VisibleForTesting
import android.webkit.WebSettings
import org.mozilla.focus.locale.LocaleAwareApplication
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.session.VisibilityLifeCycleCallback
import org.mozilla.focus.telemetry.TelemetryIntegration
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.OkHttpWrapper

open class FocusApplication : LocaleAwareApplication() {
    lateinit var visibilityLifeCycleCallback: VisibilityLifeCycleCallback
        private set

    // TODO: Replace with components implementation, which may remove need for test workaround:
    // https://github.com/mozilla-mobile/android-components/issues/899
    @VisibleForTesting // See TestFocusApplication for why this method exists.
    protected open fun getSystemUserAgent(): String = WebSettings.getDefaultUserAgent(this)

    /**
     * Reference to components needed by the application.
     *
     * We create this instance lazily because at the time FocusApplication gets constructed it is
     * not a valid Context object just yet (The Android system needs to call attachBaseContext()
     * first). Therefore we delay the creation so that the components can access and use the
     * application context at the time they get created.
     */
    val components by lazy { Components(this, getSystemUserAgent()) }

    override fun onCreate() {
        super.onCreate()

        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        enableStrictMode()

        SearchEngineManager.getInstance().init(this)

        TelemetryIntegration.init(this)

        visibilityLifeCycleCallback = VisibilityLifeCycleCallback(this).also {
            registerActivityLifecycleCallbacks(it)
        }
    }

    private fun enableStrictMode() {
        // Android/WebView sometimes commit strict mode violations, see e.g.
        // https://github.com/mozilla-mobile/focus-android/issues/660
        if (AppConstants.isReleaseBuild()) {
            return
        }

        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder().detectAll()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder().detectAll()

        threadPolicyBuilder.penaltyDialog()
        vmPolicyBuilder.penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }

    override fun onLowMemory() {
        super.onLowMemory()
        OkHttpWrapper.onLowMemory()
        // If you need to dump more memory, you may be able to clear the Picasso cache.
    }
}
