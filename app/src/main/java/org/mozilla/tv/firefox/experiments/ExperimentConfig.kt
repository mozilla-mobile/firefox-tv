/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.experiments

import mozilla.components.service.fretboard.ExperimentDescriptor

/**
 * [ExperimentConfig] defines a set of supported [ExperimentDescriptor] from [Fretboard]
 */
enum class ExperimentConfig(val value: String) {
    AA_TEST("AAtest-1675")
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
