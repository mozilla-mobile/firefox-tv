/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.tabs

import android.app.AlertDialog
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.session.Session
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.ext.webRenderComponents

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

        itemView.setOnLongClickListener {
            val builder = AlertDialog.Builder(tabsTray.context)
            builder.setTitle(title)

            builder.setPositiveButton("Close") { dialog, _ ->
                if (tabsTray.context.webRenderComponents.sessionManager.size > 1) {
                    observable.notifyObservers { onTabClosed(session) }
                } else {
                    Toast.makeText(tabsTray.context, "You must have at least one active tab", Toast.LENGTH_SHORT).show()
                }

                dialog.dismiss()
            }

            builder.create().show()

            true
        }

        if (isSelected) {
            tabView.setTextColor(tabsTray.styling.selectedItemTextColor)
            cardView.background = tabsTray.context.getDrawable(R.drawable.tab_tile_background_selected)
        } else {
            tabView.setTextColor(tabsTray.styling.itemTextColor)
            cardView.background = tabsTray.context.getDrawable(R.drawable.tab_tile_background)
        }

        thumbnailView.setImageBitmap(session.thumbnail)
    }

    /**
     * The attached view no longer needs to display any data.
     */
    fun unbind() {
        session?.unregister(this)
        cardView.clearFocus()
    }

    override fun onUrlChanged(session: Session, url: String) {
        tabView.text = url
    }
}

class TabPlusHolder(
    itemView: View,
    private val tabsTray: WebRenderTabsTray
) : RecyclerView.ViewHolder(itemView) {

    fun bind() {
        (itemView as CardView).apply {
            elevation = tabsTray.styling.itemElevation
        }

        itemView.background = tabsTray.context.getDrawable(R.drawable.navigation_button_background)
        itemView.setOnClickListener {
            tabsTray.tabsUseCases.addTab.invoke("about:blank", selectTab = true)
        }
    }
}
