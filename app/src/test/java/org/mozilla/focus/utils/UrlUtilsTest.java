package org.mozilla.focus.utils;

import android.annotation.SuppressLint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.search.SearchEngineManager;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UrlUtilsTest {
    @Test
    public void isValidSearchQueryUrl() {
        assertTrue(UrlUtils.INSTANCE.isValidSearchQueryUrl("https://example.com/search/?q=%s"));
        assertTrue(UrlUtils.INSTANCE.isValidSearchQueryUrl("http://example.com/search/?q=%s"));
        assertTrue(UrlUtils.INSTANCE.isValidSearchQueryUrl("http-test-site.com/search/?q=%s"));
        assertFalse(UrlUtils.INSTANCE.isValidSearchQueryUrl("httpss://example.com/search/?q=%s"));

        assertTrue(UrlUtils.INSTANCE.isValidSearchQueryUrl("example.com/search/?q=%s"));
        assertTrue(UrlUtils.INSTANCE.isValidSearchQueryUrl(" example.com/search/?q=%s "));

        assertFalse(UrlUtils.INSTANCE.isValidSearchQueryUrl("htps://example.com/search/?q=%s"));

        assertFalse(UrlUtils.INSTANCE.isValidSearchQueryUrl(" example.com/search/?q= "));
    }

    @Test
    public void urlsMatchExceptForTrailingSlash() throws Exception {
        assertTrue(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.mozilla.org", "http://www.mozilla.org"));
        assertTrue(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.mozilla.org/", "http://www.mozilla.org"));
        assertTrue(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.mozilla.org", "http://www.mozilla.org/"));

        assertFalse(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://mozilla.org", "http://www.mozilla.org"));
        assertFalse(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.mozilla.org/", "http://mozilla.org"));

        assertFalse(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.mozilla.org", "https://www.mozilla.org"));
        assertFalse(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("https://www.mozilla.org", "http://www.mozilla.org"));

        // Same length of domain, but otherwise different:
        assertFalse(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.allizom.org", "http://www.mozilla.org"));
        assertFalse(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.allizom.org/", "http://www.mozilla.org"));
        assertFalse(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.allizom.org", "http://www.mozilla.org/"));

        // Check upper/lower case is OK:
        assertTrue(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.MOZILLA.org", "http://www.mozilla.org"));
        assertTrue(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.MOZILLA.org/", "http://www.mozilla.org"));
        assertTrue(UrlUtils.INSTANCE.urlsMatchExceptForTrailingSlash("http://www.MOZILLA.org", "http://www.mozilla.org/"));
    }

    @Test
    public void isPermittedResourceProtocol() {
        assertFalse(UrlUtils.INSTANCE.isPermittedResourceProtocol(""));
        assertFalse(UrlUtils.INSTANCE.isPermittedResourceProtocol(null));

        assertTrue(UrlUtils.INSTANCE.isPermittedResourceProtocol("http"));
        assertTrue(UrlUtils.INSTANCE.isPermittedResourceProtocol("https"));

        assertTrue(UrlUtils.INSTANCE.isPermittedResourceProtocol("data"));
        assertTrue(UrlUtils.INSTANCE.isPermittedResourceProtocol("file"));

        assertFalse(UrlUtils.INSTANCE.isPermittedResourceProtocol("nielsenwebid"));
    }

    @Test
    public void isPermittedProtocol() {
        assertFalse(UrlUtils.INSTANCE.isSupportedProtocol(""));
        assertFalse(UrlUtils.INSTANCE.isSupportedProtocol(null));

        assertTrue(UrlUtils.INSTANCE.isSupportedProtocol("http"));
        assertTrue(UrlUtils.INSTANCE.isSupportedProtocol("https"));
        assertTrue(UrlUtils.INSTANCE.isSupportedProtocol("error"));
        assertTrue(UrlUtils.INSTANCE.isSupportedProtocol("data"));

        assertFalse(UrlUtils.INSTANCE.isSupportedProtocol("market"));
    }

    @Test
    public void testIsUrl() {
        assertTrue(UrlUtils.INSTANCE.isUrl("http://www.mozilla.org"));
        assertTrue(UrlUtils.INSTANCE.isUrl("https://www.mozilla.org"));
        assertTrue(UrlUtils.INSTANCE.isUrl("https://www.mozilla.org "));
        assertTrue(UrlUtils.INSTANCE.isUrl(" https://www.mozilla.org"));
        assertTrue(UrlUtils.INSTANCE.isUrl(" https://www.mozilla.org "));
        assertTrue(UrlUtils.INSTANCE.isUrl("https://www.mozilla.org/en-US/internet-health/"));
        assertTrue(UrlUtils.INSTANCE.isUrl("file:///mnt/sdcard/"));
        assertTrue(UrlUtils.INSTANCE.isUrl("mozilla.org"));

        assertFalse(UrlUtils.INSTANCE.isUrl("Hello World"));
        assertFalse(UrlUtils.INSTANCE.isUrl("Mozilla"));
    }

    @Test
    public void testNormalize() {
        assertEquals("http://www.mozilla.org", UrlUtils.INSTANCE.normalize("http://www.mozilla.org"));
        assertEquals("https://www.mozilla.org", UrlUtils.INSTANCE.normalize("https://www.mozilla.org"));
        assertEquals("https://www.mozilla.org/en-US/internet-health/", UrlUtils.INSTANCE.normalize("https://www.mozilla.org/en-US/internet-health/"));
        assertEquals("file:///mnt/sdcard/", UrlUtils.INSTANCE.normalize("file:///mnt/sdcard/"));

        assertEquals("http://mozilla.org", UrlUtils.INSTANCE.normalize("mozilla.org"));
        assertEquals("http://mozilla.org", UrlUtils.INSTANCE.normalize("http://mozilla.org "));
        assertEquals("http://mozilla.org", UrlUtils.INSTANCE.normalize(" http://mozilla.org "));
        assertEquals("http://mozilla.org", UrlUtils.INSTANCE.normalize(" http://mozilla.org"));
        assertEquals("http://localhost", UrlUtils.INSTANCE.normalize("localhost"));
    }

    @Test
    public void testIsSearchQuery() {
        assertTrue(UrlUtils.INSTANCE.isSearchQuery("hello world"));

        assertFalse(UrlUtils.INSTANCE.isSearchQuery("mozilla.org"));
        assertFalse(UrlUtils.INSTANCE.isSearchQuery("mozilla"));
    }

    @Test public void testCreateSearchUrl() {
        assertCreatedUrlContainsBase("dogs are cool", "https://www.google.com/search?q=dogs%20are%20cool");
        assertCreatedUrlContainsBase("how can mirrors be real if our eyes arent real?",
                "https://www.google.com/search?q=how%20can%20mirrors%20be%20real%20if%20our%20eyes%20arent%20real");
    }

    private void assertCreatedUrlContainsBase(String searchTerm, String base) {
        String searchString = UrlUtils.INSTANCE.createSearchUrl(RuntimeEnvironment.application, searchTerm);
        assertTrue("\"" + searchString + "\" does not contain \"" + base + "\"",
                searchString.contains(base));
    }

    @Test
    @SuppressLint("AuthLeak")
    public void testStripUserInfo() {
        assertEquals("", UrlUtils.INSTANCE.stripUserInfo(null));
        assertEquals("", UrlUtils.INSTANCE.stripUserInfo(""));

        assertEquals("https://www.mozilla.org", UrlUtils.INSTANCE.stripUserInfo("https://user:password@www.mozilla.org"));
        assertEquals("https://www.mozilla.org", UrlUtils.INSTANCE.stripUserInfo("https://user@www.mozilla.org"));

        assertEquals("user@mozilla.org", UrlUtils.INSTANCE.stripUserInfo("user@mozilla.org"));

        assertEquals("ftp://mozilla.org", UrlUtils.INSTANCE.stripUserInfo("ftp://user:password@mozilla.org"));

        assertEquals("öäü102ß", UrlUtils.INSTANCE.stripUserInfo("öäü102ß"));

        String badUri = "https://user:password@www.i/have/percentage/signs/%.org%";
        assertEquals(badUri, UrlUtils.INSTANCE.stripUserInfo(badUri));
        badUri = "://user:password@i/have/no/scheme.org";
        assertEquals(badUri, UrlUtils.INSTANCE.stripUserInfo(badUri));
    }

    @Test
    public void isInternalErrorURL() {
        assertTrue(UrlUtils.INSTANCE.isInternalErrorURL("data:text/html;charset=utf-8;base64,"));

        assertFalse(UrlUtils.INSTANCE.isInternalErrorURL("http://www.mozilla.org"));
        assertFalse(UrlUtils.INSTANCE.isInternalErrorURL("https://www.mozilla.org/en-us/about"));
        assertFalse(UrlUtils.INSTANCE.isInternalErrorURL("www.mozilla.org"));
        assertFalse(UrlUtils.INSTANCE.isInternalErrorURL("error:-8"));
        assertFalse(UrlUtils.INSTANCE.isInternalErrorURL("hello world"));
    }

    @Test
    public void isHttpOrHttpsUrl() {
        assertFalse(UrlUtils.INSTANCE.isHttpOrHttps(null));
        assertFalse(UrlUtils.INSTANCE.isHttpOrHttps(""));
        assertFalse(UrlUtils.INSTANCE.isHttpOrHttps("     "));
        assertFalse(UrlUtils.INSTANCE.isHttpOrHttps("mozilla.org"));
        assertFalse(UrlUtils.INSTANCE.isHttpOrHttps("httpstrf://example.org"));

        assertTrue(UrlUtils.INSTANCE.isHttpOrHttps("https://www.mozilla.org"));
        assertTrue(UrlUtils.INSTANCE.isHttpOrHttps("http://example.org"));
        assertTrue(UrlUtils.INSTANCE.isHttpOrHttps("http://192.168.0.1"));
    }

    @Test
    public void testStripCommonSubdomains() {
        assertEquals("mozilla.org", UrlUtils.INSTANCE.stripCommonSubdomains("mozilla.org"));
        assertEquals("mozilla.org", UrlUtils.INSTANCE.stripCommonSubdomains("www.mozilla.org"));
        assertEquals("mozilla.org", UrlUtils.INSTANCE.stripCommonSubdomains("m.mozilla.org"));
        assertEquals("mozilla.org", UrlUtils.INSTANCE.stripCommonSubdomains("mobile.mozilla.org"));
        assertEquals("random.mozilla.org", UrlUtils.INSTANCE.stripCommonSubdomains("random.mozilla.org"));
        assertEquals(null, UrlUtils.INSTANCE.stripCommonSubdomains(null));
    }
}
