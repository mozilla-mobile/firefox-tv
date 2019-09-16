/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.fxa

import io.mockk.mockkObject
import io.mockk.verify
import mozilla.components.concept.sync.Avatar
import mozilla.components.concept.sync.Profile
import org.junit.Assert
import org.junit.Test
import org.mozilla.tv.firefox.R
import org.mozilla.tv.firefox.channels.ImageSetStrategy
import org.mozilla.tv.firefox.telemetry.SentryIntegration

class FxaProfileTest {

    private val defaultProfileAvatarImage = ImageSetStrategy.ById(R.drawable.ic_default_avatar)

    @Test
    fun `GIVEN profile has valid displayName WHEN profile is converted to domain object THEN displayName should be displayName`() {
        val displayName = "displayName"

        val profiles = listOf(
            Profile(null, null, null, displayName),
            Profile("uid", null, null, displayName),
            Profile(null, "email", null, displayName),
            Profile(null, null, Avatar("url", true), displayName),
            Profile("uid", "email", Avatar("url", true), displayName)
        )

        profiles.map { it.toDomainObject() }
            .map { it.displayName }
            .forEach { Assert.assertEquals(displayName, it) }
    }

    @Test
    fun `GIVEN profile has no displayName WHEN profile is converted to domain object THEN displayName should be email`() {
        val email = "email"
        val profile = Profile("uid", email, null, null)
        Assert.assertEquals(email, profile.toDomainObject().displayName)
    }

    @Test
    fun `GIVEN profile has no displayName or email WHEN profile is converted to domain object THEN displayName should be blank`() {
        val profile = Profile("uid", null, null, null)
        Assert.assertEquals("", profile.toDomainObject().displayName)
    }

    @Test
    fun `GIVEN profile has no displayName or email WHEN profile is converted to domain object THEN sentry should log an error`() {
        mockkObject(SentryIntegration)

        Profile("uid", null, null, null).toDomainObject()
        verify(exactly = 1) { SentryIntegration.captureAndLogError(any(), any()) }
    }

    @Test
    fun `GIVEN profile has no avatar WHEN profile is converted to domain object THEN default resource image should be used`() {
        val profile = Profile("uid", null, null, null)

        Assert.assertEquals(defaultProfileAvatarImage, profile.toDomainObject().avatarSetStrategy)
    }

    @Test
    fun `GIVEN profile avatar is fxa default WHEN profile is converted to domain object THEN default resource image should be used`() {
        val profiles = listOf(
            Profile("uid", null, Avatar("https://firefoxusercontent.com/00000000000000000000000000000000", true), null),
            Profile("uid", null, Avatar("https://firefoxusercontent.com/00000000000000000000000000000000", false), null),
            Profile(null, null, Avatar("https://firefoxusercontent.com/00000000000000000000000000000000", false), null),
            Profile("uid", null, Avatar("https://firefoxusercontent.com/00000000000000000000000000000000", false), "display")
        )

        profiles.map { it.toDomainObject() }
            .map { it.avatarSetStrategy }
            .forEach { Assert.assertEquals(defaultProfileAvatarImage, it) }
    }

    @Test
    fun `GIVEN profile avatar is nonnull and not default WHEN profile is converted to domain object THEN that url should be used`() {
        val url = "https://www.mozilla.org"
        val expectedStrategy = ImageSetStrategy.ByPath(url, placeholderId = R.drawable.ic_default_avatar, errorId = R.drawable.ic_default_avatar)
        val profiles = listOf(
            Profile("uid", null, Avatar(url, true), null),
            Profile("uid", null, Avatar(url, false), null),
            Profile(null, null, Avatar(url, false), null),
            Profile("uid", null, Avatar(url, false), "display")
        )

        profiles.map { it.toDomainObject() }
            .map { it.avatarSetStrategy }
            .forEach { Assert.assertEquals(expectedStrategy, it) }
    }
}