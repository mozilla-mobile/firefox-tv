/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import io.reactivex.Observable
import mozilla.components.concept.sync.DeviceType
import mozilla.components.concept.sync.TabData
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.framework.UnresolvedString
import org.mozilla.tv.firefox.telemetry.SentryIntegration

private val logger = Logger(FxaReceivedTab::class.java.simpleName)

/**
 * A data container for tabs received from a single device via FxA. While we currently support
 * only one tab, this container can actually contain multiple received tabs.
 *
 * This container exists to avoid exposing the underlying [DeviceEvent.TabReceived] events to
 * consumers.
 *
 * @param [tabReceivedNotificationText] If possible, this will describe where the tab came from
 * (e.g., "Sent from Severin's Pixel 2").  If we don't know those details, it will be more
 * vague (e.g., "Tab received")
 */
data class FxaReceivedTab(
    val url: String,
    val tabReceivedNotificationText: UnresolvedString,
    val metadata: Metadata
) {
    /**
     * @param [receivedUrlCount] the number of URLs included in the original push. At this time,
     * this is only used for telemetry
     */
    data class Metadata(
        val deviceType: DeviceType, // We expose the FxA DeviceType to avoid excessive boilerplate.
        val receivedUrlCount: Int
    )
}

fun Observable<ADMIntegration.ReceivedTabs>.filterMapToDomainObject(
    sentryIntegration: SentryIntegration = SentryIntegration
): Observable<FxaReceivedTab> = this
    .flatMap { admTabs ->
        val urls = admTabs.tabData
            .map(TabData::url)
            // Note that we are intentionally discarding all but the first tab here.
            // TODO fix this in #2777
            .filter(String::isNotBlank)
        val url = urls.firstOrNull()

        if (url == null) {
            sentryIntegration.captureAndLogError(logger,
                ReceiveTabException("Received tab event with only blank URLs"))
            return@flatMap Observable.empty<FxaReceivedTab>()
        }

        val tabReceivedNotificationText = when (admTabs.device) {
            null -> UnresolvedString(R.string.fxa_tab_sent_toast_no_device)
            else -> UnresolvedString(R.string.fxa_tab_sent_toast, listOf(admTabs.device.displayName))
        }

        val metadata = FxaReceivedTab.Metadata(
            deviceType = admTabs.device?.deviceType ?: DeviceType.UNKNOWN,
            receivedUrlCount = urls.size
        )

        val domainObject = FxaReceivedTab(
            url = url,
            tabReceivedNotificationText = tabReceivedNotificationText,
            metadata = metadata
        )

        Observable.just(domainObject)
    }

/** An Exception thrown when during the receive tab process. */
private class ReceiveTabException(msg: String) : Exception(msg)
