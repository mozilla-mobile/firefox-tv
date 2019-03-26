/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.tabs

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.session.Session
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import org.mozilla.tv.firefox.R

/**
 * A RecyclerView ViewHolder implementation for "tab" items.
 */
class TabViewHolder(
    itemView: View,
    private val tabsTray: WebRenderTabsTray
) : RecyclerView.ViewHolder(itemView), Session.Observer {
    private val cardView: CardView = (itemView as CardView).apply {
        elevation = tabsTray.styling.itemElevation
    }
    private val tabView: TextView = itemView.findViewById(R.id.tabstray_url)
    private val thumbnailView: ImageView = itemView.findViewById(R.id.tabstray_thumbnail)

    private var session: Session? = null

    /**
     * Displays the data of the given session and notifies the given observable about events.
     */
    fun bind(session: Session, isSelected: Boolean, observable: Observable<TabsTray.Observer>) {
        this.session = session.also { it.register(this) }

        val title = if (session.title.isNotEmpty()) {
            session.title
        } else {
            session.url
        }

        tabView.text = title

        itemView.setOnClickListener {
            observable.notifyObservers { onTabSelected(session) }
        }

        //TODO: close tab (long press?)
        // observable.notifyObservers { onTabClosed(session) }

        if (isSelected) {
            tabView.setTextColor(tabsTray.styling.selectedItemTextColor)
            cardView.setCardBackgroundColor(tabsTray.styling.selectedItemBackgroundColor)
        } else {
            tabView.setTextColor(tabsTray.styling.itemTextColor)
            cardView.setCardBackgroundColor(tabsTray.styling.itemBackgroundColor)
        }

        thumbnailView.setImageBitmap(session.thumbnail)
    }

    /**
     * The attached view no longer needs to display any data.
     */
    fun unbind() {
        session?.unregister(this)
    }

    override fun onUrlChanged(session: Session, url: String) {
        tabView.text = url
    }
}
