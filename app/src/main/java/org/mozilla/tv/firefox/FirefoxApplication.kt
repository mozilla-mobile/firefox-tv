/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox

import android.os.StrictMode
import androidx.annotation.VisibleForTesting
import android.webkit.WebSettings
import androidx.annotation.VisibleForTesting.PRIVATE
import com.amazon.device.messaging.ADM
import mozilla.appservices.Megazord
import mozilla.components.concept.engine.utils.EngineVersion
import mozilla.components.concept.push.PushProcessor
import mozilla.components.lib.fetch.okhttp.OkHttpClient
import mozilla.components.service.glean.Glean
import mozilla.components.support.base.log.Log
import mozilla.components.support.base.log.sink.AndroidLogSink
import mozilla.components.support.ktx.android.content.runOnlyInMainProcess
import mozilla.components.support.rusthttp.RustHttpConfig
import org.mozilla.tv.firefox.components.locale.LocaleAwareApplication
import org.mozilla.tv.firefox.ext.webRenderComponents
import org.mozilla.tv.firefox.telemetry.SentryIntegration
import org.mozilla.tv.firefox.webrender.VisibilityLifeCycleCallback
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.BuildConstants
import org.mozilla.tv.firefox.utils.OkHttpWrapper
import org.mozilla.tv.firefox.utils.ServiceLocator
import org.mozilla.tv.firefox.webrender.WebRenderComponents

private const val DEFAULT_LOGTAG = "FFTV"

open class FirefoxApplication : LocaleAwareApplication() {
    lateinit var visibilityLifeCycleCallback: VisibilityLifeCycleCallback
        private set

    @VisibleForTesting(otherwise = PRIVATE) // See the TestFirefoxApplication impl for why this method exists.
    protected open fun getSystemUserAgent(): String = WebSettings.getDefaultUserAgent(this)

    // See the TestFirefoxApplication impl for why this method exists.
    open fun getEngineViewVersion(): EngineVersion = webRenderComponents.engine.version

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

        enableAndroidComponentsLogging() // In theory, the Gecko process may use this logger so init for all processes.

        // If this is not the main process then do not continue with the initialization here. Everything that
        // follows only needs to be done in our app's main process and should not be done in other processes like
        // a GeckoView child process or the crash handling process. Most importantly we never want to end up in a
        // situation where we create a GeckoRuntime from the Gecko child process
        applicationContext.runOnlyInMainProcess {
            serviceLocator = createServiceLocator()

            // Enable crash reporting. Don't add anything above here because if it crashes, we won't know.
            SentryIntegration.init(this, serviceLocator.settingsRepo)

            initRustDependencies()
            initGlean()
            TelemetryIntegration.INSTANCE.init(this)
            initFretboard()
            initPush()

            enableStrictMode()

            visibilityLifeCycleCallback = VisibilityLifeCycleCallback(this).also {
                registerActivityLifecycleCallbacks(it)
            }
        }
    }

    private fun initRustDependencies() {
        Megazord.init()
        RustHttpConfig.setClient(lazy { OkHttpClient(OkHttpWrapper.client, this) })
    }

    private fun initFretboard() {
        with(serviceLocator.fretboardProvider) {
            loadExperiments()
            updateExperiments()
        }
    }

    private fun initPush() {
        // Only use push feature if ADM is available on this device
        if (ADM(applicationContext).isSupported) {
            PushProcessor.install(serviceLocator.pushFeature)
        } else {
            android.util.Log.i(DEFAULT_LOGTAG, "ADM is not available on this device.")
        }
    }

    // This method is used to call Glean.setUploadEnabled. During the tests, this is
    // overridden to disable ping upload.
    @VisibleForTesting
    protected open fun setGleanUpload() {
        serviceLocator.settingsRepo.dataCollectionEnabled.observeForever { collectionEnabled ->
            if (collectionEnabled != null) {
                // This needs to be called before Glean.initialize, or we risk 1) not
                // sending startup data, or 2) sending even when the user has toggled
                // off data collection
                Glean.setUploadEnabled(collectionEnabled)
            }
        }
    }

    private fun initGlean() {
        setGleanUpload()
        Glean.initialize(applicationContext)
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

private fun enableAndroidComponentsLogging() {
    Log.addSink(AndroidLogSink(defaultTag = DEFAULT_LOGTAG))
}
