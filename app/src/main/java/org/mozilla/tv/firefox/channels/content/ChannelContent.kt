/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.channels.content

import org.mozilla.tv.firefox.channels.ImageSetStrategy

/**
 * Namespace for retrieval of data objects that back 'TV Guide'-style channel
 * content.
 *
 * Functions are added as extension functions so that they can be organized
 * into separate files.
 */
object ChannelContent {

    /**
     * Helper function shared by various content files. See [getNewsChannels]
     * for an example.
     */
    fun setImage(id: Int): ImageSetStrategy = ImageSetStrategy.ById(id)
}
