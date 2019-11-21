/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.ext

import mozilla.components.browser.session.Session

object Js {
    const val CACHE_VAR = "_firefoxForFireTvPreviouslyFocusedElement"
    const val CACHE_JS = "var $CACHE_VAR = document.activeElement;"
    const val RESTORE_JS = "if ($CACHE_VAR) $CACHE_VAR.focus();"

    // This will only happen if YouTube is loading or navigation has broken
    const val NO_ELEMENT_FOCUSED = "document.activeElement === null"
    // This will only happen if YouTube is loading or navigation has broken
    const val BODY_ELEMENT_FOCUSED = "document.activeElement.tagName === \"BODY\""
    const val SIDEBAR_FOCUSED = "document.activeElement.parentElement.parentElement.id === 'guide-list'"

    const val PAUSE_VIDEO = "document.querySelectorAll('video').forEach(v => v.pause());"

    /**
     * Due to a bug in the Fire OS WebView (#2540, #2541), fullscreen mp4 videos will appear partially off screen if the
     * page, before entering fullscreen, is at a scroll position greater than 0. We can correct these videos by caching
     * the scroll position before the page was fullscreened and restoring this scroll position after entering fullscreen
     * mode.
     *
     * This fix does not work for all sites. In particular, it does not help YouTube desktop.
     *
     * This fix has a side effect of sometimes changing the page scroll position after the user leaves fullscreen.
     *
     * webm videos were working fine before this fix and this fix does not appear to impact them; I have not tried other
     * video types types because I don't know of any other supported ones.
     */
    object MP4TranslationWorkaround {
        private const val CACHED_SCROLL_POSITION = "_firefoxTV_cachedScrollPosition"

        /**
         * Caches the page scroll position before an element is fullscreened via a scroll position observer. Ideally,
         * we'd cache the scroll position when fullscreen is pressed but there are no appropriate hooks for that
         * ([Session.Observer.onFullScreenChanged] is too late) so we use a global scroll observer instead.
         */
        val OBSERVE_SCROLL_POSITION = """
var $CACHED_SCROLL_POSITION;
var _firefoxTV_lastTimeout;
var _firefoxTV_isScrollPositionObserverLoaded;
(function () {
    /* Ensure we only add one listener. */
    if (_firefoxTV_isScrollPositionObserverLoaded) { return; }
    _firefoxTV_isScrollPositionObserverLoaded = true;

    console.log('FFTV workaround - adding scroll event listener');
    window.addEventListener('scroll', (e) => {
        /* During the transition to fullscreen, the WebView may or may not change the scroll position to an incorrect
         * value so we add a short delay before caching. This creates a race condition & is imperfect: on a slow page,
         * it's possible the incorrect value will cache before we restore the fullscreen value so we'll restore the
         * incorrect position. Also, if the user is fast enough, they can open fullscreen before we cache the value.
         * This value was set through manual testing. I did not find better solutions. */
        if (typeof _firefoxTV_lastTimeout !== 'undefined') {
            /* We clear the timeout as an optimization: there are many scroll events so there might be performance
             * problems if we kept every event on the JS thread. */
            window.clearTimeout(_firefoxTV_lastTimeout);
        }

        var pendingScrollPosition = window.scrollY;
        _firefoxTV_lastTimeout = window.setTimeout(() => {
            $CACHED_SCROLL_POSITION = pendingScrollPosition;
            console.log('FFTV workaround - caching scroll position: ' + $CACHED_SCROLL_POSITION);
        }, 500);
    });
})();""".trimIndent()

        val UPDATE_FULLSCREEN_SCROLL_POSITION = """
var $CACHED_SCROLL_POSITION;
(function () {
    /* When fullscreen is pressed, the WebView sometimes updates the scroll position of the page. We delay a short
     * duration so that we can ensure the WebView scroll position update occurs before we write our final scroll
     * position. This creates a race condition & is imperfect: on a slow page, sometimes WebView will overwrite our
     * scroll position anyway. This delay duration was set through manual testing. I did not find better solutions.
     *
     * We use the cached scroll position from when this method is initially called so that we're less likely to use one
     * of WebView's incorrect positions (see OBSERVE_SCROLL_POSITION). */
    var nonFullscreenPageScrollPosition = $CACHED_SCROLL_POSITION;
    window.setTimeout(() => {
        console.log('FFTV workaround - scrolled fullscreen to non-fullscreen scroll position: ' + nonFullscreenPageScrollPosition);
        window.scrollTo(0, nonFullscreenPageScrollPosition);
    }, 1500);
})();""".trimIndent()
    }

