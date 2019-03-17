/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.navigationoverlay

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import org.mozilla.tv.firefox.ext.map
import org.mozilla.tv.firefox.session.SessionRepo
import org.mozilla.tv.firefox.utils.URLs

class NavigationOverlayViewModel(sessionRepo: SessionRepo) : ViewModel() {
    @Suppress("DEPRECATION")
    val viewIsSplit: LiveData<Boolean> = sessionRepo.legacyState.map {
        it.currentUrl != URLs.APP_URL_HOME
    }
}
