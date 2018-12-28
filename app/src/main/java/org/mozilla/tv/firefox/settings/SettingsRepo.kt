package org.mozilla.tv.firefox.settings

import android.app.Application
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.SharedPreferences
import android.os.StrictMode
import android.preference.PreferenceManager
import mozilla.components.support.ktx.android.os.resetAfter
import org.mozilla.tv.firefox.R

private const val PREF_KEY_TELEMETRY = R.string.pref_key_telemetry
const val IS_TELEMETRY_ENABLED_DEFAULT = true

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
        StrictMode.allowThreadDiskReads().resetAfter {
            val resources = applicationContext.resources
            _dataCollectionEnabled.value = _sharedPreferences
                    .getBoolean(resources.getString(PREF_KEY_TELEMETRY), IS_TELEMETRY_ENABLED_DEFAULT)
        }
    }

    fun setDataCollectionEnabled(toEnable: Boolean) {
        _sharedPreferences.edit()
                .putBoolean(_resources.getString(PREF_KEY_TELEMETRY), toEnable)
                .apply()
        _dataCollectionEnabled.value = toEnable
    }
}
