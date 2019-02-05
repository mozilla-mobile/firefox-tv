/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class StringKtTest {

    @Test
    fun `WHEN youtube TV pages are entered THEN it is a youtube TV uri`() {
        arrayOf(
            "https://www.youtube.com/tv#/", // initial home page redirect
            "https://www.youtube.com/tv#/surface?c=FEtopics&resume", // final home page redirect
            "https://www.youtube.com/tv#/watch/video/idle?v=Q8TGx71PVgQ&list=RDQ8TGx71PVgQ&resume" // video page
        ).forEach {
            assertTrue(it, it.isUriYouTubeTV)
        }
    }

    @Test
    fun `WHEN youtube TV with capitals pages are entered THEN it is a youtube TV uri`() {
        arrayOf(
            "https://www.youtube.com/TV#/surface?c=FEtopics&resume" // YOUTUBE.COM/TV will redirect to this
        ).forEach {
            assertTrue(it, it.isUriYouTubeTV)
        }
    }

    @Test
    fun `WHEN a youtube TV string contains leading or trailing spaces THEN it is a youtube TV uri`() {
        arrayOf(
            " https://www.youtube.com/tv#/  "
        ).forEach {
            assertTrue(it, it.isUriYouTubeTV)
        }
    }

    @Test
    fun `WHEN tv dot youtube pages are entered THEN it is not a youtube TV uri`() {
        arrayOf(
            "https://tv.youtube.com/welcome/" // home page
        ).forEach {
            assertFalse(it, it.isUriYouTubeTV)
        }
    }

    @Test
    fun `WHEN youtube desktop pages are entered THEN it is not a youtube TV uri`() {
        arrayOf(
            "https://www.youtube.com/", // home page
            "https://www.youtube.com/watch?v=Q8TGx71PVgQ&list=RDQ8TGx71PVgQ&start_radio=1" // video page
        ).forEach {
            assertFalse(it, it.isUriYouTubeTV)
        }
    }

    @Test
    fun `WHEN youtube mobile pages are entered THEN it is not a youtube TV uri`() {
        arrayOf(
            "https://m.youtube.com/", // home page
            "https://m.youtube.com/watch?v=Q8TGx71PVgQ&list=RDQ8TGx71PVgQ&start_radio=1" // video page
        ).forEach {
            assertFalse(it, it.isUriYouTubeTV)
        }
    }

    @Test
    fun `WHEN non-youtube pages are entered THEN it is not a youtube TV uri`() {
        arrayOf(
            "https://www.mozilla.org/en-US/",
            "https://en.wikipedia.org/wiki/YouTube_TV"
        ).forEach {
            assertFalse(it, it.isUriYouTubeTV)
        }
    }

    @Test
    fun `WHEN bad urls are entered THEN it is not a youtube TV uri`() {
        arrayOf(
            "",
            " ",
            "not-a-uri"
        ).forEach {
            assertFalse(it, it.isUriYouTubeTV)
        }
    }

    @Ignore // this fails. We should fix it by moving to the Uri class.
    @Test
    fun `WHEN non youtube TV uris containing youtube tv uri str are entered THEN it is not a youtube TV uri`() {
        arrayOf(
            "https://en.wikipedia.org/wiki/youtube.com/tv",
            "https://developer.mozilla.org/en-US/docs/Mozilla/Add-ons/youtube.com/tv/WebExtensions",
            "https://mozilla.org/youtube.com/tv"
        ).forEach {
            assertFalse(it, it.isUriYouTubeTV)
        }
    }
}
