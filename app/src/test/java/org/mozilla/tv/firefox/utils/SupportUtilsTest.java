package org.mozilla.tv.firefox.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.tv.firefox.BuildConfig;
import org.mozilla.tv.firefox.helpers.FirefoxRobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

@RunWith(FirefoxRobolectricTestRunner.class)
public class SupportUtilsTest {

    @Test
    public void cleanup() {
        // Other tests might get confused by our locale fiddling, so lets go back to the default:
        Locale.setDefault(Locale.ENGLISH);
    }

    /*
     * Super simple sumo URL test - it exists primarily to verify that we're setting the language
     * and page tags correctly. appVersion is null in tests, so we just test that there's a null there,
     * which doesn't seem too useful...
     */
    @Test
    public void getSumoURLForTopic() throws Exception {
        final String version = BuildConfig.VERSION_NAME;
        Locale.setDefault(Locale.GERMANY);
        assertEquals("https://support.mozilla.org/1/mobile/" + version + "/Android/de-DE/foobar",
                SupportUtils.getSumoURLForTopic(RuntimeEnvironment.application, "foobar"));

        Locale.setDefault(Locale.CANADA_FRENCH);
        assertEquals("https://support.mozilla.org/1/mobile/" + version + "/Android/fr-CA/foobar",
                SupportUtils.getSumoURLForTopic(RuntimeEnvironment.application, "foobar"));
    }

    /**
     * This is a pretty boring tests - it exists primarily to verify that we're actually setting
     * a langtag in the manfiesto URL.
     */
    @Test
    public void getManifestoURL() throws Exception {
        Locale.setDefault(Locale.UK);
        assertEquals("https://www.mozilla.org/en-GB/about/manifesto/",
                SupportUtils.getManifestoURL());

        Locale.setDefault(Locale.KOREA);
        assertEquals("https://www.mozilla.org/ko-KR/about/manifesto/",
                SupportUtils.getManifestoURL());
    }

}