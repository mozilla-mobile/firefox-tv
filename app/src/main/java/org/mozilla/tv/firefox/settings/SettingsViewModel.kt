package org.mozilla.tv.firefox.settings

import android.arch.lifecycle.ViewModel

class SettingsViewModel(private val settingsRepo: SettingsRepo) : ViewModel() {
    val dataCollectionEnabled = settingsRepo.dataCollectionEnabled

    fun setDataCollectionEnabled(toEnable: Boolean) {
        settingsRepo.setDataCollectionEnabled(toEnable)
    }
}
