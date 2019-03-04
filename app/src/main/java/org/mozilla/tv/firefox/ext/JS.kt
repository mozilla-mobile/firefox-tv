/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

object JS {
    const val CACHE_VAR = "_firefoxForFireTvPreviouslyFocusedElement"
    const val CACHE_JS = "var $CACHE_VAR = document.activeElement;"

    // This will only happen if YouTube is loading or navigation has broken
    const val noElementFocused = "document.activeElement === null"
    // This will only happen if YouTube is loading or navigation has broken
    const val bodyElementFocused = "document.activeElement.tagName === \"BODY\""
    const val sidebarFocused = "document.activeElement.parentElement.parentElement.id === 'guide-list'"

    const val pauseVideo = "document.querySelectorAll('video').forEach(v => v.pause());"
}
