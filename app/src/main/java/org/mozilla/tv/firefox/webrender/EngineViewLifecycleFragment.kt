/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender

import android.os.Bundle
import androidx.annotation.UiThread
import android.view.View
import android.webkit.WebView
import mozilla.components.concept.engine.EngineView
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.components.locale.LocaleAwareFragment
import org.mozilla.tv.firefox.components.locale.LocaleManager
import org.mozilla.tv.firefox.ext.onPauseIfNotNull
import org.mozilla.tv.firefox.ext.onResumeIfNotNull
import java.util.Locale

/**
 * Initializes and manages the lifecycle of an [EngineView] instance inflated by the super class.
 * It was originally inspired by Android's WebViewFragment.
 *
 * To use this class, override it with a super-class that inflates a layout with an [EngineView] with
 * @id=webview.
 *
 * Notes on alternative implementations: while composability is generally preferred over
 * inheritance, there are too many entry points to use this with composition (i.e. all lifecycle
 * methods) so it's more error-prone and we stuck with this implementation. Composability was
 * tried in PR #428.
 */
abstract class EngineViewLifecycleFragment : LocaleAwareFragment() {
    /**
     * The [EngineView] in use by this fragment. If the value is non-null, the EngineView is present
     * in the view hierarchy, null otherwise.
     */
    var engineView: EngineView? = null
        @UiThread get // On a background thread, it may have been removed from the view hierarchy.
        private set

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        engineView = (view.findViewById<View>(R.id.engineView) as EngineView).apply {
            onEngineViewCreated(this)
        }
    }

    open fun onEngineViewCreated(engineView: EngineView) = Unit

    override fun onStop() {
        super.onStop()

        // NB: onStop unexpectedly calls onPause: see below.
        //
        // When the user says "Alexa pause [the video]", the Activity will be paused/resumed while
        // Alexa handles the request. If the EngineView is paused during video playback, the video will
        // have poor behavior (on YouTube the screen goes black, may rebuffer, and may lose the voice
        // command). Unfortunately, there does not appear to be any way to prevent this other than
        // to not call EngineView.onPause so we pause the EngineView later, here in onStop, when it isn't
        // affected by Alexa voice commands. Luckily, Alexa pauses the video for us. afaict, on
        // Fire TV, `onPause` without `onStop` isn't called very often so I don't think there will
        // be many side effects (#936).
        //
        // The problem is not reproducible when onPause is called here in onStop (even if pauseTimers is
        // called in onPause, in the android-components library).
        engineView!!.onPauseIfNotNull() // internally calls EngineView.onPause: see impl for details.
    }

    override fun onStart() {
        super.onStart()

        // NB: onStart unexpectedly calls onResume: see onStop for details.
        engineView!!.onResumeIfNotNull()
    }

    override fun applyLocale() {
        val context = context!!
        val localeManager = LocaleManager.getInstance()
        if (!localeManager.isMirroringSystemLocale(context)) {
            val currentLocale = localeManager.getCurrentLocale(context)
            Locale.setDefault(currentLocale)

            val resources = context.resources
            val config = resources.configuration
            config.setLocale(currentLocale)

            @Suppress("DEPRECATION") // TODO: This is non-trivial to fix: #850.
            resources.updateConfiguration(config, null)
        }
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See focus-android issue #666.
        val unneeded = WebView(getContext())
        unneeded.destroy()
    }
}
