package org.mozilla.tv.firefox.settings

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.Context
import mozilla.components.support.base.observer.Consumable
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.webrender.WebViewCache

class SettingsViewModel(private val settingsRepo: SettingsRepo, private val sessionRepo: SessionRepo) : ViewModel() {
    private var _events = MutableLiveData<Consumable<SettingsFragment.Action>>()

    val events: LiveData<Consumable<SettingsFragment.Action>> = _events
    val dataCollectionEnabled = settingsRepo.dataCollectionEnabled

    fun setDataCollectionEnabled(toEnable: Boolean) {
        settingsRepo.setDataCollectionEnabled(toEnable)
    }

    fun clearBrowsingData(context: Context, webViewCache: WebViewCache) {
        sessionRepo.clearBrowsingData(context, webViewCache)
        _events.value = Consumable.from(SettingsFragment.Action.SESSION_CLEARED)
    }
}
