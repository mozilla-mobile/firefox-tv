/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.reactivex.Observable
import kotlinx.android.synthetic.main.default_channel.view.channelSubtitle
import kotlinx.android.synthetic.main.default_channel.view.channelTitle

/**
 * A data container representing a standard channel on the homescreen. This class references a Context and must
 * be nulled when its lifecycle ends.
 */
class DefaultChannel(
    val channelContainer: ViewGroup,
    private val adapter: DefaultChannelAdapter
) {

    val focusChangeObservable get() = adapter.focusChangeObservable

    val removeTileEvents: Observable<ChannelTile> = adapter.removeEvents

    fun setTitle(title: CharSequence) {
        titleView.text = title
    }

    fun setSubtitle(subtitle: CharSequence?) {
        subtitleView.visibility = if (subtitle.isNullOrEmpty()) {
            View.GONE
        } else {
            View.VISIBLE
        }

        subtitleView.text = subtitle
    }

    fun setContents(tileData: List<ChannelTile>) {
        adapter.submitList(tileData)
    }

    private val titleView: TextView = channelContainer.channelTitle
    private val subtitleView: TextView = channelContainer.channelSubtitle
}
