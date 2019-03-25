/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.tabs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_tabstray.*
import mozilla.components.feature.tabs.tabstray.TabsFeature
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.architecture.FocusOnShowDelegate
import org.mozilla.tv.firefox.ext.requireWebRenderComponents
import org.mozilla.tv.firefox.ext.serviceLocator

/**
 * A fragment for displaying the tabs tray.
 */
class TabsTrayFragment : Fragment() {
    private var tabsFeature: TabsFeature? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_tabstray, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tabsFeature = TabsFeature(
                tabsTray,
                requireWebRenderComponents.sessionManager,
                requireWebRenderComponents.tabsUseCases,
                ::closeTabsTray)

        addTab.setOnClickListener {
            val tabsUseCases = requireWebRenderComponents.tabsUseCases
            tabsUseCases.addTab.invoke("about:blank", selectTab = true)
        }
    }

    override fun onStart() {
        super.onStart()

        tabsFeature?.start()
    }

    override fun onStop() {
        super.onStop()

        tabsFeature?.stop()
    }

    private fun closeTabsTray() {
        serviceLocator!!.screenController.handleMenu(fragmentManager!!)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        FocusOnShowDelegate().onHiddenChanged(this, hidden)
        super.onHiddenChanged(hidden)
    }

    companion object {
        const val FRAGMENT_TAG = "tabs"

        @JvmStatic
        fun create() = TabsTrayFragment()
    }
}
