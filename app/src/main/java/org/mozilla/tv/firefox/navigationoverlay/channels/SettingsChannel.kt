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

class SettingsChannel {
    companion object {
        fun makeAdapter(): RecyclerView.Adapter<SettingsTileHolder> {
            return SettingsAdapter()
        }
    }

    enum class SettingsItem {
        DATA_COLLECTION, CLEAR_COOKIES, ABOUT, PRIVACY_POLICY
    }

    // TODO: Make TileViewHolder sealed class unifying Tile types
    private class SettingsAdapter: RecyclerView.Adapter<SettingsTileHolder>() {

        private val settingsItems = arrayOf(
                SettingsChannel.SettingsItem.DATA_COLLECTION,
                SettingsChannel.SettingsItem.CLEAR_COOKIES,
                SettingsChannel.SettingsItem.ABOUT,
                SettingsChannel.SettingsItem.PRIVACY_POLICY
        )

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SettingsTileHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.settings_tile, parent, false)
        )

        override fun getItemCount(): Int {
            return settingsItems.size
        }

        override fun onBindViewHolder(holder: SettingsTileHolder, position: Int) = with(holder) {
            // TODO: load from settings item type
            iconView.setImageResource(R.drawable.ic_about)
            titleView.setText(R.string.preference_mozilla_telemetry2)
        }

    }

    class SettingsTileHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView = itemView.tile_icon
        val titleView = itemView.tile_title
    }
}
