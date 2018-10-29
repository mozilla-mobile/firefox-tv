/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import org.mozilla.tv.firefox.BuildConfig
import org.mozilla.tv.firefox.pocket.PocketRepoState

/**
 * Computes information that must be derived from the [BuildConfig].
 *
 * This logic is often simple but noisy, so pulling it out of client code improves readability.
 */
class BuildConfigDerivables {
    @Suppress("SENSELESS_COMPARISON") // Values of BuildConfig can change but the compiler doesn't know that
    val initialPocketRepoState = when {
        BuildConfig.POCKET_KEY == null -> PocketRepoState.NoKey
        else -> PocketRepoState.Loading
    }
}
