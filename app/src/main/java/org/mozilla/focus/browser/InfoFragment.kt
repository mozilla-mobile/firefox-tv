/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import mozilla.components.browser.session.Session
import org.mozilla.focus.R
import org.mozilla.focus.ext.requireComponents
import org.mozilla.focus.engine.EngineViewLifecycleFragment

class InfoFragment : EngineViewLifecycleFragment(), Session.Observer {
    private var progressView: ProgressBar? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_info, container, false)
        progressView = view.findViewById(R.id.progress)

        applyLocale()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = Session(arguments!!.getString(ARGUMENT_URL))
        session.register(this, owner = this)

        val engineSession = requireComponents.sessionManager.getOrCreateEngineSession(session)
        engineSession.disableTrackingProtection()
        webView!!.render(engineSession)
    }

    override fun onProgress(session: Session, progress: Int) {
        progressView?.progress = progress
    }

    override fun onLoadingStateChanged(session: Session, loading: Boolean) {
        if (loading) {
            progressView?.announceForAccessibility(getString(R.string.accessibility_announcement_loading))
        }

        progressView?.visibility = if (loading) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    companion object {
        private const val ARGUMENT_URL = "url"

        fun create(url: String): InfoFragment {
            val arguments = Bundle()
            arguments.putString(ARGUMENT_URL, url)

            val fragment = InfoFragment()
            fragment.arguments = arguments

            return fragment
        }
    }
}