    /**
     * This script will:
     * - Add playback state change listeners to all <video>s in the DOM; it uses a mutation
     *   observer to attach listeners to new <video> nodes as well
     * - On playback state change, notify Java about the current playback state
     * - Prevent this script from being injected more than once per page
     * - As a workaround to a WebView bug, modify certain videos' CSS tags.
     *
     * Note that `//` style comments are not supported in `evalJS`.
     *
     * Development tips:
     * - This script was written using Typescript with Visual Studio Code: it may be easier to modify
     *   it by copy-pasting it back-and-forth.
     * - Iterating on the Fire TV is slow: you can speed it up by making this a WebExtension content
     *   script and testing on desktop
     * - For a list of HTMLMediaElement (i.e. video) events, like 'ratechange', see the w3c's HTML5 video
     *   page: https://www.w3.org/2010/05/video/mediaevents.html
     */
    val JS_OBSERVE_PLAYBACK_STATE = """
var _firefoxTV_playbackStateObserverJava;
var _firefoxTV_isPlaybackStateObserverLoaded;
(function () {
    /* seeking will send "pause, play" and so is covered here. */
    const PLAYBACK_STATE_CHANGE_EVENTS = ['play', 'pause', 'ratechange'];
    const MILLIS_BETWEEN_PLAYBACK_STATE_SYNC_BY_TIME = 1000 * 10 /* seconds */;

    const javaInterface = _firefoxTV_playbackStateObserverJava;
    if (!javaInterface) {
        console.error('Cannot sync playback state to Java: JavascriptInterface is not found.');
    }

    const videosWithListeners = new Set();

    let playbackStateSyncIntervalID;

    function onDOMChangedForVideos() {
        addPlaybackStateListeners();
        removeNegativeTranslationCSSCentering();
        syncPlaybackState();
    }

    function addPlaybackStateListeners() {
        document.querySelectorAll('video').forEach(videoElement => {
            if (videosWithListeners.has(videoElement)) { return; }
            videosWithListeners.add(videoElement);

            PLAYBACK_STATE_CHANGE_EVENTS.forEach(event => {
                videoElement.addEventListener(event, syncPlaybackState);
            });
        });
    }

    function syncPlaybackState() {
        let isVideoPresent;
        let isPlaying;
        let positionSeconds;
        let playbackRate; /* 0.5, 1, etc. */
        const maybeTargetVideo = getPlayingVideoOrFirstInDOMOrNull();
        if (maybeTargetVideo) {
            isVideoPresent = true;
            isPlaying = !maybeTargetVideo.paused;
            positionSeconds = maybeTargetVideo.currentTime;
            playbackRate = maybeTargetVideo.playbackRate;
        } else {
            isVideoPresent = false;
            isPlaying = false;
            positionSeconds = null;
            playbackRate = null;
        }

        schedulePlaybackStateSyncInterval(isPlaying);

        javaInterface.syncPlaybackState(isVideoPresent, isPlaying, positionSeconds, playbackRate);
    }

    /*
     * When a video has left=50% & transform=translateX(-50%) properties (a common CSS centering
     * trick), videos are pushed outside their containers: if we see similar properties, we remove
     * them as a workaround. #2526
     */
    function removeNegativeTranslationCSSCentering() {
        const videoElements = document.querySelectorAll('video');

        videoElements.forEach((vid) => {
            const style = window.getComputedStyle(vid);
            const left = parseFloat(style.left); /* style.left is e.g. "320px" */

            /* The matrix's 4th column, 1st row (m41) represents the X translation.
             * See: https://stackoverflow.com/a/42267468 */
            const translateX = new DOMMatrix(style.transform).m41;

            const translateNegatesLeft = left + translateX === 0;
            if (translateNegatesLeft) {
                vid.style.left = '0px';
                vid.style.transform = 'translate(0px, 0px)';
                console.log('FFTV workaround - removed CSS transform "translate"');
            }
        });
    }

    /**
     * When a video is playing, schedules a function to repeatedly sync the playback state;
     * cancels it when there is no video playing.
     *
     * Java and JavaScript increment the current playback position independently and run the risk of
     * getting out of sync (e.g. upon buffering). We could try to handle the buffering case specifically
     * but its state is difficult to identify with and syncing periodically is a better general solution.
     * We don't sync with the video's 'timeupdate' event because it's called very frequently and could
     * detract from performance.
     */
    function schedulePlaybackStateSyncInterval(isVideoPlaying) {
        if (isVideoPlaying && !playbackStateSyncIntervalID) {
            playbackStateSyncIntervalID = setInterval(syncPlaybackState,
                MILLIS_BETWEEN_PLAYBACK_STATE_SYNC_BY_TIME);

        } else if (!isVideoPlaying && playbackStateSyncIntervalID) {
            clearInterval(playbackStateSyncIntervalID);
            playbackStateSyncIntervalID = null;
        }
    }

    function getPlayingVideoOrFirstInDOMOrNull() {
        const maybePlayingVideo = Array.from(document.querySelectorAll('video')).find(video => !video.paused);
        if (maybePlayingVideo) { return maybePlayingVideo; }

        /* If there are no playing videos, just return the first one. */
        return document.querySelector('video');
    }

    function nodeContainsVideo(node) {
        return node.nodeName.toLowerCase() === 'video' ||
                ((node instanceof Element) && !!node.querySelector('video'));
    }

    const documentChangedObserver = new MutationObserver(mutationList => {
        const wasVideoAdded = mutationList.some(mutation => {
            return mutation.type === 'childList' &&
                    (Array.from(mutation.addedNodes).some(nodeContainsVideo) ||
                            Array.from(mutation.removedNodes).some(nodeContainsVideo));
        });

        if (wasVideoAdded) {
            /* This may traverse the whole DOM so let's only call it if it's necessary. */
            onDOMChangedForVideos();
        }
    });

    /* Sometimes the script is evaluated more than once per page:
     * only inject code with side effects once. */
    if (!_firefoxTV_isPlaybackStateObserverLoaded) {
        _firefoxTV_isPlaybackStateObserverLoaded = true;

        documentChangedObserver.observe(document, {subtree: true, childList: true});

        /* The DOM is changed from blank to filled for the initial page load.
         * While the function name assumes videos are present, checking for
         * videos is as expensive as calling the function so we just call it. */
        onDOMChangedForVideos();
    }
})();
""".trimIndent()

