/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.utils

import android.os.Build

/**
 * Contains information about the device the app is running on.
 */
class DeviceInfo {

    /**
     * Translate Fire TV model codes into device names
     */
    fun getDeviceModel(): String {
        // Amazon does not localize their device names, so we do not need to
        val deviceCodeMap = mapOf(
            "AFTA" to "Fire TV Cube",
            "AFTN" to "Fire TV 4K",
            "AFTS" to "Fire TV",
            "AFTB" to "Fire TV",
            "AFTMM" to "Fire TV Stick 4K",
            "AFTT" to "Fire TV Stick",
            "AFTM" to "Fire TV Stick",
            "AFTRS" to "Fire TV Edition - Element 4K",
            "AFTKMST12" to "Fire TV Edition - Toshiba 4K",
            "AFTBAMR311" to "Fire TV Edition - Toshiba HD",
            "AFTJMST12" to "Fire TV Edition - Insignia 4K",
            "AFTEAMR311" to "Fire TV Edition - Insignia HD"
        )

        return deviceCodeMap.getOrElse(Build.MODEL) { "Fire TV" }
    }
}
