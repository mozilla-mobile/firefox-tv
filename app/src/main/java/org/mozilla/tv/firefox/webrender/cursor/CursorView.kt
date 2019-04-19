/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import android.util.AttributeSet
import androidx.annotation.CheckResult
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import org.mozilla.tv.firefox.R
import java.util.concurrent.TimeUnit

private const val BITMAP_PRESSED = R.drawable.cursor_full_active
private const val BITMAP_UNPRESSED = R.drawable.cursor_full
private const val HIDE_ANIMATION_DURATION_MILLIS = 250L
private val HIDE_AFTER_MILLIS = TimeUnit.SECONDS.toMillis(3)

/**
 * Renders the cursor visible on top of the browser.
 *
 * Most behavior is controlled by [CursorModel]
 *
 * Any images set must be Bitmaps, not Drawables
 */
class CursorView(context: Context, attrs: AttributeSet) : AppCompatImageView(context, attrs) {

    private val onDrawMutablePositionCache = PointF(x, y)

    private var cursorModel: CursorModel? = null

    init {
        setImageResource(BITMAP_UNPRESSED)
    }

    @CheckResult(suggest = "Dispose me, please. 🥰")
    fun setup(cursorModel: CursorModel): Disposable {
        this.cursorModel = cursorModel

        val compositeDisposable = CompositeDisposable()

        cursorModel.isCursorActive.subscribe { isCursorActive ->
            isVisible = isCursorActive
            if (!isCursorActive) {
                animate().cancel()
            }
        }.addTo(compositeDisposable)

        Observables.combineLatest(cursorModel.isCursorMoving, cursorModel.isSelectPressed) {
            // Only emit false if we are both stationary and not pressed
            moving, pressed -> moving || pressed
        }
                .distinctUntilChanged()
                .subscribe { cursorIsMovingOrPressed ->
                    if (cursorIsMovingOrPressed) {
                        animate().cancel()
                        alpha = 1f
                        invalidate()
                    } else {
                        animate()
                                .setStartDelay(HIDE_AFTER_MILLIS)
                                .setDuration(HIDE_ANIMATION_DURATION_MILLIS)
                                .alpha(0f)
                                .start()
                    }
                }.addTo(compositeDisposable)

        cursorModel.isSelectPressed
                .distinctUntilChanged()
                .subscribe { pressed -> when (pressed) {
                        true -> setImageResource(BITMAP_PRESSED)
                        false -> setImageResource(BITMAP_UNPRESSED)
                    } }.addTo(compositeDisposable)

        return compositeDisposable
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        onDrawMutablePositionCache.set(x, y)

        val shouldInvalidate = cursorModel?.mutatePosition(onDrawMutablePositionCache) ?: false
        x = onDrawMutablePositionCache.x - (width / 2)
        y = onDrawMutablePositionCache.y - (height / 2)

        if (shouldInvalidate) invalidate()
    }
}
