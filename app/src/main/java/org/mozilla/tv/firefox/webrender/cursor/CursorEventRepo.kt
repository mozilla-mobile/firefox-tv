/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.view.KeyEvent
import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.PublishSubject
import io.reactivex.subjects.Subject
import org.mozilla.tv.firefox.ScreenController
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.utils.Direction
import java.util.concurrent.TimeUnit

private val DIRECTION_KEYS = listOf(
        KEYCODE_DPAD_UP,
        KEYCODE_DPAD_DOWN,
        KEYCODE_DPAD_LEFT,
        KEYCODE_DPAD_RIGHT
)

/**
 * This class exposes low level cursor movement events.
 *
 * This is usually not the class you want to use. These are unprocessed, and for
 * most use cases you will want to use a class at a higher level of abstraction.
 */
class CursorEventRepo(
        private val cursorModel: CursorModel,
        screenController: ScreenController
) {

    sealed class CursorEvent {
        /**
         * This represents the user moving the cursor to the edge of the screen,
         * attempting to scroll, and failing because the website has no more
         * content in that direction.
         */
        data class ScrolledToEdge(val edge: Direction) : CursorEvent() // TODO how often does this happen?  Telemetry would be good
        data class CursorMoved(val direction: Direction) : CursorEvent()
    }

    // Note that these events are emitted very quickly. Consumers should usually throttle them
    private val keyEvents: Subject<KeyEvent> = PublishSubject.create()

    private var webViewCouldScrollInDirection: (Direction) -> Boolean = { false }

    fun setWebViewCouldScrollInDirection(couldScroll: (Direction) -> Boolean) {
        webViewCouldScrollInDirection = couldScroll
    }

    fun pushKeyEvent(keyEvent: KeyEvent) {
        keyEvents.onNext(keyEvent)
    }

    val webRenderDirectionEvents: Observable<CursorEvent> by lazy {
        fun <T> Observable<T>.throttleEvents(): Observable<T> =
                // Prior to throttling, this caused major performance problems
                this.throttleFirst(10, TimeUnit.MILLISECONDS)

        fun <T> Observable<Pair<T, ScreenControllerStateMachine.ActiveScreen>>.onlyEmitIfWebRenderIsActive() =
                this.filter { (_, activeScreen) -> activeScreen == ScreenControllerStateMachine.ActiveScreen.WEB_RENDER }

        fun <T> Observable<Pair<T, ScreenControllerStateMachine.ActiveScreen>>.dropActiveScreen() =
                this.map { (value, _) -> value }

        fun Observable<KeyEvent>.filterIsDirectionKey() =
                this.filter { DIRECTION_KEYS.contains(it.keyCode) }

        fun Observable<KeyEvent>.filterIsKeyDown() =
                // i.e., key is pressed, not released
                this.filter { it.action == ACTION_DOWN }

        fun Observable<KeyEvent>.mapEventToDirection() =
                this.flatMap {
                    when (it.keyCode) {
                        KEYCODE_DPAD_UP -> Observable.just(Direction.UP)
                        KEYCODE_DPAD_DOWN -> Observable.just(Direction.DOWN)
                        KEYCODE_DPAD_LEFT -> Observable.just(Direction.LEFT)
                        KEYCODE_DPAD_RIGHT -> Observable.just(Direction.RIGHT)
                        else -> Observable.empty()
                    }
                }

        fun Observable<Direction>.mapToCursorEvent() =
                this.map { cursorDirection ->
                    val edgeNearCursor = cursorModel.getEdgeOfScreenNearCursor()
                    val couldScroll = edgeNearCursor?.let { webViewCouldScrollInDirection(it) }

                    val cursorMovedToEdgeOfScreen = edgeNearCursor == cursorDirection
                    val endOfDomContentReached = couldScroll == false

                    return@map if (cursorMovedToEdgeOfScreen && endOfDomContentReached) {
                        CursorEvent.ScrolledToEdge(cursorDirection)
                    } else {
                        CursorEvent.CursorMoved(cursorDirection)
                    }
                }

        Observables.combineLatest(keyEvents.throttleEvents(), screenController.currentActiveScreen)
                .onlyEmitIfWebRenderIsActive()
                .dropActiveScreen()
                .filterIsDirectionKey()
                .filterIsKeyDown()
                .mapEventToDirection()
                .mapToCursorEvent()
    }
}
