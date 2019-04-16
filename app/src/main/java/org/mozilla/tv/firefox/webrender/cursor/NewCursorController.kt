/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.annotation.SuppressLint
import android.graphics.PointF
import android.view.KeyEvent
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

private const val TRANSLATE_BY = .3f
private const val MAX_ACCELERATION = .7f

class NewCursorController(private val activeScreen: Observable<ScreenControllerStateMachine.ActiveScreen>) {

    var shouldDisplay = false
        private set

    var screenBounds: PointF? = null
    var lastDirectionKeyPressed: KeyEvent by Delegates.observable(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP)) { _, _, newValue ->
        handleAcceleration(newValue)
        handleVisibility(newValue)
    }

    private var translateX = 0f
    private var translateY = 0f
    private var currentAcceleration = 0f
    private var accelerationDisposable: Disposable? = null
    private var visibilityDisposable: Disposable? = null

    @SuppressLint("CheckResult") // This should live for the duration of the app
    fun setup() {
        activeScreen
                .switchMap {
                    if (it == ScreenControllerStateMachine.ActiveScreen.WEB_RENDER) {
                        Observable.interval(1, TimeUnit.MILLISECONDS)
                                .observeOn(Schedulers.computation())
                    } else {
                        Observable.empty()
                    }
                }
                .observeOn(Schedulers.computation())
                .subscribe { translate() }
    }

    fun mutatePosition(oldPos: PointF) {
        val screenBounds = screenBounds ?: return

        oldPos.x += translateX
        oldPos.y += translateY
        oldPos.x = oldPos.x.coerceIn(0f, screenBounds.x)
        oldPos.y = oldPos.y.coerceIn(0f, screenBounds.y)
        translateX = 0f
        translateY = 0f
    }

    private fun translate() {
        if (lastDirectionKeyPressed.action == KeyEvent.ACTION_UP) return

        val translation = TRANSLATE_BY + currentAcceleration

        when (lastDirectionKeyPressed.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> translateY -= translation
            KeyEvent.KEYCODE_DPAD_DOWN -> translateY += translation
            KeyEvent.KEYCODE_DPAD_LEFT -> translateX -= translation
            KeyEvent.KEYCODE_DPAD_RIGHT -> translateX += translation
        }
    }

    private fun handleAcceleration(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_UP) {
            accelerationDisposable?.dispose()
            accelerationDisposable = null
            currentAcceleration = 0f
        } else if (event.action == KeyEvent.ACTION_DOWN && accelerationDisposable == null) {
            accelerationDisposable = Observable.interval(10, TimeUnit.MILLISECONDS)
                    .take(20)
                    .subscribe { currentAcceleration += MAX_ACCELERATION / 20 }
        }
    }

    private fun handleVisibility(event: KeyEvent) {
        if (event.action == KeyEvent.ACTION_UP) {
            visibilityDisposable = Observable.timer(3, TimeUnit.SECONDS)
                    .subscribe { shouldDisplay = false }
        } else if (event.action == KeyEvent.ACTION_DOWN) {
            visibilityDisposable?.dispose()
            shouldDisplay = true
        }
    }
}
