package org.mozilla.tv.firefox.navigationoverlay

import androidx.lifecycle.MutableLiveData
import mozilla.components.support.test.eq
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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
        sessionRepo = mock(SessionRepo::class.java)
        sessionState = MutableLiveData()
        @Suppress("DEPRECATION")
        `when`(sessionRepo.legacyState).thenReturn(sessionState)
        pinnedTileRepo = mock(PinnedTileRepo::class.java)
        pinnedTiles = MutableLiveData()
        @Suppress("DEPRECATION")
        `when`(pinnedTileRepo.legacyPinnedTiles).thenReturn(pinnedTiles)
        telemetryIntegration = mock(TelemetryIntegration::class.java)
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
                currentUrl = "www.google.com",
                loading = false
            )
            sessionState.value = SessionRepo.State(
                backEnabled = false,
                forwardEnabled = true,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "firefox:home",
                loading = false
            )
            sessionState.value = SessionRepo.State(
                backEnabled = false,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "https://www.wikipedia.org",
                loading = false
            )
            sessionState.value = SessionRepo.State(
                backEnabled = false,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "www.google.com",
                loading = false
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
                currentUrl = "www.google.com",
                loading = false
            )
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = true,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "firefox:home",
                loading = false
            )
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = "https://www.wikipedia.org",
                loading = false
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
                currentUrl = google,
                loading = false
            )
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = true,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = facebook,
                loading = false
            )
            sessionState.value = SessionRepo.State(
                backEnabled = true,
                forwardEnabled = false,
                turboModeActive = true,
                desktopModeActive = false,
                currentUrl = wikipedia,
                loading = false
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
                currentUrl = mozilla,
                loading = false
            )
        }
    }

    @Test
    fun `WHEN back in toolbar is clicked THEN associated telemetry method is called`() {
        setToolbarVmState()
        toolbarVm.backButtonClicked()

        verify(telemetryIntegration, times(1)).overlayClickEvent(eq(NavigationEvent.BACK), anyBoolean(), anyBoolean(), anyBoolean())
    }

    @Test
    fun `WHEN forward in toolbar is clicked THEN associated telemetry method is called`() {
        setToolbarVmState()
        toolbarVm.forwardButtonClicked()

        verify(telemetryIntegration, times(1)).overlayClickEvent(
            eq(NavigationEvent.FORWARD), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    @Test
    fun `WHEN reload in toolbar is clicked THEN associated telemetry method is called`() {
        setToolbarVmState()
        toolbarVm.reloadButtonClicked()

        verify(telemetryIntegration, times(1)).overlayClickEvent(
            eq(NavigationEvent.RELOAD), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    @Test
    fun `WHEN pin button in toolbar is clicked THEN associated telemetry method is called`() {
        setToolbarVmState()
        toolbarVm.pinButtonClicked()

        verify(telemetryIntegration, times(1)).overlayClickEvent(
            eq(NavigationEvent.PIN_ACTION), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    @Test
    fun `WHEN turbo mode in toolbar is clicked THEN associated telemetry method is called`() {
        setToolbarVmState()
        toolbarVm.turboButtonClicked()

        verify(telemetryIntegration, times(1)).overlayClickEvent(
            eq(NavigationEvent.TURBO), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    @Test
    fun `WHEN desktop mode in toolbar is clicked THEN associated telemetry method is called`() {
        setToolbarVmState()
        toolbarVm.desktopModeButtonClicked()

        verify(telemetryIntegration, times(1)).overlayClickEvent(
            eq(NavigationEvent.DESKTOP_MODE), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    @Test
    fun `WHEN exit in toolbar is clicked THEN associated telemetry method is called`() {
        setToolbarVmState()
        toolbarVm.exitFirefoxButtonClicked()

        verify(telemetryIntegration, times(1)).overlayClickEvent(
            eq(NavigationEvent.EXIT_FIREFOX), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    /**
     * This method will set the state of the pinnedTiles and sessionState LiveData.
     * This is needed because overlayClickEvent will not be called if state is null.
     * The toolbarVm state needs an observer before it will update, because it is a MediatorLiveData.
     */
    private fun setToolbarVmState() {
        toolbarVm.state.observeForever { }
        pinnedTiles.value = linkedMapOf()
        sessionState.value = SessionRepo.State(
            backEnabled = false,
            forwardEnabled = false,
            turboModeActive = true,
            desktopModeActive = false,
            currentUrl = "www.google.com",
            loading = false
        )
    }
}
