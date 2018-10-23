/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.annotation.VisibleForTesting
import android.webkit.WebSettings
import org.mozilla.tv.firefox.locale.LocaleAwareApplication
import org.mozilla.tv.firefox.search.SearchEngineManager
import org.mozilla.tv.firefox.session.VisibilityLifeCycleCallback
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.AppConstants
import org.mozilla.tv.firefox.utils.OkHttpWrapper
import org.mozilla.tv.firefox.utils.ServiceLocator

open class FirefoxApplication : LocaleAwareApplication() {
    lateinit var visibilityLifeCycleCallback: VisibilityLifeCycleCallback
        private set

    // TODO: Replace with components implementation, which may remove need for test workaround:
    // https://github.com/mozilla-mobile/android-components/issues/899
    @VisibleForTesting // See TestFocusApplication for why this method exists.
    protected open fun getSystemUserAgent(): String = WebSettings.getDefaultUserAgent(this)

    /**
     * Reference to components needed by the application.
     *
     * We create this instance lazily because at the time FirefoxApplication gets constructed it is
     * not a valid Context object just yet (The Android system needs to call attachBaseContext()
     * first). Therefore we delay the creation so that the components can access and use the
     * application context at the time they get created.
     */
    val components by lazy { Components(this, getSystemUserAgent()) }
    var serviceLocator = ServiceLocator()
        private set

    // [setTestServiceLocator] is used during testing to pass in a [ServiceLocator] with mocked dependencies.
    // This is particularly useful for mocking endpoint singletons.
    //
    // This should NEVER be used by production code.
    //
    // The fact that this method is visible to production code is a Bad Design™. A better solution
    // would be to load different versions of ServiceLocator using source sets, which would make it
    // impossible to abuse this method in production. At this time, however, we have no source set
    // for instrumentation testing, and introducing one just for this purpose seems like overkill.
    // We can take that approach in the future without significant changes if it becomes necessary.
    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun setTestServiceLocator(serviceLocator: ServiceLocator) {
        this.serviceLocator = serviceLocator
    }

    override fun onCreate() {
        super.onCreate()

        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        enableStrictMode()

        SearchEngineManager.getInstance().init(this)

        TelemetryIntegration.INSTANCE.init(this)

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
