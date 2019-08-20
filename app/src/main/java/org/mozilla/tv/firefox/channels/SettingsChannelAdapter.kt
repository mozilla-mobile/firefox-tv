/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.settings_tile.view.*
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.URLs

class SettingsChannelAdapter(
    private val loadUrl: (String) -> Unit,
    private val showSettings: (SettingsScreen) -> Unit
) : RecyclerView.Adapter<SettingsTileHolder>() {
    private val settingsItems = arrayOf(
        SettingsItem(
            SettingsScreen.DATA_COLLECTION,
            R.drawable.ic_data_collection,
            R.string.preference_mozilla_telemetry2,
            R.id.settings_tile_telemetry),
        SettingsItem(
            SettingsScreen.CLEAR_COOKIES,
            R.drawable.mozac_ic_delete,
            R.string.settings_cookies_dialog_title,
            R.id.settings_tile_cleardata),
        SettingsItem(
            SettingsButton.ABOUT,
            R.drawable.mozac_ic_info,
            R.string.menu_about,
            R.id.settings_tile_about),
        SettingsItem(
            SettingsButton.PRIVACY_POLICY,
            R.drawable.mozac_ic_globe,
            R.string.preference_privacy_notice,
            R.id.settings_tile_privacypolicy)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SettingsTileHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.settings_tile, parent, false)
    )

    override fun getItemCount(): Int {
        return settingsItems.size
    }

    override fun onBindViewHolder(holder: SettingsTileHolder, position: Int) = with(holder) {
        val itemData = settingsItems[position]
        iconView.setImageResource(itemData.imgRes)
        titleView.setText(itemData.titleRes)
        itemView.settings_cardview.setOnClickListener {
            when (val type = itemData.type) {
                SettingsScreen.DATA_COLLECTION -> showSettings(type as SettingsScreen)
                SettingsScreen.CLEAR_COOKIES -> showSettings(type as SettingsScreen)
                SettingsButton.ABOUT -> loadUrl(URLs.URL_ABOUT)
                SettingsButton.PRIVACY_POLICY -> loadUrl(URLs.PRIVACY_NOTICE_URL)
            }
            TelemetryIntegration.INSTANCE.settingsTileClickEvent(itemData.type)
        }
        itemView.contentDescription = itemView.context.getString(itemData.titleRes)
        itemView.id = itemData.viewId // Add ids for testing
    }
}

class SettingsTileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val iconView = itemView.settings_icon
    val titleView = itemView.settings_title
}

// We differentiate between Settings tiles that lead to other Settings screens, or are just buttons
interface SettingsTile
enum class SettingsScreen : SettingsTile {
    DATA_COLLECTION, CLEAR_COOKIES, FXA_PROFILE
}
enum class SettingsButton : SettingsTile {
        ABOUT, PRIVACY_POLICY
}

private data class SettingsItem(val type: SettingsTile, val imgRes: Int, val titleRes: Int, val viewId: Int)
