/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_new_settings.*
import org.mozilla.focus.R

/** The home fragment which displays the navigation tiles of the app. */
class NewSettingsFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?) =
            inflater!!.inflate(R.layout.fragment_new_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // todo: saved instance state?
        val linearLayout = view.findViewById<LinearLayout>(R.id.new_settings_list)
        val lparams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val text1 = TextView(context)
        text1.text = "SETTINGS 1"
        text1.layoutParams = lparams
        val text2 = TextView(context)
        text2.text = "SETTINGS 2"
        text2.layoutParams = lparams


        val text3 = TextView(context)
        text3.text = "SETTINGS 3"
        text3.layoutParams = lparams

        linearLayout.addView(text1)
        linearLayout.addView(text2)
        linearLayout.addView(text3)
    }

    companion object {
        const val FRAGMENT_TAG = "new_settings"

        @JvmStatic
        fun create() = NewSettingsFragment()
    }
}