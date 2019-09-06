/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

@file:Suppress("DEPRECATION") // TODO remove this file when we migrate to glean

package org.mozilla.tv.firefox.telemetry

import org.mozilla.telemetry.Telemetry
import org.mozilla.telemetry.TelemetryHolder

/**
 * Allows us to use the now deprecated [TelemetryHolder] without deprecation annotations.
 *
 * This is required because the only way to suppress deprecation errors in import statements
 * is to suppress them file-wide, which could cover up future deprecation problems.
 */
object DeprecatedTelemetryHolder {
    fun get(): Telemetry = TelemetryHolder.get()
    fun set(telemetry: Telemetry) = TelemetryHolder.set(telemetry)
}
