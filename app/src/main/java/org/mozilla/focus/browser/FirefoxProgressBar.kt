/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.browser

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.arch.lifecycle.Observer
import android.graphics.drawable.AnimationDrawable
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.View
import android.view.View.GONE
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.firefox_progress_bar.view.*
import org.mozilla.focus.R
import org.mozilla.focus.architecture.NonNullObserver
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

private const val HIDE_MESSAGE_ID = 0
private const val HIDE_ANIMATION_DURATION_MILLIS = 250L
private val HIDE_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(3)

class FirefoxProgressBar @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0)
    : LinearLayout(context, attrs, defStyle) {

    private val hideHandler = FirefoxProgressBarHideHandler(this)

    fun initialize(browserFrag: BrowserFragment) {
        browserFrag.session.url.observe(browserFrag, Observer { sessionUrl -> url.text = sessionUrl })
        browserFrag.session.loading.observe(browserFrag, object : NonNullObserver<Boolean>() {
            public override fun onValueChanged(loading: Boolean) {
                if (loading) {
                    showBar()
                } else {
                    scheduleHideBar()
                }
            }
        })
    }

    init {
        LayoutInflater.from(context)
                .inflate(R.layout.firefox_progress_bar, this, true)
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
    }

    private fun showBar() {
        hideHandler.removeMessages(HIDE_MESSAGE_ID)
        visibility = View.VISIBLE
        (progressAnimation.background as AnimationDrawable).start()
        animate().cancel()
        alpha = 1f
    }

    private fun scheduleHideBar() {
        hideHandler.removeMessages(HIDE_MESSAGE_ID)
        hideHandler.sendEmptyMessageDelayed(HIDE_MESSAGE_ID, HIDE_AFTER_MILLIS)
    }
}

/**
 * Hides the cursor when it receives a message.
 *
 * We use a [Handler], with [Message]s, because they make no allocations, unlike
 * more modern/readable approaches:
 * - coroutines
 * - Animators with start delays (and cancelling them as necessary)
 */
private class FirefoxProgressBarHideHandler(view: FirefoxProgressBar) : Handler(Looper.getMainLooper()) {
    private val viewWeakReference = WeakReference<FirefoxProgressBar>(view)

    override fun handleMessage(msg: Message?) {
        val progressBar = viewWeakReference.get()
        progressBar
                ?.animate()
                ?.withEndAction {
                    progressBar.visibility = GONE
                    (progressBar.progressAnimation.background as AnimationDrawable).stop()
                }
                ?.setDuration(HIDE_ANIMATION_DURATION_MILLIS)
                ?.alpha(0f)
                ?.start()
    }
}