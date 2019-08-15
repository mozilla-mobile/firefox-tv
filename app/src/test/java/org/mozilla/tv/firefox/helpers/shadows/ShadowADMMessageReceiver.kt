/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.shadows

import com.amazon.device.messaging.ADMMessageHandlerBase
import com.amazon.device.messaging.ADMMessageReceiver
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Custom Shadow for [ADMMessageReceiver]
 */
@Implements(ADMMessageReceiver::class)
class ShadowADMMessageReceiver {

    /**
     * [ADMMessageReceiver] has RuntimeException stub in the compileOnly constructor
     * which hinders unit testing. This overrides to exclude super() and the exception stub.
     */
    @Implementation
    @Suppress("UNUSED_PARAMETER")
    fun __constructor__(arg0: Class<out ADMMessageHandlerBase>) {}
}
