/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay.channels

import org.mozilla.tv.firefox.pinnedtile.PinnedTileRepo
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration

enum class TileType { PINNED_TILE }

/**
 * ChannelRepo abstracts app logic that requires exposures to other repos (e.g. removing a pinned
 * tile channel would require a reference to [PinnedTileRepo].
 *
 * [TileType] is used to determine which Repo is responsible to handle requested operations
 */
class ChannelRepo(
    private val pinnedTileRepo: PinnedTileRepo
) {
    fun removeChannelContent(tileData: ChannelTile) {
        when (tileData.type) {
            TileType.PINNED_TILE -> {
                TelemetryIntegration.INSTANCE.homeTileRemovedEvent(tileData)
                pinnedTileRepo.removePinnedTile(tileData.url)
            }
        }
    }
}
