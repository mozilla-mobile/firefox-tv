package org.mozilla.tv.firefox.settings

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences
import android.os.StrictMode
import android.preference.PreferenceManager
import org.mozilla.tv.firefox.R

private const val PREF_KEY_TELEMETRY = R.string.pref_key_telemetry

class SettingsRepo(private val applicationContext: Application) {
    private val _sharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
    private val _resources = applicationContext.resources

    private val _dataCollectionEnabled = MutableLiveData<Boolean>()
    val dataCollectionEnabled: LiveData<Boolean> = _dataCollectionEnabled

    init {
        loadSettingsFromPreferences()
    }

    private fun loadSettingsFromPreferences() {
        // The first access to shared preferences will require a disk read.
        val threadPolicy = StrictMode.allowThreadDiskReads()
        try {
            val resources = applicationContext.resources
            _dataCollectionEnabled.value = _sharedPreferences.getBoolean(resources.getString(PREF_KEY_TELEMETRY), true)
        } finally {
            StrictMode.setThreadPolicy(threadPolicy)
        }
    }

    fun setDataCollectionEnabled(toEnable: Boolean) {
        _sharedPreferences.edit()
                .putBoolean(_resources.getString(PREF_KEY_TELEMETRY), toEnable)
                .apply()
        _dataCollectionEnabled.value = toEnable
    }
}
