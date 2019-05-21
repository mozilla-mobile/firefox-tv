/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import android.content.Context
import org.mozilla.tv.firefox.channels.ChannelTile
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration

private val TELEMETRY = TelemetryIntegration.INSTANCE

/**
 * Data object that delivers custom configuration details to new channels
 */
data class ChannelConfig(
    val onClickTelemetry: ((ChannelTile) -> Unit)? = null,
    val onLongClickTelemetry: ((ChannelTile) -> Unit)? = null,
    val onFocusTelemetry: ((ChannelTile, Boolean) -> Unit)? = null,
    val itemsMayBeRemoved: Boolean = false
) {
    companion object {
        fun getPocketConfig(): ChannelConfig = ChannelConfig(
                onClickTelemetry = { tile -> TELEMETRY.pocketVideoClickEvent(tile.id) },
                // TODO focus telemetry should probably only be sent on focus gain, but this is
                //  how our previous implementation worked. Keeping this to maintain data consistency
                onFocusTelemetry = { tile, _ -> TELEMETRY.pocketVideoImpressionEvent(tile.id) }
        )

        fun getPinnedTileConfig(context: Context): ChannelConfig = ChannelConfig(
                onClickTelemetry = { tile -> TELEMETRY.homeTileClickEvent(context, tile) },
                itemsMayBeRemoved = true
        )
    }
}
