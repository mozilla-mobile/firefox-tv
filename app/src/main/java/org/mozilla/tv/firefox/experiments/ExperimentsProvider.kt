/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.experiments

import android.content.Context
import mozilla.components.service.fretboard.Fretboard
import org.mozilla.tv.firefox.R

/**
 * [ExperimentsProvider] checks for experiment branch from [Fretboard] to provide its respective content.
 * See [getAAExitButtonExperiment] for example
 */
class ExperimentsProvider(private val fretboard: Fretboard, private val context: Context) {

    fun getAAExitButtonExperiment(expConfig: ExperimentConfig): String {
        return if (fretboard.isInExperiment(context, expConfig.value)) {
            context.resources.getString(R.string.exit_firefox_a11y,
                    context.resources.getString(R.string.firefox_tv_brand_name_short))
        } else {
            context.resources.getString(R.string.exit_firefox_a11y,
                    context.resources.getString(R.string.firefox_tv_brand_name_short))
        }
    }
}
