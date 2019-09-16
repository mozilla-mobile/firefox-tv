/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import mozilla.components.concept.sync.Profile
import mozilla.components.support.base.log.logger.Logger
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channels.ImageSetStrategy
import org.mozilla.tv.firefox.telemetry.SentryIntegration

private val logger = Logger("FxaProfile")

/**
 *  A wrapper for [Profile]. This insulates us from any upstream changes to the API, and
 *  allows us to validate related data at the edge of our app.
 *
 *  Note that [Profile] has additional fields not reflected in [FxaProfile] (namely
 *  [Profile.uid] and [Profile.email]). These are not currently required by our app, but
 *  can be added if that ever changes.
 */
data class FxaProfile(
    val avatarSetStrategy: ImageSetStrategy,
    val displayName: String
)

// This URL will be sent for any user that has no avatar set. It is improperly sized for
// our ImageView, so we filter it out here and use an SVG instead
private const val DEFAULT_FXA_AVATAR_URL = "https://firefoxusercontent.com/00000000000000000000000000000000"
private const val DEFAULT_AVATAR_RESOURCE = R.drawable.ic_default_avatar

fun Profile.toDomainObject(): FxaProfile {
    val avatar = this.avatar
    val displayName = this.displayName
    val email = this.email

    val validatedAvatar = when {
        avatar == null -> ImageSetStrategy.ById(DEFAULT_AVATAR_RESOURCE)
        avatar.url == DEFAULT_FXA_AVATAR_URL -> ImageSetStrategy.ById(DEFAULT_AVATAR_RESOURCE)
        else -> ImageSetStrategy.ByPath(avatar.url, DEFAULT_AVATAR_RESOURCE, DEFAULT_AVATAR_RESOURCE)
    }

    @Suppress("ThrowableNotThrown")
    val validatedDisplayName = when {
        displayName != null -> displayName
        email != null -> email
        else -> {
            SentryIntegration.captureAndLogError(logger,
                IllegalStateException("FxA profile displayName and email fields are unexpectedly both null"))
            // According to the FxA team, email should never be null, so we log an error
            // and fall back to an empty string here
            ""
        }
    }

    return FxaProfile(validatedAvatar, validatedDisplayName)
}
