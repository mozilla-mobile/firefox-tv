/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.experiments

import mozilla.components.service.fretboard.ExperimentDescriptor

/**
 * [ExperimentConfig] defines a set of supported [ExperimentDescriptor] from [Fretboard]
 */
enum class ExperimentConfig(val value: String) {
    // N.B.: an experiment WILL ALWAYS BE DISABLED if it is not added to the fretboard backend.
    //
    // The format is: "ExperimentName-IssueNumberForExperiment"
    AA_TEST("AAtest-1675"),
    HINT_BAR_TEST("HintBar-2011"),
    TV_GUIDE_CHANNELS("TvGuideChannels-2195"),
    TURBO_MODE_REBRAND("TurboModeRebrand-2689"),

    /**
     * This is not an experiment. If Amazon deploys a fix for this bug, our workaround
     * may break it: we use this flag as an option to disable this workaround remotely.
     */
    MP4_VIDEO_WORKAROUND("Mp4VideoWorkaround-2540"),

    /**
     * This flag does nothing; prior to v4.5, this flag was used to enable the incomplete
     * implementation of FxA sign in and send tab for internal testing until we hard-coded it
     * enabled for release. We keep this flag around as documentation because this file is linked
     * to as living documentation for which experiments QA can be enable.
     *
     * We chose to hard-code it as enabled because if we used this flag, the incomplete
     * implementation would be enabled on older versions of the app and we didn't feel it was
     * worth the time to add a new flag as this was not being A/B tested.
     */
    @Deprecated("This flag does nothing")
    @Suppress("unused")
    SEND_TAB("SendTab-2511")
}

/**
 * [ExperimentSuffix] defines branch variant suffix to each experiments
 */
enum class ExperimentSuffix(val value: String) {
    A("A"),
    B("B"),
    C("C"),
    D("D"),
    E("E"),
}
