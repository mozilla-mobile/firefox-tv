/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.ext

import android.content.Context
import org.mozilla.focus.locale.LocaleManager
import java.util.Locale

// I extracted this code from WebFragment and I don't fully understand LocaleManager
// so I opted to put this code here instead of directly inside LocaleManager.
fun LocaleManager.maybeApplyNonSystemLocale(context: Context) {
    if (isMirroringSystemLocale(context)) return

    val currentLocale = getCurrentLocale(context);
    Locale.setDefault(currentLocale);

    val resources = context.getResources();
    val config = resources.getConfiguration();
    config.setLocale(currentLocale);
    resources.updateConfiguration(config, null);
}
