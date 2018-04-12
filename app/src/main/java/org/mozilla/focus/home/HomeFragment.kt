/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.TypedValue
import android.view.ContextMenu
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.home_tile.*
import kotlinx.coroutines.experimental.CancellationException
import kotlinx.coroutines.experimental.Job
import org.mozilla.focus.R
import org.mozilla.focus.autocomplete.UrlAutoCompleteFilter
import org.mozilla.focus.telemetry.UrlTextInputLocation
import org.mozilla.focus.utils.OnUrlEnteredListener

private const val SETTINGS_ICON_IDLE_ALPHA = 0.4f
private const val SETTINGS_ICON_ACTIVE_ALPHA = 1.0f

/** The home fragment which displays the navigation tiles of the app. */
class HomeFragment : Fragment() {

    lateinit var urlBar: LinearLayout
    var onUrlEnteredListener = object : OnUrlEnteredListener {} // default impl does nothing.
    var onSettingsPressed: (() -> Unit)? = null
    val urlAutoCompleteFilter = UrlAutoCompleteFilter()

    /**
     * Used to cancel background->UI threads: we attach them as children to this job
     * and cancel this job at the end of the UI lifecycle, cancelling the children.
     */
    private lateinit var uiLifecycleCancelJob: Job

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_home, container, false)
        urlBar = rootView.findViewById(R.id.homeUrlBar)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // todo: saved instance state?
        uiLifecycleCancelJob = Job()

        initUrlInputView()

        settingsButton.alpha = SETTINGS_ICON_IDLE_ALPHA
        settingsButton.setImageResource(R.drawable.ic_settings) // Must be set in code for SVG to work correctly.
        settingsButton.setOnFocusChangeListener { v, hasFocus ->
            v.alpha = if (hasFocus) SETTINGS_ICON_ACTIVE_ALPHA else SETTINGS_ICON_IDLE_ALPHA
        }

        settingsButton.setOnClickListener { v ->
            onSettingsPressed?.invoke()
        }

        registerForContextMenu(tileContainer)

        tileContainer.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                val gridLayoutManager = recyclerView?.layoutManager as GridLayoutManager
                val lastVisibleItem = gridLayoutManager.findLastCompletelyVisibleItemPosition()
                // We add a scroll offset, revealing the next row to hint that there are more home tiles
                if (dy > 0 && getFocusedTilePosition() > lastVisibleItem) {
                    val scrollOffset = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP, home_tile.height.toFloat() / 2, context.resources.displayMetrics)
                    recyclerView.smoothScrollBy(0, scrollOffset.toInt())
                }
            }
        })
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        activity.menuInflater.inflate(R.menu.menu_context_hometile, menu)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiLifecycleCancelJob.cancel(CancellationException("Parent lifecycle has ended"))
    }

    override fun onResume() {
        super.onResume()
        urlAutoCompleteFilter.load(context)
    }

    override fun onAttachFragment(childFragment: Fragment?) {
        super.onAttachFragment(childFragment)
        urlBar.requestFocus()
    }

    private fun initUrlInputView() = with (urlInputView) {
        setOnCommitListener {
            onUrlEnteredListener.onTextInputUrlEntered(text.toString(), urlInputView.lastAutocompleteResult, UrlTextInputLocation.HOME)
        }
        setOnFilterListener { searchText, view -> urlAutoCompleteFilter.onFilter(searchText, view) }
    }

    companion object {
        const val FRAGMENT_TAG = "home"

        @JvmStatic
        fun create() = HomeFragment()
    }

    fun getFocusedTilePosition(): Int {
        return (activity.currentFocus.parent as? RecyclerView)?.getChildAdapterPosition(activity.currentFocus) ?: RecyclerView.NO_POSITION
    }

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_MENU &&
                event.action == KeyEvent.ACTION_UP &&
                getFocusedTilePosition() != RecyclerView.NO_POSITION) {
            activity.openContextMenu(tileContainer)
            return true
        }
        return false
    }
}
