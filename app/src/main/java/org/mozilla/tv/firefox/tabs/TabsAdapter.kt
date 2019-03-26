/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.tabs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import mozilla.components.browser.session.Session
import mozilla.components.concept.tabstray.TabsTray
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.tv.firefox.R

const val ADD_VIEW_TYPE = 300
const val SESSION_VIEW_TYPE = 200

/**
 * RecyclerView adapter implementation to display a list/grid of tabs.
 */
@Suppress("TooManyFunctions")
class TabsAdapter(
    delegate: Observable<TabsTray.Observer> = ObserverRegistry()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    TabsTray,
    Observable<TabsTray.Observer> by delegate {

    internal lateinit var tabsTray: WebRenderTabsTray

    private val holders = mutableListOf<TabViewHolder>()
    private var sessions: List<Session> = listOf()
    private var selectedIndex: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == ADD_VIEW_TYPE) {
            return TabPlusHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.tabstray_add_item,
                            parent,
                            false),
                    tabsTray
            )
        } else {
            return TabViewHolder(
                    LayoutInflater.from(parent.context).inflate(
                            R.layout.tabstray_item,
                            parent,
                            false),
                    tabsTray
            ).also {
                holders.add(it)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == sessions.size) {
            return ADD_VIEW_TYPE
        }
        return SESSION_VIEW_TYPE
    }

    override fun getItemCount() = sessions.size + 1 // plus one for add VH

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is TabViewHolder) {
            holder.bind(sessions[position], position == selectedIndex, this)
        } else {
            (holder as TabPlusHolder).bind()
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is TabViewHolder)
            holder.unbind()
    }

    fun unsubscribeHolders() {
        holders.forEach { it.unbind() }
        holders.clear()
    }

    override fun displaySessions(sessions: List<Session>, selectedIndex: Int) {
        this.sessions = sessions
        this.selectedIndex = selectedIndex
        notifyDataSetChanged()
    }

    override fun updateSessions(sessions: List<Session>, selectedIndex: Int) {
        this.sessions = sessions
        this.selectedIndex = selectedIndex
    }

    override fun onSessionsInserted(position: Int, count: Int) =
            notifyItemRangeInserted(position, count)

    override fun onSessionsRemoved(position: Int, count: Int) =
            notifyItemRangeRemoved(position, count)

    override fun onSessionMoved(fromPosition: Int, toPosition: Int) =
            notifyItemMoved(fromPosition, toPosition)

    override fun onSessionsChanged(position: Int, count: Int) =
            notifyItemRangeChanged(position, count, null)
}
