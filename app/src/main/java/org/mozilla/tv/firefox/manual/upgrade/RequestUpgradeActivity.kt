/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.manual.upgrade

import android.app.Activity
import android.os.Bundle
import kotlinx.android.synthetic.main.request_manual_upgrade.manual_upgrade_button_negative
import kotlinx.android.synthetic.main.request_manual_upgrade.manual_upgrade_button_positive
import kotlinx.android.synthetic.main.request_manual_upgrade.manual_upgrade_description
import kotlinx.android.synthetic.main.request_manual_upgrade.manual_upgrade_title
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.utils.IntentUtils

// TODO This file can be removed from master as soon as it has been released. See #2794

/**
 * Displays an activity that asks the user to manually upgrade the app.
 */
class RequestUpgradeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.request_manual_upgrade)

        manual_upgrade_title.text = resources.getString(
            R.string.update_prompt_header,
            resources.getString(R.string.firefox_tv_brand_name_short)
        )
        manual_upgrade_description.text = resources.getString(
            R.string.update_prompt_instruction,
            resources.getString(R.string.firefox_tv_brand_name_short),
            resources.getString(R.string.firefox_tv_brand_name_short)
        )

        manual_upgrade_button_positive.setOnClickListener {
            IntentUtils.openFftvStorePage(this)
        }
        manual_upgrade_button_negative.setOnClickListener {
            this.finish()
        }
    }
}

/**
 * Displays an activity that forces the user to manually upgrade the app.
 */
class ForceUpgradeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.force_manual_upgrade)

        manual_upgrade_title.text = resources.getString(
            R.string.update_prompt_header,
            resources.getString(R.string.firefox_tv_brand_name_short)
        )
        manual_upgrade_description.text = resources.getString(
            R.string.update_prompt_instruction,
            resources.getString(R.string.firefox_tv_brand_name_short),
            resources.getString(R.string.firefox_tv_brand_name_short)
        )

        manual_upgrade_button_positive.setOnClickListener {
            IntentUtils.openFftvStorePage(this)
        }
    }
}
