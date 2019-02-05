/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.architecture

import android.arch.lifecycle.ViewModel
import android.support.v4.app.Fragment
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.tv.firefox.MainActivity
import org.mozilla.tv.firefox.pinnedtile.PinnedTileViewModel
import org.mozilla.tv.firefox.settings.SettingsFragment
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.support.v4.SupportFragmentController

@RunWith(RobolectricTestRunner::class)
class FirefoxViewModelProvidersTest {

    private lateinit var mainActivity: MainActivity
    private lateinit var fragment: Fragment

    @Before
    fun setUp() {
        // Ideally, we'd run the Activity through onCreate but Robolectric fails on WebView shadow creation.
        mainActivity = Robolectric.buildActivity(MainActivity::class.java).get()
        fragment = SupportFragmentController.of(SettingsFragment.create()).create().get()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WHEN passed an unknown ViewModel THEN of(FragmentActivity) throws an exception`() {
        FirefoxViewModelProviders.of(mainActivity).get(UnknownViewModel::class.java)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `WHEN passed an unknown ViewModel THEN of(Fragment) throws an exception`() {
        FirefoxViewModelProviders.of(fragment).get(UnknownViewModel::class.java)
    }

    @Test
    fun `WHEN passed a valid ViewModel THEN of(FragmentActivity) returns a non-null value`() {
        assertNotNull(FirefoxViewModelProviders.of(mainActivity).get(PinnedTileViewModel::class.java))
    }

    @Test
    fun `WHEN passed a valid ViewModel THEN of(Fragment) returns a non-null value`() {
        assertNotNull(FirefoxViewModelProviders.of(fragment).get(PinnedTileViewModel::class.java))
    }
}

private class UnknownViewModel : ViewModel()
