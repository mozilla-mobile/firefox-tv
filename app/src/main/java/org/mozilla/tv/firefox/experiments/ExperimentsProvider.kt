/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.experiments

import android.content.Context
import io.sentry.Sentry
import mozilla.components.service.fretboard.ExperimentDescriptor
import mozilla.components.service.fretboard.Fretboard
import org.mozilla.tv.firefox.R

/**
 * [ExperimentsProvider] checks for experiment branch from [Fretboard] to provide its respective content.
 * See [getAAExitButtonExperiment] for example
 *
 * Note: Consider implementing fallback options (log in Sentry using [ExperimentIllegalStateException]
 * since fretboard doesn't necessarily load the latest changes from Kinto backend. See
 * [FretboardProvider.updateExperiments] and [FretboardProvider.loadExperiments] for more details
 */
class ExperimentsProvider(private val fretboard: Fretboard, private val context: Context) {

    fun getAAExitButtonExperiment(expConfig: ExperimentConfig): String {
        val expDescriptor = checkBranchVariants(expConfig)
        return if (expDescriptor != null) {
            when {
                (expDescriptor.name.endsWith(ExperimentSuffix.A.value)) ->
                    context.resources.getString(R.string.exit_firefox_a11y,
                        context.resources.getString(R.string.firefox_tv_brand_name_short))
                (expDescriptor.name.endsWith(ExperimentSuffix.B.value)) ->
                    context.resources.getString(R.string.exit_firefox_a11y,
                        context.resources.getString(R.string.firefox_tv_brand_name_short))
                // Fallback: AA testing should be 50/50 so technically should never get to else.
                else -> {
                    Sentry.capture(ExperimentIllegalStateException("AATestIllegalBranchName"))
                    context.resources.getString(R.string.exit_firefox_a11y,
                        context.resources.getString(R.string.firefox_tv_brand_name_short))
                }
            }
        } else {
            // The user is currently not part of the experiment

            // Sentry disabled because I'm concerned about the impact of sending this error to our servers so many times
            // a day and we take no action when we see this error. See #2155 for a proper investigation.
            // Sentry.capture(NotInExperimentException("AAExperiment"))
            context.resources.getString(R.string.exit_firefox_a11y,
                context.resources.getString(R.string.firefox_tv_brand_name_short))
        }
    }

    fun shouldShowHintBar(): Boolean {
        val expDescriptor = checkBranchVariants(ExperimentConfig.HINT_BAR_TEST)
        return when {
            // The user is currently not part of the experiment
            expDescriptor == null -> false
            expDescriptor.name.endsWith(ExperimentSuffix.A.value) -> true
            expDescriptor.name.endsWith(ExperimentSuffix.B.value) -> false
            expDescriptor.name.endsWith(ExperimentSuffix.C.value) -> true
            else -> {
                Sentry.capture(ExperimentIllegalStateException("Hint Bar Illegal Branch Name"))
                false
            }
        }
    }

    fun shouldShowTvGuideChannels(): Boolean {
        val expDescriptor = checkBranchVariants(ExperimentConfig.TV_GUIDE_CHANNELS)
        return when {
            expDescriptor == null -> false // Experiment unknown, or overridden to be false.
            expDescriptor.name.endsWith(ExperimentSuffix.A.value) -> false
            expDescriptor.name.endsWith(ExperimentSuffix.B.value) -> true
            expDescriptor.name.endsWith(ExperimentSuffix.C.value) -> true
            else -> {
                Sentry.capture(ExperimentIllegalStateException("TV Guide Channels Illegal Branch Name"))
                false
            }
        }
    }

    fun shouldShowSendTab(): Boolean {
        val expDescriptor = checkBranchVariants(ExperimentConfig.SEND_TAB)
        return when {
            expDescriptor == null -> false // Experiment unknown, or overridden to be false.
            expDescriptor.name.endsWith(ExperimentSuffix.A.value) -> false
            expDescriptor.name.endsWith(ExperimentSuffix.B.value) -> true
            else -> {
                Sentry.capture(ExperimentIllegalStateException("FxA Login Illegal Branch Name"))
                false
            }
        }
    }

