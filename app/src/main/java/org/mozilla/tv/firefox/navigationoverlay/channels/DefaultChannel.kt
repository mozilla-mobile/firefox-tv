/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.view.ViewGroup
import android.widget.TextView
import kotlinx.android.synthetic.main.default_channel.view.channelTitle

class DefaultChannel(
        val containerView: ViewGroup,
        private val adapter: DefaultChannelAdapter
) {

    fun setTitle(title: CharSequence) {
        titleView.text = title
    }

    fun setContents(tileData: List<ChannelTile>) {
        adapter.setTiles(tileData)
    }

    private val titleView: TextView = containerView.channelTitle
}
