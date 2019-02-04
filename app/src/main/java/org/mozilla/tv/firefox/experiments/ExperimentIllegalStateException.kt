/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.experiments

/**
 * [ExperimentIllegalStateException] signals an illegal or inappropriate experiment state
 *
 * For example, in AB testing (60/40), any experiment branches other than "A" or "B" would be
 * considered an illegal experiment state
 */
open class ExperimentIllegalStateException(message: String) : IllegalStateException(message)
