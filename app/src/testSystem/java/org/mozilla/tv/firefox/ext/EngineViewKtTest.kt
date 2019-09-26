/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import android.webkit.WebBackForwardList
import android.webkit.WebView
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import mozilla.components.concept.engine.EngineView
import org.junit.Before
import org.junit.Test
import org.mozilla.tv.firefox.helpers.FakeWebBackForwardList
import org.mozilla.tv.firefox.helpers.FakeWebHistoryItem
import org.mozilla.tv.firefox.helpers.toFakeWebBackForwardList
import org.mozilla.tv.firefox.utils.URLs

private const val URL_INITIAL_SIGN_IN = "https://accounts.firefox.com/authorization?action=email&response_type=code&client_id=85da77264642d6a1&scope=profile+https%3A%2F%2Fidentity.mozilla.com%2Fapps%2Foldsync&state=madeUpState&code_challenge_method=S256&code_challenge=madeUpCodeChallenge&access_type=offline&keys_jwk=madeUpKeys&redirect_uri=https%3A%2F%2Faccounts.firefox.com%2Foauth%2Fsuccess%2F85da77264642d6a1"

private const val URL_SIGN_IN_SUCCESS = "https://accounts.firefox.com/oauth/signin?action=email&response_type=code&client_id=85da77264642d6a1&scope=profile%2Bhttps%3A%2F%2Fidentity.mozilla.com%2Fapps%2Foldsync&state=madeUpState&code_challenge_method=S256&code_challenge=madeUpCodeChallenge&access_type=offline&keys_jwk=madeUpKeys&redirect_uri=https%3A%2F%2Faccounts.firefox.com%2Foauth%2Fsuccess%2F85da77264642d6a1"
private val URL_FLOW_NO_VALIDATION = listOf(
    URL_INITIAL_SIGN_IN,
    URL_SIGN_IN_SUCCESS
)

private const val URL_NEEDS_VALIDATION = "https://accounts.firefox.com/oauth/signin?action=email&response_type=code&client_id=85da77264642d6a1&scope=profile%2Bhttps%3A%2F%2Fidentity.mozilla.com%2Fapps%2Foldsync&state=madeUpState&code_challenge_method=S256&code_challenge=madeUpCodeChallenge&access_type=offline&keys_jwk=madeUpKeys&redirect_uri=https%3A%2F%2Faccounts.firefox.com%2Foauth%2Fsuccess%2F85da77264642d6a1"
private const val URL_NEEDS_VALIDATION_2 = "https://accounts.firefox.com/confirm_signin?action=email&response_type=code&client_id=85da77264642d6a1&scope=profile%2Bhttps%3A%2F%2Fidentity.mozilla.com%2Fapps%2Foldsync&state=madeUpState&code_challenge_method=S256&code_challenge=madeUpCodeChallenge&access_type=offline&keys_jwk=madeUpKeys&redirect_uri=https%3A%2F%2Faccounts.firefox.com%2Foauth%2Fsuccess%2F85da77264642d6a1"
private const val URL_VALIDATION_CONFIRMED = "https://accounts.firefox.com/signin_confirmed?action=email&response_type=code&client_id=85da77264642d6a1&scope=profile%2Bhttps%3A%2F%2Fidentity.mozilla.com%2Fapps%2Foldsync&state=madeUpState&code_challenge_method=S256&code_challenge=madeUpCodeChallenge&access_type=offline&keys_jwk=madeUpKeys&redirect_uri=https%3A%2F%2Faccounts.firefox.com%2Foauth%2Fsuccess%2F85da77264642d6a1"
private val URL_FLOW_WITH_EMAIL_VALIDATION = listOf(
    URL_INITIAL_SIGN_IN,
    URL_NEEDS_VALIDATION,
    URL_NEEDS_VALIDATION_2,
    URL_VALIDATION_CONFIRMED
)

class EngineViewKtTest {

    private lateinit var engineView: EngineView

    private lateinit var mockWebView: WebView
    private lateinit var backForwardList: WebBackForwardList
    private var canGoBackOrForward = true

    @Before
    fun setUp() {
        mockkStatic("org.mozilla.tv.firefox.ext.EngineViewKt")

        // These values are expected to be replaced in individual tests.
        backForwardList = mockk()
        canGoBackOrForward = true

        mockWebView = mockk {
            every { goBackOrForward(any()) } returns Unit

            every { canGoBackOrForward(any()) } answers { canGoBackOrForward }
            every { copyBackForwardList() } answers { backForwardList }
        }

        engineView = mockk {
            every { webView } returns mockWebView
        }
    }

    @Test
    fun `GIVEN the history stack only contains the home screen before sign in and email validation is not required WHEN going back before FxA sign in THEN the user is returned to the home screen`() {
        backForwardList = listOf(
            URLs.APP_URL_HOME
        ).plus(URL_FLOW_NO_VALIDATION)
            .toFakeWebBackForwardListAsOriginalUrls()

        engineView.maybeGoBackBeforeFxaSignIn()

        verify(exactly = 1) { mockWebView.goBackOrForward(-URL_FLOW_NO_VALIDATION.size) }
    }

    @Test
    fun `GIVEN the history stack is not empty and email validation is not required WHEN going back before FxA sign in THEN the user is returned to the item before sign in`() {
        backForwardList = listOf(
            URLs.APP_URL_HOME,
            "https://google.com",
            "https://apple.com",
            "https://facebook.com"
        ).plus(URL_FLOW_NO_VALIDATION)
            .toFakeWebBackForwardListAsOriginalUrls()

        engineView.maybeGoBackBeforeFxaSignIn()

        verify(exactly = 1) { mockWebView.goBackOrForward(-URL_FLOW_NO_VALIDATION.size) }
    }

    @Test
    fun `GIVEN the history stack is not empty and email validation is required WHEN going back before FxA sign in THEN the user is returned to the item before sign in`() {
        backForwardList = listOf(
            URLs.APP_URL_HOME,
            "https://google.com",
            "https://apple.com",
            "https://facebook.com"
        ).plus(URL_FLOW_WITH_EMAIL_VALIDATION)
            .toFakeWebBackForwardListAsOriginalUrls()

        engineView.maybeGoBackBeforeFxaSignIn()

        verify(exactly = 1) { mockWebView.goBackOrForward(-URL_FLOW_WITH_EMAIL_VALIDATION.size) }
    }

    @Test
    fun `GIVEN the history stack visits firefox accounts, other pages, then sign in and email validation is required WHEN going back before FxA sign in THEN the user is returned to the item before sign in`() {
        backForwardList = listOf(
            URLs.APP_URL_HOME,
            "https://google.com",
            URLs.FIREFOX_ACCOUNTS,
            "https://apple.com"
        ).plus(URL_FLOW_NO_VALIDATION)
            .toFakeWebBackForwardListAsOriginalUrls()

        engineView.maybeGoBackBeforeFxaSignIn()

        verify(exactly = 1) { mockWebView.goBackOrForward(-URL_FLOW_NO_VALIDATION.size) }
    }
}

private fun List<String>.toFakeWebBackForwardListAsOriginalUrls(): FakeWebBackForwardList {
    return map { FakeWebHistoryItem(mockOriginalUrl = it) }
        .toFakeWebBackForwardList()
}
