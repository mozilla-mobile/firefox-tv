/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.os.StrictMode
import android.preference.PreferenceManager
import android.support.annotation.VisibleForTesting
import android.webkit.WebSettings
import org.mozilla.tv.firefox.components.locale.LocaleAwareApplication
import org.mozilla.tv.firefox.webrender.VisibilityLifeCycleCallback
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.BuildConstants
import org.mozilla.tv.firefox.utils.OkHttpWrapper
import org.mozilla.tv.firefox.utils.ServiceLocator
import org.mozilla.tv.firefox.webrender.WebRenderComponents

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
    val components by lazy { WebRenderComponents(this, getSystemUserAgent()) }
    lateinit var serviceLocator: ServiceLocator

    override fun onCreate() {
        super.onCreate()

        PreferenceManager.setDefaultValues(this, R.xml.settings, false)

        serviceLocator = createServiceLocator()

        TelemetryIntegration.INSTANCE.init(this)

        with(serviceLocator.fretboardProvider) {
            loadExperiments()
            updateExperiments()
        }

        enableStrictMode()

        visibilityLifeCycleCallback = VisibilityLifeCycleCallback(this).also {
            registerActivityLifecycleCallbacks(it)
        }
    }

    // ServiceLocator needs to be created in onCreate in order to accept Application
    // as an argument. Because of this, if we override `val serviceLocator` but
    // accidentally called `super.onCreate`, it would overwrite our test
    // ServiceLocator. To prevent this land mine, we override this method instead
    open fun createServiceLocator() = ServiceLocator(this)

    protected open fun enableStrictMode() {
        // Android/WebView sometimes commit strict mode violations, see e.g.
        // https://github.com/mozilla-mobile/focus-android/issues/660
        if (BuildConstants.isReleaseBuild) {
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
        serviceLocator.sessionManager.onLowMemory()
        // If you need to dump more memory, you may be able to clear the Picasso cache.
    }
}
