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
 * A data container for tabs received from a single device via FxA. While we currently support only
 * one tab, this container can actually contain multiple received tabs.
 *
 * This container exists to avoid exposing the underlying [DeviceEvent.TabReceived] events to consumers.
 */
data class FxaReceivedTab(
    val url: String,
    val sourceDescription: UnresolvedString,
    val metadata: Metadata
) {
    data class Metadata(
        val deviceType: DeviceType // We expose the FxA DeviceType to avoid excessive boilerplate.
    )
}

fun Observable<ADMIntegration.ReceivedTabs>.filterMapToDomainObject(
    sentryIntegration: SentryIntegration = SentryIntegration
): Observable<FxaReceivedTab> = this
    .flatMap { admTabs ->
        val url = admTabs.tabData
            .map(TabData::url)
            .firstOrNull(String::isNotBlank)

        if (url == null) {
            sentryIntegration.captureAndLogError(logger,
                ReceiveTabException("Received tab event with only blank URLs"))
            return@flatMap Observable.empty<FxaReceivedTab>()
        }

        val sourceDescription = when (admTabs.device) {
            null -> UnresolvedString(R.string.fxa_tab_sent_toast_no_device)
            else -> UnresolvedString(R.string.fxa_tab_sent_toast, listOf(admTabs.device.displayName))
        }

        val metadata = FxaReceivedTab.Metadata(
            deviceType = admTabs.device?.deviceType ?: DeviceType.UNKNOWN
        )

        val domainObject = FxaReceivedTab(
            url = url,
            sourceDescription = sourceDescription,
            metadata = metadata
        )

        Observable.just(domainObject)
    }

/** An Exception thrown when during the receive tab process. */
private class ReceiveTabException(msg: String) : Exception(msg)
