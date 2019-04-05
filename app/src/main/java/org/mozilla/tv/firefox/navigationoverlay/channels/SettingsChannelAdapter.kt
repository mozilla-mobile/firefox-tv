/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.home_tile.view.*
import kotlinx.android.synthetic.main.settings_tile.view.*
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.utils.URLs

class SettingsChannelAdapter(
    private val loadUrl: (String) -> Unit,
    private val showSettings: (SettingsType) -> Unit
) : RecyclerView.Adapter<SettingsTileHolder>() {
    private val settingsItems = arrayOf(
        SettingsItem(
            SettingsType.DATA_COLLECTION,
            R.drawable.ic_data_collection,
            R.string.preference_mozilla_telemetry2,
            R.id.settings_tile_telemetry),
        SettingsItem(
            SettingsType.CLEAR_COOKIES,
            R.drawable.mozac_ic_delete,
            R.string.settings_cookies_dialog_title,
            R.id.settings_tile_cleardata),
        SettingsItem(
            SettingsType.ABOUT,
            R.drawable.mozac_ic_info,
            R.string.menu_about,
            R.id.settings_tile_about),
        SettingsItem(
            SettingsType.PRIVACY_POLICY,
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
                SettingsType.DATA_COLLECTION -> showSettings(type)
                SettingsType.CLEAR_COOKIES -> showSettings(type)
                SettingsType.ABOUT -> loadUrl(URLs.URL_ABOUT)
                SettingsType.PRIVACY_POLICY -> loadUrl(URLs.PRIVACY_NOTICE_URL)
            }
        }
        itemView.contentDescription = itemView.context.getString(itemData.titleRes)
        itemView.id = itemData.viewId // Add ids for testing
    }
}

class SettingsTileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val iconView = itemView.settings_icon
    val titleView = itemView.settings_title
}

enum class SettingsType {
    DATA_COLLECTION, CLEAR_COOKIES, ABOUT, PRIVACY_POLICY
}

private data class SettingsItem(val type: SettingsType, val imgRes: Int, val titleRes: Int, val viewId: Int)
