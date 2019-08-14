/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import mozilla.components.lib.push.amazon.AbstractAmazonPushService
import com.amazon.device.messaging.ADMMessageReceiver

class ADMService : AbstractAmazonPushService() {
    class ADMReceiver : ADMMessageReceiver(ADMService::class.java)
}