    /**
     * The soft keyboard sends `tab` events when `submit` is clicked. This adds an extra step
     * to many user flows (for example, on google.com, people expect a search to kick off after
     * clicking submit, but it doesn't). This script forces a `submit` call after tab events are
     * received.
     *
     * This behavior is added to all <input> elements any time new elements are added to the DOM.
     */
    val ADD_SUBMIT_LISTENER_TO_ALL_INPUTS = """
    (function () {
        if (typeof _firefoxTV_inputAddedObserver !== 'undefined') return;
    
        function inputSubmitListener(event) {
            console.log("SEVTEST: keypress detected");
            console.log(event)
            if (event && event.key === 'Tab') {
                // Get the nearest <form> ancestor
                var formWrapper = Array.from(event.path).find((it) => it.tagName === 'FORM')
                if (formWrapper) formWrapper.submit()
            };
        };
    
        function attachListeners() {
            console.log("SEVTEST: listeners attached");
            Array.from(document.getElementsByTagName('input'))
                .forEach((input) => {
                    input.removeEventListener("keydown", inputSubmitListener);
                    input.addEventListener("keydown", inputSubmitListener);
                });
        };
    
        function nodeContainsInput(node) {
            return node.nodeName.toLowerCase() === 'input' ||
                ((node instanceof Element) && !!node.querySelector('input'));
        }
    
        _firefoxTV_inputAddedObserver = new MutationObserver(mutationList => {
            const wasInputAdded = mutationList.some(mutation => {
                return mutation.type === 'childList' &&
                    (Array.from(mutation.addedNodes).some(nodeContainsInput));
            });
            
            console.log("SEVTEST: mutation. wasInputAdded: " + wasInputAdded);
    
            if (wasInputAdded) {
                /* This may traverse the whole DOM so let's only call it if it's necessary. */
                attachListeners();
            }
        });
        
        _firefoxTV_inputAddedObserver.observe(document, {subtree: true, childList: true});
        
        attachListeners();
    })();
        """.trimIndent()
}
