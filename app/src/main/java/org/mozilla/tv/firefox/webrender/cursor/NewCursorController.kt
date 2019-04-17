/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.graphics.PointF
import android.view.KeyEvent
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.PublishSubject
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.*
import org.mozilla.tv.firefox.ext.isUriYouTubeTV
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.Direction

// TODO reorganize so tweakable ones are on top
private const val MAX_ACCELERATION = .7f
private const val MAX_VELOCITY = 40f
private const val MS_TO_MAX_ACCELERATION = 200
private const val ACCELERATION_PER_MS = MAX_ACCELERATION / MS_TO_MAX_ACCELERATION
private const val MS_PER_FRAME = 16

/**
 *  TODO
 *  [ ] Remove old Rx implementation cruft
 *  [ ] Pipe events up to repo (Maybe unnecessary?  cursorEventRepo is already dipping into key events.  Check to see if this implementation needs to be changed)
 *  [X] Handle visibility animation
 *  [ ] Touch simulation
 *  [X] Hook up directionKeyPress
 *  [X] Enable/disable invalidate calls
 *  [ ] View click animation
 *  [ ] Click actions
 *  [ ] Tweak values to make them feel good
 *  [ ] Fix {what were we writing here?}
 */

class NewCursorController(
        activeScreen: Observable<ScreenControllerStateMachine.ActiveScreen>,
        frameworkRepo: FrameworkRepo,
        sessionRepo: SessionRepo
) {
    var screenBounds: PointF? = null

    private val directionKeysPressed = mutableSetOf<Direction>()
    private var lastVelocity = 0f
    private var lastUpdatedAtMS = 0L // the first value when we start drawing will be an edge case

    val isCursorActive: Observable<Boolean> = Observables.combineLatest(
            activeScreen,
            frameworkRepo.isVoiceViewEnabled,
            sessionRepo.state
    ) { activeScreen, isVoiceViewEnabled, sessionState ->
        // We only display the cursor when the web content is active.
        val isWebRenderActive = activeScreen == WEB_RENDER
        val doesWebpageHaveOwnNavControls = sessionState.currentUrl.isUriYouTubeTV || isVoiceViewEnabled
        isWebRenderActive && !doesWebpageHaveOwnNavControls
    }.also {
        // No need to dispose: this survives for the duration of the app.
        it.subscribe { isCursorActive ->
            if (!isCursorActive) resetCursor()
        }
    }

    private val _isCursorMoving = PublishSubject.create<Boolean>()
    val isCursorMoving: Observable<Boolean> = _isCursorMoving.hide()

    fun directionKeyPress(event: KeyEvent): Boolean {
        if (!isCursorActive.blockingFirst()) { // todo: do I hang on startup?
            return false
        }

        val direction = when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> Direction.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> Direction.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> Direction.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> Direction.RIGHT
            else -> return false
        }
        when (event.action) {
            KeyEvent.ACTION_UP -> directionKeysPressed -= direction
            KeyEvent.ACTION_DOWN -> directionKeysPressed += direction
            else -> return false
        }

        _isCursorMoving.onNext(!directionKeysPressed.isEmpty())

        return true
    }

    /**
     * TODO describe this
     *
     * @return whether or not the view should continue to invalidate itself
     */
    fun mutatePosition(oldPos: PointF): Boolean {
        // Grab old and new times
        if (directionKeysPressed.isEmpty()) {
            resetCursor()
            return false
        } else {
            val currTime = System.currentTimeMillis()
            if (lastUpdatedAtMS == -1L) lastUpdatedAtMS = currTime - MS_PER_FRAME // TODO comment about why we do this
            lastVelocity = internalMutatePositionAndReturnVelocity(
                    oldPos,
                    lastUpdatedAtMS,
                    currTime,
                    lastVelocity,
                    directionKeysPressed
            )
            lastUpdatedAtMS = currTime
            return true
        }
    }

    private fun resetCursor() { // todo: name
        lastVelocity = 0f
        lastUpdatedAtMS = -1
        directionKeysPressed.clear()
    }

    private fun internalMutatePositionAndReturnVelocity(
            oldPos: PointF,
            oldTimeMS: Long,
            newTimeMS: Long,
            oldVelocity: Float,
            directionsPressed: Set<Direction>
    ): Float {
        // directionsPressed empty case is handled in `mutatePosition`
        require(directionsPressed.isNotEmpty())

        val timePassed = newTimeMS - oldTimeMS
        val accelerateBy = ACCELERATION_PER_MS * timePassed
        val velocity = (oldVelocity + accelerateBy).coerceIn(0f, MAX_VELOCITY)

        var verticalVelocity = 0f
        if (directionKeysPressed.contains(Direction.UP)) verticalVelocity -= velocity
        if (directionKeysPressed.contains(Direction.DOWN)) verticalVelocity += velocity
        var horizontalVelocity = 0f
        if (directionKeysPressed.contains(Direction.LEFT)) horizontalVelocity -= velocity
        if (directionKeysPressed.contains(Direction.RIGHT)) horizontalVelocity += velocity
        oldPos.y += verticalVelocity
        oldPos.x += horizontalVelocity
        oldPos.x = oldPos.x.coerceIn(0f, screenBounds?.x) // TODO Comment about screenbounds nullability
        oldPos.y = oldPos.y.coerceIn(0f, screenBounds?.y)
        return velocity
    }
}
