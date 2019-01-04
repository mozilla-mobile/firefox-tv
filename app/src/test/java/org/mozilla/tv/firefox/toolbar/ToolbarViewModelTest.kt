package org.mozilla.tv.firefox.toolbar

import android.arch.lifecycle.MutableLiveData
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mozilla.tv.firefox.ext.map
import org.mozilla.tv.firefox.helpers.ext.assertValues
import org.mozilla.tv.firefox.pinnedtile.PinnedTile
import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import org.mozilla.tv.firefox.utils.PreventLiveDataMainLooperCrashRule
import org.robolectric.RobolectricTestRunner

private const val mozilla = "https://www.mozilla.org/en-US/"
private const val google = "www.google.com/"
private const val facebook = "www.facebook.com/"
private const val wikipedia = "https://www.wikipedia.org/"

@RunWith(RobolectricTestRunner::class) // Requires Robolectric for Uri
class ToolbarViewModelTest {

    @get:Rule val rule = PreventLiveDataMainLooperCrashRule()

    private lateinit var toolbarVm: ToolbarViewModel
    private lateinit var sessionRepo: SessionRepo
    private lateinit var pinnedTileRepo: PinnedTileRepo
    private lateinit var telemetryIntegration: TelemetryIntegration

    private lateinit var sessionState: MutableLiveData<SessionRepo.State>
    private lateinit var pinnedTiles: MutableLiveData<LinkedHashMap<String, PinnedTile>>

    @Before
    fun setup() {
        sessionRepo = spy(mock(SessionRepo::class.java))
        sessionState = MutableLiveData()
        `when`(sessionRepo.state).thenReturn(sessionState)
        pinnedTileRepo = spy(mock(PinnedTileRepo::class.java))
        pinnedTiles = MutableLiveData()
        `when`(pinnedTileRepo.getPinnedTiles()).thenReturn(pinnedTiles)
        telemetryIntegration = spy(mock(TelemetryIntegration::class.java))
        toolbarVm = ToolbarViewModel(sessionRepo, pinnedTileRepo, telemetryIntegration)
    }

    @Test
    fun `WHEN session back enabled is false THEN vm back enabled is false`() {
        toolbarVm.state.map { it.backEnabled }.assertValues(false, false, false, false) {
            pinnedTiles.value = linkedMapOf()
            sessionState.value = SessionRepo.State(
                backEnabled = false,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "www.google.com"
            )
            sessionState.value = SessionRepo.State(
                backEnabled = false,
                forwardEnabled = true,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "firefox:home"
            )
            sessionState.value = SessionRepo.State(
                backEnabled = false,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "https://www.wikipedia.org"
            )
            sessionState.value = SessionRepo.State(
                backEnabled = false,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "www.google.com"
            )
        }
    }

    @Test
    fun `GIVEN session back enabled is true WHEN back forward index is 2 or greater THEN vm back enabled should be true`() {
        toolbarVm.state.map { it.backEnabled }.assertValues(true, true, true) {
            pinnedTiles.value = linkedMapOf()
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "www.google.com"
            )
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = true,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "firefox:home"
            )
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "https://www.wikipedia.org"
            )
        }
    }

    @Test
    fun `WHEN current url is pinned THEN pinChecked should be true`() {
        toolbarVm.state.map { it.pinChecked }.assertValues(true, true, true) {
            val tile = mock(PinnedTile::class.java)
            pinnedTiles.value = linkedMapOf(google to tile, facebook to tile, wikipedia to tile)
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = google
            )
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = true,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = facebook
            )
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = wikipedia
            )
        }
    }

    @Test
    fun `WHEN new session state url is not home THEN no overlay visibility event should be emitted`() {
        @Suppress("RemoveEmptyParenthesesFromLambdaCall")
        toolbarVm.events.assertValues(/* No values */) {
            toolbarVm.state.observeForever { /* start subscription */ }
            pinnedTiles.value = linkedMapOf()
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = mozilla
            )
        }
    }
}
