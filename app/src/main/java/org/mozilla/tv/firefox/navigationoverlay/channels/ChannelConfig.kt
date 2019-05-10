/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

/**
 * Data object that delivers custom configuration details to new channels
 */
data class ChannelConfig(
        val onClickTelemetry: ((ChannelTile) -> Unit)? = null,
        val onLongClickTelemetry: ((ChannelTile) -> Unit)? = null,
        val onFocusTelemetry: ((ChannelTile, Boolean) -> Unit)? = null
)
