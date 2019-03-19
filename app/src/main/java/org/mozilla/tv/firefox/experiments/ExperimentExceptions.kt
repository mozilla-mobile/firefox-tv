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

/**
 * [NotInExperimentException] is for logging users that are currently not part of an
 * Experiment
 *
 * Our current implementation for [FretboardProvider.loadExperiments] and
 * [FretboardProvider.updateExperiments] are not synchronized. This is causing some of the
 * new users to be not part of an experiment (even though they should be) in the initial
 * launch (restarting the app would have you properly sync'd up) - #1978
 *
 * @param name: Name of the experiment
 */
open class NotInExperimentException(name: String) : Exception(name)
