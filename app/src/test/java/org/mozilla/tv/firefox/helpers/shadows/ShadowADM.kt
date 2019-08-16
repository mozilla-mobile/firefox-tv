/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.helpers.shadows

import android.content.Context
import com.amazon.device.messaging.ADM
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/**
 * Custom Shadow for [ADM]
 */
@Implements(ADM::class)
class ShadowADM {

    /**
     * [ADM] has RuntimeException stub in the compileOnly constructor
     * which hinders unit testing. This overrides to exclude super() and the exception stub.
     */
    @Implementation
    @Suppress("UNUSED_PARAMETER")
    fun __constructor__(var1: Context) {}

    /**
     * [ADM] has RuntimeException stub in [ADM.isSupported]
     * which hinders unit testing. This overrides to exclude super() and the exception stub.
     */
    @Implementation
    fun isSupported(): Boolean { return true }
}
