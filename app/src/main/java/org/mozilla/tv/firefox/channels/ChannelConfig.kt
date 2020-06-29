/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels

import android.content.Context
import org.mozilla.tv.firefox.architecture.KillswitchLocales
import org.mozilla.tv.firefox.ext.serviceLocator
import org.mozilla.tv.firefox.telemetry.TelemetryIntegration
import java.util.Locale

private val TELEMETRY = TelemetryIntegration.INSTANCE

/**
 * Data object that delivers custom configuration details to new channels
 */
data class ChannelConfig(
    val onClickTelemetry: ((ChannelTile) -> Unit)? = null,
    val onLongClickTelemetry: ((ChannelTile) -> Unit)? = null,
    val onFocusTelemetry: ((ChannelTile, Boolean) -> Unit)? = null,
    val itemsMayBeRemoved: Boolean = false,
    val isEnabledInCurrentExperiment: Boolean,
    val enabledInLocales: KillswitchLocales
) {
    companion object {
        fun getPinnedTileConfig(context: Context): ChannelConfig = ChannelConfig(
            onClickTelemetry = { tile -> TELEMETRY.homeTileClickEvent(context, tile) },
            itemsMayBeRemoved = true,
            isEnabledInCurrentExperiment = true,
            enabledInLocales = KillswitchLocales.All
        )

        fun getTvGuideConfig(context: Context): ChannelConfig = ChannelConfig(
            onClickTelemetry = { tile -> TELEMETRY.homeTileClickEvent(context, tile) },
            itemsMayBeRemoved = true,
            isEnabledInCurrentExperiment =
                context.serviceLocator.experimentsProvider.shouldShowTvGuideChannels(),
            enabledInLocales = KillswitchLocales.ActiveIn(Locale.US)
        )
    }
}
