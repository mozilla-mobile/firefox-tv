/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.sentry.Sentry
import kotlinx.android.synthetic.main.settings_screen_buttons.view.cancel_action
import kotlinx.android.synthetic.main.settings_screen_buttons.view.confirm_action
import kotlinx.android.synthetic.main.settings_screen_fxa_profile.view.avatar_image
import kotlinx.android.synthetic.main.settings_screen_fxa_profile.view.button_firefox_tabs
import kotlinx.android.synthetic.main.settings_screen_fxa_profile.view.button_sign_out
import kotlinx.android.synthetic.main.settings_screen_fxa_profile.view.description_bottom
import kotlinx.android.synthetic.main.settings_screen_fxa_profile.view.description_top
import kotlinx.android.synthetic.main.settings_screen_switch.toggle
import kotlinx.android.synthetic.main.settings_screen_switch.view.back_button
import kotlinx.android.synthetic.main.settings_screen_switch.view.description
import kotlinx.android.synthetic.main.settings_screen_switch.view.toggle
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.architecture.FirefoxViewModelProviders
import org.mozilla.tv.firefox.channels.SettingsScreen
import org.mozilla.tv.firefox.channels.SettingsTile
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.fxa.FxaRepo
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration

const val KEY_SETTINGS_TYPE = "KEY_SETTINGS_TYPE"

/** The settings for the app. */
class SettingsFragment : Fragment() {
    enum class Action {
        SESSION_CLEARED
    }

    var profileDisposable: Disposable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val settingsVM = FirefoxViewModelProviders.of(this@SettingsFragment).get(SettingsViewModel::class.java)
        val type: SettingsTile = SettingsScreen.valueOf(arguments!!.getString(KEY_SETTINGS_TYPE)!!)
        val view = when (type) {
            SettingsScreen.DATA_COLLECTION -> setupDataCollectionScreen(inflater, container, settingsVM)
            SettingsScreen.CLEAR_COOKIES -> setupClearCookiesScreen(inflater, container, settingsVM)
            SettingsScreen.FXA_PROFILE -> setupFxaProfileScreen(inflater, container)
            else -> {
                Sentry.capture(IllegalStateException("Unexpected Settings type received: $type"))
                return container!!
            }
        }
        view.findViewById<ImageButton>(R.id.back_button).setOnClickListener {
            serviceLocator!!.screenController.handleBack(fragmentManager!!)
        }

        return view
    }

    private fun setupDataCollectionScreen(
        inflater: LayoutInflater,
        parentView: ViewGroup?,
        settingsViewModel: SettingsViewModel
    ): View {
        val view = inflater.inflate(R.layout.settings_screen_switch, parentView, false)
        settingsViewModel.dataCollectionEnabled.observe(viewLifecycleOwner, Observer<Boolean> { state ->
            view.toggle.isChecked = state ?: return@Observer
        })
        view.toggle.setOnClickListener {
            settingsViewModel.setDataCollectionEnabled(toggle.isChecked)
        }
        view.description.text = resources.getString(R.string.settings_telemetry_description,
                resources.getString(R.string.firefox_tv_brand_name))
        return view
    }

    private fun setupClearCookiesScreen(
        inflater: LayoutInflater,
        parentView: ViewGroup?,
        settingsViewModel: SettingsViewModel
    ): View {
        settingsViewModel.events.observe(viewLifecycleOwner, Observer {
            it?.consume { event ->
                when (event) {
                    Action.SESSION_CLEARED -> {
                        activity?.recreate()
                    }
                }
                true
            }
        })

        val view = inflater.inflate(R.layout.settings_screen_buttons, parentView, false)
        view.confirm_action.setOnClickListener {
            settingsViewModel.clearBrowsingData(serviceLocator!!.engineViewCache)
            serviceLocator!!.screenController.handleBack(fragmentManager!!)
        }
        view.cancel_action.setOnClickListener {
            serviceLocator!!.screenController.handleBack(fragmentManager!!)
        }
        return view
    }

    private fun setupFxaProfileScreen(
        inflater: LayoutInflater,
        parentView: ViewGroup?
    ): View {
        val view = inflater.inflate(R.layout.settings_screen_fxa_profile, parentView, false)

        setupFxaText(view)
        setupFxaProfileListeners(view)
        profileDisposable = observeFxaProfile(view)

        return view
    }

    private fun setupFxaText(view: View) {
        val appName = resources.getString(R.string.app_name)
        view.button_firefox_tabs.text = resources.getString(R.string.fxa_settings_primary_button, appName)
        // Username is positioned and styled differently, so it is left blank here
        // and set on another TextView
        view.description_top.text = resources.getString(R.string.fxa_settings_body, "")
    }

    private fun setupFxaProfileListeners(view: View) {
        val serviceLocator = serviceLocator!!
        val screenController = serviceLocator.screenController
        val fxaRepo = serviceLocator.fxaRepo
        val telemetryIntegration = TelemetryIntegration.INSTANCE

        view.button_firefox_tabs.setOnClickListener {
            // TODO show send tab tutorial
            telemetryIntegration.fxaProfileGetTabsButtonClickEvent()
        }
        view.button_sign_out.setOnClickListener {
            fxaRepo.logout()
            screenController.handleBack(fragmentManager!!)
            telemetryIntegration.fxaProfileSignOutButtonClickEvent()
        }
        view.back_button.setOnClickListener {
            screenController.handleBack(fragmentManager!!)
            telemetryIntegration.fxaProfileGoBackButtonClickEvent()
        }
    }

    private fun observeFxaProfile(view: View): Disposable = context!!.serviceLocator.fxaRepo.accountState
        .ofType(FxaRepo.AccountState.AuthenticatedWithProfile::class.java)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
            view.description_bottom.text = it.profile.displayName
            it.profile.avatar.invoke(view.avatar_image)
        }

    override fun onDestroyView() {
        super.onDestroyView()
        profileDisposable?.dispose()
    }

    companion object {
        const val FRAGMENT_TAG = "settings"

        fun newInstance(type: SettingsScreen): SettingsFragment {
            return SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_SETTINGS_TYPE, type.toString())
                }
            }
        }
    }
}