    /** This is not an experiment: see [ExperimentConfig.MP4_VIDEO_WORKAROUND] for details. */
    fun shouldUseMp4VideoWorkaround(): Boolean {
        val expDescriptor = checkBranchVariants(ExperimentConfig.MP4_VIDEO_WORKAROUND)
        return when {
            expDescriptor == null -> false // Experiment unknown, or overridden to be false.
            expDescriptor.name.endsWith(ExperimentSuffix.A.value) -> false
            expDescriptor.name.endsWith(ExperimentSuffix.B.value) -> true
            else -> {
                Sentry.capture(ExperimentIllegalStateException("MP4 Video Workaround Illegal Branch Name"))
                false
            }
        }
    }

    private fun shouldUseTurboRebrand(): Boolean {
        val expDescriptor = checkBranchVariants(ExperimentConfig.TURBO_MODE_REBRAND)
        return when {
            expDescriptor == null -> false // Experiment unknown, or overridden to be false.
            expDescriptor.name.endsWith(ExperimentSuffix.A.value) -> false
            expDescriptor.name.endsWith(ExperimentSuffix.B.value) -> true
            else -> {
                Sentry.capture(ExperimentIllegalStateException("Turbo Mode Rebrand Illegal Branch Name"))
                false
            }
        }
    }

    data class TurboModeToolbarContent(
        val imageId: Int,
        val contentDescriptionId: Int
    )

    fun getTurboModeToolbar() = when(shouldUseTurboRebrand()) {
        true -> TurboModeToolbarContent(imageId = R.drawable.etp_selector, contentDescriptionId = R.string.toolbar_etp)
        false -> TurboModeToolbarContent(imageId = R.drawable.turbo_selector, contentDescriptionId = R.string.turbo_mode)
    }

    data class TurboModeOnboardingContent(
        val titleId: Int,
        val descriptionId: Int,
        val enableButtonTextId: Int,
        val disableButtonTextId: Int,
        val imageId: Int,
        val imageContentDescriptionId: Int
    )

    fun getTurboModeOnboarding() = when (shouldUseTurboRebrand()) {
        true -> TurboModeOnboardingContent(
            titleId = R.string.onboarding_etp_title,
            descriptionId = R.string.onboarding_etp_description,
            enableButtonTextId = R.string.onboarding_etp_enable,
            disableButtonTextId = R.string.onboarding_etp_disable,
            imageId = R.drawable.etp_onboarding,
            imageContentDescriptionId = R.string.onboarding_etp_image_a11y
        )
        false -> TurboModeOnboardingContent(
            titleId = R.string.onboarding_turbo_mode_title,
            descriptionId = R.string.onboarding_turbo_mode_body2,
            enableButtonTextId = R.string.button_turbo_mode_keep_enabled2,
            disableButtonTextId = R.string.button_turbo_mode_turn_off2,
            imageId = R.drawable.turbo_mode_onboarding,
            imageContentDescriptionId = R.string.turbo_mode_image_a11y
        )
    }

    /**
     * Check if [ExperimentConfig] + [ExperimentSuffix] is in the experiment and return its
     * corresponding [ExperimentDescriptor].
     *
     * Return null otherwise
     */
    private fun checkBranchVariants(expConfig: ExperimentConfig): ExperimentDescriptor? {
        for (suffix in ExperimentSuffix.values()) {
            val expDescriptor = ExperimentDescriptor(expConfig.value + ":" + suffix.name)
            if (fretboard.isInExperiment(context, expDescriptor)) {
                // Correct experiment variant is found
                return expDescriptor
            }
        }

        // No matching experiment, so return null
        return null
    }
}
