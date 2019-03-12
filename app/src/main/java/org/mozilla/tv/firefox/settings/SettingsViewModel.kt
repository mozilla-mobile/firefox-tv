package org.mozilla.tv.firefox.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mozilla.components.support.base.observer.Consumable
import org.mozilla.tv.firefox.framework.FrameworkRepo
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.webrender.EngineViewCache

class SettingsViewModel(
    frameworkRepo: FrameworkRepo,
    private val settingsRepo: SettingsRepo,
    private val sessionRepo: SessionRepo
) : ViewModel() {
    private var _events = MutableLiveData<Consumable<SettingsFragment.Action>>()

    val isVoiceViewEnabled = frameworkRepo.isVoiceViewEnabled

    val events: LiveData<Consumable<SettingsFragment.Action>> = _events
    val dataCollectionEnabled = settingsRepo.dataCollectionEnabled

    fun setDataCollectionEnabled(toEnable: Boolean) {
        settingsRepo.setDataCollectionEnabled(toEnable)
    }

    fun clearBrowsingData(engineViewCache: EngineViewCache) {
        sessionRepo.clearBrowsingData(engineViewCache)
        _events.value = Consumable.from(SettingsFragment.Action.SESSION_CLEARED)
    }
}
