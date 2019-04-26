/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.webrender.cursor

import android.annotation.SuppressLint
import android.graphics.PointF
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.annotation.CheckResult
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.mozilla.tv.firefox.ScreenControllerStateMachine
import org.mozilla.tv.firefox.ScreenControllerStateMachine.ActiveScreen.WEB_RENDER
import org.mozilla.tv.firefox.ext.isUriYouTubeTV
import org.mozilla.tv.firefox.ext.toDirection
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.Direction

// Constants that we expect to be tweaked in order to adjust cursor behavior
private const val INITIAL_VELOCITY = 5f
private const val MAX_VELOCITY = 25f
private const val MS_TO_MAX_ACCELERATION = 200
private const val MAX_SCROLL_VELOCITY = 13

// Other constants
private const val MAX_ACCELERATION = MAX_VELOCITY - INITIAL_VELOCITY
private const val ACCELERATION_PER_MS = MAX_ACCELERATION / MS_TO_MAX_ACCELERATION
// 60 FPS = 16.6 repeating millis/frame.
// This is only used to draw one frame, so it's alright that it's only an approximation
private const val MS_PER_FRAME = 16
private const val EDGE_OF_SCREEN_MARGIN = 1
private const val LAST_UPDATE_AT_MS_UNSET = -1L

/**
 * @param [wasKeyEventConsumed] represents whether or not the passed [KeyEvent] was consumed
 * @param [simulatedTouch] a [MotionEvent]. If it is not null, this should be dispatched to an Activity
 */
data class HandleKeyEventResult(val wasKeyEventConsumed: Boolean, val simulatedTouch: MotionEvent?)

sealed class CursorEvent {
    /**
     * This represents the user moving the cursor to the edge of the screen,
     * attempting to scroll, and failing because the website has no more
     * content in that direction.
     */
    data class ScrolledToEdge(val edge: Direction) : CursorEvent() // TODO how often does this happen?  Telemetry would be good
    data class CursorMoved(val direction: Direction) : CursorEvent()
}

/**
 * Calculates cursor movements.
 *
 * Remote key events are pushed here from the rest of the app. Actual cursor position changes are
 * made when this class is called by [CursorView.onDraw]. This was done for performance reasons;
 * see the comment on [mutatePosition] for more details.
 */
