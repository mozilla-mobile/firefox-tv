/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.home_tile.view.*
import org.mozilla.tv.firefox.R

class SettingsChannelAdapter : RecyclerView.Adapter<SettingsTileHolder>() {
    private val settingsItems = arrayOf(
            SettingsItem(
                    SettingsTileType.DATA_COLLECTION,
                    R.drawable.lb_ic_sad_cloud,
                    R.string.preference_mozilla_telemetry2),
            SettingsItem(
                    SettingsTileType.CLEAR_COOKIES,
                    R.drawable.mozac_ic_delete,
                    R.string.settings_cookies_dialog_title),
            SettingsItem(
                    SettingsTileType.ABOUT,
                    R.drawable.mozac_ic_info,
                    R.string.menu_about),
            SettingsItem(
                    SettingsTileType.PRIVACY_POLICY,
                    R.drawable.mozac_ic_lock,
                    R.string.preference_privacy_notice)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SettingsTileHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.settings_tile, parent, false)
    )

    override fun getItemCount(): Int {
        return settingsItems.size
    }

    override fun onBindViewHolder(holder: SettingsTileHolder, position: Int) = with(holder) {
        iconView.setImageResource(settingsItems[position].imgRes)
        titleView.setText(settingsItems[position].titleRes)
    }

}

class SettingsTileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val iconView = itemView.tile_icon
    val titleView = itemView.tile_title
}

private enum class SettingsTileType {
    DATA_COLLECTION, CLEAR_COOKIES, ABOUT, PRIVACY_POLICY
}

private data class SettingsItem(val type: SettingsTileType, val imgRes: Int, val titleRes: Int)
