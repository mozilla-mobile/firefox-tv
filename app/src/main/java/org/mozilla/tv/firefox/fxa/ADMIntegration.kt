/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import android.app.Application
import com.amazon.device.messaging.ADM
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import mozilla.components.concept.push.PushProcessor
import mozilla.components.concept.sync.Device
import mozilla.components.concept.sync.TabData
import mozilla.components.feature.push.AutoPushFeature
import mozilla.components.feature.push.PushConfig
import mozilla.components.feature.push.ServiceType
import mozilla.components.feature.sendtab.SendTabFeature
import mozilla.components.service.fxa.manager.FxaAccountManager
import mozilla.components.support.base.log.logger.Logger

private val logger = Logger("FFTV - ADMIntegration")
private const val senderId = "fftv"

/**
 * All Amazon Device Messaging (ADM) API calls should be done here. This class checks that the device supports ADM before trying to use it.
 * When adding new functionality, do not forget to check [isADMAvailable] first.
 *
 * When running on emulator or any device not supporting ADM and Instant Run is enabled,
 * you may see ClassNotFound log warnings - these can be safely ignored.
 */
class ADMIntegration(private val app: Application) {
    private val isADMAvailable = run {
        // Suggested implementation from https://developer.amazon.com/docs/adm/integrate-your-app.html#gracefully-degrade-if-adm-is-unavailable
        try {
            Class.forName("com.amazon.device.messaging.ADM")
            ADM(app).isSupported
        } catch (e: ClassNotFoundException) {
            logger.warn("ADM is not available on this device.")
            false
        }
    }

    private val pushFeature by lazy {
        AutoPushFeature(app, ADMService(), PushConfig(senderId = senderId, serviceType = ServiceType.ADM))
    }

    private val _receivedTabsRaw: PublishSubject<ReceivedTabs> = PublishSubject.create()
    val receivedTabsRaw: Observable<ReceivedTabs> = _receivedTabsRaw.hide()

    fun createSendTabFeature(accountManager: FxaAccountManager) {
        if (isADMAvailable) {
            // For push to work in debug builds (not needed for release), an api key is needed in the assets folder.
            // See README for instructions.
            SendTabFeature(accountManager, pushFeature) { device, tabData ->
                _receivedTabsRaw.onNext(ReceivedTabs(device, tabData))
            }
        }
    }

    fun initPush() {
        if (isADMAvailable) {
            PushProcessor.install(pushFeature)
        }
    }

    fun initPushFeature() {
        if (isADMAvailable) {
            pushFeature.initialize()
        }
    }

    fun shutdownPushFeature() {
        if (isADMAvailable) {
            pushFeature.shutdown()
        }
    }

    /**
     * A tab, as received by ADM. We let consumers handle the conversion to more useful types.
     */
    data class ReceivedTabs(val device: Device?, val tabData: List<TabData>)
}