class CursorModel(
    activeScreen: Observable<ScreenControllerStateMachine.ActiveScreen>,
    frameworkRepo: FrameworkRepo,
    sessionRepo: SessionRepo
) {
    // This is set early in the Fragment lifecycle. Most methods short if it is not available
    var screenBounds: PointF? = null
    var webViewCouldScrollInDirectionProvider: ((Direction) -> Boolean)? = null

    private val directionKeysPressed = mutableSetOf<Direction>()
    private var lastVelocity = 0f
    private var lastUpdatedAtMS = LAST_UPDATE_AT_MS_UNSET
    private var lastKnownCursorPos = PointF(0f, 0f)
    private var isInitialCursorPositionSet = false
    // scrollDistance is declared here as a micro-optimization to prevent alloc / dealloc
    // costs during our onDraw loop
    private val scrollDistance = PointF(0f, 0f)

    private val _cursorMovedEvents = PublishSubject.create<CursorEvent>()
    /**
     * These events are emitted VERY quickly. Be sure to throttle them!
     */
    val cursorMovedEvents: Observable<CursorEvent> = _cursorMovedEvents.hide()

    private val _scrollRequests = PublishSubject.create<PointF>()
    val scrollRequests: Observable<PointF> = _scrollRequests.hide()

    private val _isCursorMoving = BehaviorSubject.createDefault<Boolean>(false)
    private val isCursorMoving: Observable<Boolean> = _isCursorMoving.hide()

    private val _isSelectPressed = BehaviorSubject.createDefault<Boolean>(false)
    val isSelectPressed: Observable<Boolean> = _isSelectPressed.hide()
            .distinctUntilChanged()

    val isAnyCursorKeyPressed: Observable<Boolean> = Observables.combineLatest(isCursorMoving, isSelectPressed) {
        // Only emit false if we are both stationary and not pressed
        moving, pressed -> moving || pressed
    }
    .distinctUntilChanged()

    val isCursorEnabledForAppState: Observable<Boolean> = Observables.combineLatest(
            activeScreen,
            frameworkRepo.isVoiceViewEnabled,
            sessionRepo.state
    ) { activeScreen, isVoiceViewEnabled, sessionState ->
        // We only display the cursor when the web content is active.
        val isWebRenderActive = activeScreen == WEB_RENDER
        val doesWebpageHaveOwnNavControls = sessionState.currentUrl.isUriYouTubeTV || isVoiceViewEnabled
        isWebRenderActive && !doesWebpageHaveOwnNavControls
    }

    init {
        attachResetStateObserver()
    }

    /**
     * @returns a [HandleKeyEventResult]
     */
    fun handleKeyEvent(event: KeyEvent): HandleKeyEventResult {
        return when {
            Direction.KEY_CODES.contains(event.keyCode) ->
                HandleKeyEventResult(wasKeyEventConsumed = handleDirectionKeyEvent(event), simulatedTouch = null)
            // Center key is used on device, Enter key is used on emulator
            event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER -> {
                val motionEvent = maybeToMotionEvent(event)
                HandleKeyEventResult(wasKeyEventConsumed = motionEvent != null, simulatedTouch = motionEvent)
            }
            else -> HandleKeyEventResult(wasKeyEventConsumed = false, simulatedTouch = null)
        }
    }

    /**
     * @returns true if the event is consumed
     */
    private fun handleDirectionKeyEvent(event: KeyEvent): Boolean {
        if (!isCursorEnabledForAppState.blockingFirst()) {
            return false
        }
        require(Direction.KEY_CODES.contains(event.keyCode)) {
            "Invalid key event passed to CursorController#handleDirectionKeyEvent: $event"
        }

        val direction = event.toDirection() ?: return false
        when (event.action) {
            KeyEvent.ACTION_UP -> directionKeysPressed -= direction
            KeyEvent.ACTION_DOWN -> {
                directionKeysPressed += direction
                pushCursorEvent(direction)
            }
            else -> return false
        }

        _isCursorMoving.onNext(!directionKeysPressed.isEmpty())

        return true
    }

    private fun pushCursorEvent(direction: Direction) {
        val edgeNearCursor = getEdgeOfScreenNearCursor()
        val couldScroll = edgeNearCursor?.let { webViewCouldScrollInDirectionProvider?.invoke(it) }

        val cursorMovedToEdgeOfScreen = edgeNearCursor == direction
        val endOfDomContentReached = couldScroll == false

        val event = if (cursorMovedToEdgeOfScreen && endOfDomContentReached) {
            CursorEvent.ScrolledToEdge(direction)
        } else {
            CursorEvent.CursorMoved(direction)
        }

        _cursorMovedEvents.onNext(event)
    }

    /**
     * If the cursor is enabled, and the passed [KeyEvent] was causing by pressing the select
     * key, build a [MotionEvent] to be used by an Activity to simulate a touch event
     *
     * @return returns a MotionEvent if the event is consumed, null otherwise
     */
    @SuppressLint("Recycle")
    @CheckResult(suggest = "Recycle MotionEvent after use")
    private fun maybeToMotionEvent(event: KeyEvent): MotionEvent? {
        if (!isCursorEnabledForAppState.blockingFirst()) {
            return null
        }
        if (event.keyCode != KeyEvent.KEYCODE_DPAD_CENTER && event.keyCode != KeyEvent.KEYCODE_ENTER) {
            return null
        }

        fun buildMotionEvent() = MotionEvent.obtain(event.downTime, event.eventTime, event.action,
                lastKnownCursorPos.x, lastKnownCursorPos.y, 0)

        return when (event.action) {
            KeyEvent.ACTION_UP -> {
                _isSelectPressed.onNext(false)
                buildMotionEvent()
            }
            KeyEvent.ACTION_DOWN -> {
                _isSelectPressed.onNext(true)
                buildMotionEvent()
            }
            else -> null
        }
    }

    /**
     * Called by CursorView in order to drive cursor movement.
     *
     * Performance:
     * Previous code updated cursor VM position every 16MS (1_000 MS / 60 frames == 16.66). This
     * led to minor jitter because we were not tied exactly to the actual framerate. This new
     * implementation is harder to understand, but is noticeably more performant.
     *
     * Architecture:
     * Having a View#onDraw call VM updates does not mesh well with our architecture, but we found
     * no way to achieve desired performance with any other approach. Instead, we contained this
     * architectural aberration to this class, and as much as possible it should be left here.
     *
     * STRONGLY avoid accessing this state outside of this class, and think very carefully about
     * how you do if it is unavoidable.
     *
     * @return whether or not the view should continue to invalidate itself
     * Note that in addition to returning a value, this function also mutates [oldPosAndReturnedPos]
     */
    fun mutatePosition(oldPosAndReturnedPos: PointF): Boolean {
        lastKnownCursorPos = oldPosAndReturnedPos
        when {
            !isInitialCursorPositionSet -> {
                if (screenBounds != null) {
                    oldPosAndReturnedPos.x = screenBounds!!.x / 2
                    oldPosAndReturnedPos.y = screenBounds!!.y / 2
                    isInitialCursorPositionSet = true
                }
                return true
            }
            directionKeysPressed.isEmpty() -> {
                resetCursorState()
                return false
            }
            else -> {
                val currTime = System.currentTimeMillis()
                // If the cursor pos has not recently been updated, reset to current time.
                // Otherwise, the cursor would shoot across the page the first time it was
                // touched.
                if (lastUpdatedAtMS == LAST_UPDATE_AT_MS_UNSET) lastUpdatedAtMS = currTime - MS_PER_FRAME
                val timePassedSinceLastMutation = currTime - lastUpdatedAtMS

                lastVelocity = internalMutatePositionAndReturnVelocity(
                        oldPosAndReturnedPos,
                        timePassedSinceLastMutation,
                        lastVelocity,
                        directionKeysPressed
                )

                calculateAndSendScrollEvent(timePassedSinceLastMutation, lastVelocity, oldPosAndReturnedPos)
                lastUpdatedAtMS = currTime
                return true
            }
        }
    }

    private fun calculateAndSendScrollEvent(timePassed: Long, velocity: Float, oldPos: PointF) {
        // This should not live inside internalMutatePositionAndReturnVelocity.
        // Move it if you can find a better solution
        val approxFramesPassed = (timePassed / 16).toInt()
        getScrollDistance(velocity, oldPos, approxFramesPassed)
        if (scrollDistance.x != 0f || scrollDistance.y != 0f) {
            _scrollRequests.onNext(scrollDistance)
        }
    }

    /**
     * Mutates [oldPos] to its new position. This position is calculated based on the time passed
     * since the last update, in order to avoid miscalculations when dropping frames.
     *
     * Calculates necessary scrolling, if any, and sends out a request to handle it.
     *
     * @returns the new cursor velocity
     */
    private fun internalMutatePositionAndReturnVelocity(
        oldPos: PointF,
        timePassedSinceLastMutation: Long,
        oldVelocity: Float,
        directionsPressed: Set<Direction>
    ): Float {
        // directionsPressed empty case is handled in `mutatePosition`
        require(directionsPressed.isNotEmpty())
        val screenBounds = screenBounds ?: return 0f

        val accelerateBy = ACCELERATION_PER_MS * timePassedSinceLastMutation
        val velocity = (oldVelocity + accelerateBy).coerceIn(0f, MAX_VELOCITY)

        fun updatePosition() {
            var verticalVelocity = 0f
            if (directionKeysPressed.contains(Direction.UP)) verticalVelocity -= velocity
            if (directionKeysPressed.contains(Direction.DOWN)) verticalVelocity += velocity
            var horizontalVelocity = 0f
            if (directionKeysPressed.contains(Direction.LEFT)) horizontalVelocity -= velocity
            if (directionKeysPressed.contains(Direction.RIGHT)) horizontalVelocity += velocity
            oldPos.x += horizontalVelocity
            oldPos.y += verticalVelocity
            oldPos.x = oldPos.x.coerceIn(0f, screenBounds.x)
            oldPos.y = oldPos.y.coerceIn(0f, screenBounds.y)
        }

        updatePosition()

        return velocity
    }

    /**
     * If the cursor is near the edge of the screen, this will return that
     * direction.  If not, it will return null.
     *
     * When in a corner, only UP or DOWN will be returned.
     */
    fun getEdgeOfScreenNearCursor(): Direction? {
        val screenBounds = screenBounds ?: return null
        return when {
            lastKnownCursorPos.y < EDGE_OF_SCREEN_MARGIN -> Direction.UP
            lastKnownCursorPos.y > screenBounds.y - EDGE_OF_SCREEN_MARGIN -> Direction.DOWN
            lastKnownCursorPos.x < EDGE_OF_SCREEN_MARGIN -> Direction.LEFT
            lastKnownCursorPos.x > screenBounds.x - EDGE_OF_SCREEN_MARGIN -> Direction.RIGHT
            else -> null
        }
    }

    private fun resetCursorState() {
        lastVelocity = INITIAL_VELOCITY
        lastUpdatedAtMS = LAST_UPDATE_AT_MS_UNSET
        directionKeysPressed.clear()
    }

    // This is taken from older code.  Crufty, but it works
    private fun getScrollDistance(vel: Float, pos: PointF, framesPassed: Int) {
        val scrollVelReturnVal = PointF(0f, 0f)
        val screenBounds = screenBounds
        if (screenBounds == null) {
            scrollDistance.x = 0f
            scrollDistance.y = 0f
            return
        }

        if (vel > 0f) {
            val percentMaxVel = vel / MAX_VELOCITY
            if (pos.x == 0f && directionKeysPressed.contains(Direction.LEFT)) {
                scrollVelReturnVal.x = -percentMaxVel
            } else if (pos.x == screenBounds.x && directionKeysPressed.contains(Direction.RIGHT)) {
                scrollVelReturnVal.x = percentMaxVel
            }

            if (pos.y == 0f && directionKeysPressed.contains(Direction.UP)) {
                scrollVelReturnVal.y = -percentMaxVel
            } else if (pos.y == screenBounds.y && directionKeysPressed.contains(Direction.DOWN)) {
                scrollVelReturnVal.y = percentMaxVel
            }
        }

        scrollDistance.x = (scrollVelReturnVal.x * MAX_SCROLL_VELOCITY * framesPassed)
        scrollDistance.y = (scrollVelReturnVal.y * MAX_SCROLL_VELOCITY * framesPassed)
    }

    @SuppressLint("CheckResult") // Does not need to be disposed as this survives for the duration of the app
    private fun attachResetStateObserver() {
        isCursorEnabledForAppState.subscribe { isCursorActive ->
            if (!isCursorActive) resetCursorState()
        }
    }
}
