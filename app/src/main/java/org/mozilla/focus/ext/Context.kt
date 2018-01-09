/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Context
import android.view.accessibility.AccessibilityManager

// Extension functions for the Context class

/**
 * The (visible) version name of the application, as specified by the <manifest> tag's versionName
 * attribute. E.g. "2.0".
 */
val Context.appVersionName: String?
    get() {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.versionName
    }

val Context.isVoiceViewEnabled: Boolean
    get() {
        // In my understanding, isTouchExplorationEnabled should represent VoiceView being enabled,
        // whereas isEnabled says if the accessibility manager is enabled for other services.
        val am = this.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        return am.isEnabled && am.isTouchExplorationEnabled
    }