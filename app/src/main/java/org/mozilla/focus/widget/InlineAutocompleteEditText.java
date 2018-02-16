/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.UrlUtils;

public class InlineAutocompleteEditText extends android.support.v7.widget.AppCompatEditText {
    public interface OnCommitListener {
        void onCommit();
    }

    public interface OnFilterListener {
        void onFilter(String searchText, InlineAutocompleteEditText view);
    }

    public interface OnBackPressedListener {
        void onBackPressed();
    }

    public static class AutocompleteResult {
        public static AutocompleteResult emptyResult() {
            return new AutocompleteResult("", "", 0);
        }

        private final String text;
        private final String source;
        private final int totalItems;

        public AutocompleteResult(@NonNull String text, @NonNull String source, int totalItems) {
            this.text = text;
            this.source = source;
            this.totalItems = totalItems;
        }

        public boolean isEmpty() {
            return text.isEmpty();
        }

        public String getText() {
            return text;
        }

        public String getSource() {
            return source;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getLength() {
            return text.length();
        }

        public boolean startsWith(String text) {
            return this.text.startsWith(text);
        }
    }

    private static final String LOGTAG = "GeckoToolbarEditText";
    private static final NoCopySpan AUTOCOMPLETE_SPAN = new NoCopySpan.Concrete();

    private final Context mContext;

    private OnCommitListener mCommitListener;
    private OnFilterListener mFilterListener;
    private OnBackPressedListener mOnBackPressedListener;

    // The previous autocomplete result returned to us
    private AutocompleteResult mAutoCompleteResult = AutocompleteResult.emptyResult();
    // If text change is due to us setting autocomplete
    private boolean mSettingAutoComplete;
    // Spans used for marking the autocomplete text
    private Object[] mAutoCompleteSpans;
    // Do not process autocomplete result
    private boolean mDiscardAutoCompleteResult;

    /**
     * True if the current key press is text entry from the Fire TV Remote app (from Google Play) or
     * a "Clear" press on the soft keyboard, false otherwise. We include the latter due to
     * implementation necessity.
     *
     * fwiw, there are a few ways I've found to tell if the remote app is in use:
     * - {@link #onKeyPreIme(int, KeyEvent)} is not called when entering text input with the remote
     * app
     * - You can {@link InputDevice#getDeviceIds()} but it's only useful if you have a key event to
     * associate each press with.
     * - commitText("", ...) is called, followed by deleteSurroundingText (these two calls happen
     * when Clear is selected on the soft keyboard too, but that's the only other event I've found)
     * and then commitText is called with a String argument with more than 1 character (only if the
     * user has entered 1 character). No other input device I've found does this. This call pattern
     * doesn't happen when backspace is pressed on the remote app.
     */
    private boolean isKeyFromRemoteAppOrSoftKeyboardClear;

    public InlineAutocompleteEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public void setOnCommitListener(OnCommitListener listener) {
        mCommitListener = listener;
    }

    public void setOnFilterListener(OnFilterListener listener) {
        mFilterListener = listener;
    }

    public void setOnBackPressedListener(OnBackPressedListener listener) {
        mOnBackPressedListener = listener;
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setOnKeyListener(new KeyListener());
        addTextChangedListener(new TextChangeListener());
    }

    @Override
    public void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            resetAutocompleteState();
            return;
        }

        removeAutocomplete(getText());

        final InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        try {
            imm.restartInput(this);
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
        } catch (NullPointerException e) {
            Log.e(LOGTAG, "InputMethodManagerService, why are you throwing"
                    + " a NullPointerException? See bug 782096", e);
        }
    }

    @Override
    public void setText(final CharSequence text, final TextView.BufferType type) {
        super.setText(text, type);

        // Any autocomplete text would have been overwritten, so reset our autocomplete states.
        resetAutocompleteState();
    }

    @Override
    public void sendAccessibilityEventUnchecked(AccessibilityEvent event) {
        // We need to bypass the isShown() check in the default implementation
        // for TYPE_VIEW_TEXT_SELECTION_CHANGED events so that accessibility
        // services could detect a url change.
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED &&
                getParent() != null && !isShown()) {
            onInitializeAccessibilityEvent(event);
            dispatchPopulateAccessibilityEvent(event);
            getParent().requestSendAccessibilityEvent(this, event);
        } else {
            super.sendAccessibilityEventUnchecked(event);
        }
    }

    /**
     * Mark the start of autocomplete changes so our text change
     * listener does not react to changes in autocomplete text
     */
    private void beginSettingAutocomplete() {
        beginBatchEdit();
        mSettingAutoComplete = true;
    }

    /**
     * Mark the end of autocomplete changes
     */
    private void endSettingAutocomplete() {
        mSettingAutoComplete = false;
        endBatchEdit();
    }

    /**
     * Reset autocomplete states to their initial values
     */
    private void resetAutocompleteState() {
        mAutoCompleteSpans = new Object[] {
                // Span to mark the autocomplete text
                AUTOCOMPLETE_SPAN,
                // Span to change the autocomplete text color
                new BackgroundColorSpan(
                        ContextCompat.getColor(getContext(), R.color.colorAccent))
        };

        mAutoCompleteResult = AutocompleteResult.emptyResult();

        // Show the cursor.
        setCursorVisible(true);
    }

    /**
     * Get the portion of text that is not marked as autocomplete text.
     *
     * @param text Current text content that may include autocomplete text
     */
    private static String getNonAutocompleteText(final Editable text) {
        final int start = text.getSpanStart(AUTOCOMPLETE_SPAN);
        if (start < 0) {
            // No autocomplete text; return the whole string.
            return text.toString();
        }

        // Only return the portion that's not autocomplete text
        return TextUtils.substring(text, 0, start);
    }

    /**
     * Remove any autocomplete text
     *
     * @param text Current text content that may include autocomplete text
     */
    private boolean removeAutocomplete(final Editable text) {
        final int start = text.getSpanStart(AUTOCOMPLETE_SPAN);
        if (start < 0) {
            // No autocomplete text
            return false;
        }

        beginSettingAutocomplete();

        // When we call delete() here, the autocomplete spans we set are removed as well.
        text.delete(start, text.length());

        // Reshow the cursor.
        setCursorVisible(true);

        endSettingAutocomplete();
        return true;
    }

    /**
     * Convert any autocomplete text to regular text
     *
     * @param text Current text content that may include autocomplete text
     */
    private boolean commitAutocomplete(final Editable text) {
        final int start = text.getSpanStart(AUTOCOMPLETE_SPAN);
        if (start < 0) {
            // No autocomplete text
            return false;
        }

        beginSettingAutocomplete();

        // Remove all spans here to convert from autocomplete text to regular text
        for (final Object span : mAutoCompleteSpans) {
            text.removeSpan(span);
        }

        // Reshow the cursor.
        setCursorVisible(true);

        endSettingAutocomplete();

        // Filter on the new text
        if (mFilterListener != null) {
            mFilterListener.onFilter(text.toString(), null);
        }
        return true;
    }

    /**
     * Add autocomplete text based on the result URI.
     *
     * @param result Result URI to be turned into autocomplete text
     */
    public void onAutocomplete(final AutocompleteResult result) {
        // If mDiscardAutoCompleteResult is true, we temporarily disabled
        // autocomplete (due to backspacing, etc.) and we should bail early.
        //
        // We disable autocomplete when the Fire TV remote app (from the Play Store) is entering
        // text input because autocomplete would be time consuming to implement: for full reasoning,
        // see https://github.com/mozilla-mobile/firefox-tv/issues/276#issuecomment-365801269
        // For a soft keyboard "Clear", it doesn't matter if we disable autocomplete.
        if (mDiscardAutoCompleteResult || isKeyFromRemoteAppOrSoftKeyboardClear) {
            return;
        }

        if (!isEnabled() || result == null || result.isEmpty()) {
            mAutoCompleteResult = AutocompleteResult.emptyResult();
            return;
        }

        final Editable text = getText();
        final int textLength = text.length();
        final int resultLength = result.getLength();
        final int autoCompleteStart = text.getSpanStart(AUTOCOMPLETE_SPAN);
        mAutoCompleteResult = result;

        if (autoCompleteStart > -1) {
            // Autocomplete text already exists; we should replace existing autocomplete text.

            // If the result and the current text don't have the same prefixes,
            // the result is stale and we should wait for the another result to come in.
            if (!TextUtils.regionMatches(result.getText(), 0, text, 0, autoCompleteStart)) {
                return;
            }

            beginSettingAutocomplete();

            // Replace the existing autocomplete text with new one.
            // replace() preserves the autocomplete spans that we set before.
            text.replace(autoCompleteStart, textLength, result.getText(), autoCompleteStart, resultLength);

            // Reshow the cursor if there is no longer any autocomplete text.
            if (autoCompleteStart == resultLength) {
                setCursorVisible(true);
            }

            endSettingAutocomplete();

        } else {
            // No autocomplete text yet; we should add autocomplete text

            // If the result prefix doesn't match the current text,
            // the result is stale and we should wait for the another result to come in.
            if (resultLength <= textLength ||
                    !TextUtils.regionMatches(result.getText(), 0, text, 0, textLength)) {
                return;
            }

            final Object[] spans = text.getSpans(textLength, textLength, Object.class);
            final int[] spanStarts = new int[spans.length];
            final int[] spanEnds = new int[spans.length];
            final int[] spanFlags = new int[spans.length];

            // Save selection/composing span bounds so we can restore them later.
            for (int i = 0; i < spans.length; i++) {
                final Object span = spans[i];
                final int spanFlag = text.getSpanFlags(span);

                // We don't care about spans that are not selection or composing spans.
                // For those spans, spanFlag[i] will be 0 and we don't restore them.
                if ((spanFlag & Spanned.SPAN_COMPOSING) == 0 &&
                        (span != Selection.SELECTION_START) &&
                        (span != Selection.SELECTION_END)) {
                    continue;
                }

                spanStarts[i] = text.getSpanStart(span);
                spanEnds[i] = text.getSpanEnd(span);
                spanFlags[i] = spanFlag;
            }

            beginSettingAutocomplete();

            // First add trailing text.
            text.append(result.getText(), textLength, resultLength);

            // Restore selection/composing spans.
            for (int i = 0; i < spans.length; i++) {
                final int spanFlag = spanFlags[i];
                if (spanFlag == 0) {
                    // Skip if the span was ignored before.
                    continue;
                }
                text.setSpan(spans[i], spanStarts[i], spanEnds[i], spanFlag);
            }

            // Mark added text as autocomplete text.
            for (final Object span : mAutoCompleteSpans) {
                text.setSpan(span, textLength, resultLength, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Hide the cursor.
            setCursorVisible(false);

            // Make sure the autocomplete text is visible. If the autocomplete text is too
            // long, it would appear the cursor will be scrolled out of view. However, this
            // is not the case in practice, because EditText still makes sure the cursor is
            // still in view.
            bringPointIntoView(resultLength);

            endSettingAutocomplete();
        }

        announceForAccessibility(text.toString());
    }

    @NonNull
    public AutocompleteResult getLastAutocompleteResult() {
        return mAutoCompleteResult;
    }

    private static boolean hasCompositionString(Editable content) {
        Object[] spans = content.getSpans(0, content.length(), Object.class);

        if (spans != null) {
            for (Object span : spans) {
                if ((content.getSpanFlags(span) & Spanned.SPAN_COMPOSING) != 0) {
                    // Found composition string.
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Code to handle deleting autocomplete first when backspacing.
     * If there is no autocomplete text, both removeAutocomplete() and commitAutocomplete()
     * are no-ops and return false. Therefore we can use them here without checking explicitly
     * if we have autocomplete text or not.
     *
     * Also turns off text prediction for private mode tabs.
     */
    @Override
    public InputConnection onCreateInputConnection(final EditorInfo outAttrs) {
        final InputConnection ic = super.onCreateInputConnection(outAttrs);
        if (ic == null) {
            return null;
        }

        return new InputConnectionWrapper(ic, false) {
            @Override
            public boolean deleteSurroundingText(final int beforeLength, final int afterLength) {
                removeAutocomplete(getText());
                return super.deleteSurroundingText(beforeLength, afterLength);
            }

            private boolean removeAutocompleteOnComposing(final CharSequence text) {
                final Editable editable = getText();
                final int composingStart = BaseInputConnection.getComposingSpanStart(editable);
                final int composingEnd = BaseInputConnection.getComposingSpanEnd(editable);
                // We only delete the autocomplete text when the user is backspacing,
                // i.e. when the composing text is getting shorter.
                if (composingStart >= 0 &&
                        composingEnd >= 0 &&
                        (composingEnd - composingStart) > text.length() &&
                        removeAutocomplete(editable)) {
                    // Make the IME aware that we interrupted the setComposingText call,
                    // by having finishComposingText() send change notifications to the IME.
                    finishComposingText();
                    setComposingRegion(composingStart, composingEnd);
                    return true;
                }
                return false;
            }

            @Override
            public boolean commitText(CharSequence text, int newCursorPosition) {
                if (isCommitTextFromRemoteAppOrSoftKeyboardClear(text)) {
                    setIsKeyFromRemoteAppOrSoftKeyboardClear(true);
                }

                if (removeAutocompleteOnComposing(text)) {
                    return false;
                }
                return super.commitText(text, newCursorPosition);
            }

            private boolean isCommitTextFromRemoteAppOrSoftKeyboardClear(final CharSequence text) {
                // Two events call this with text as the empty string: Clear from the soft keyboard
                // and input from the remote app (which calls this with the empty string to
                // clear the input, then calls this with the full input). Since we don't want
                // autocomplete for the remote app and autocomplete is unnecessary when all the text
                // is cleared, we compare against the empty string to see if this could be the
                // remote app.
                return "".equals(text);
            }

            @Override
            public boolean setComposingText(final CharSequence text, final int newCursorPosition) {
                if (removeAutocompleteOnComposing(text)) {
                    return false;
                }
                return super.setComposingText(text, newCursorPosition);
            }
        };
    }

    private class TextChangeListener implements TextWatcher {
        private int textLengthBeforeChange;

        @Override
        public void afterTextChanged(final Editable editable) {
            if (!isEnabled() || mSettingAutoComplete) {
                return;
            }

            final String text = getNonAutocompleteText(editable);
            final int textLength = text.length();
            boolean doAutocomplete = true;

            if (UrlUtils.isSearchQuery(text) ||
                    isKeyFromRemoteAppOrSoftKeyboardClear) { // See var use in onAutocomplete.
                doAutocomplete = false;
            } else if (textLength == textLengthBeforeChange - 1 || textLength == 0) {
                // If you're hitting backspace (the string is getting smaller), don't autocomplete
                doAutocomplete = false;
            }

            // If we are not autocompleting, we set mDiscardAutoCompleteResult to true
            // to discard any autocomplete results that are in-flight, and vice versa.
            mDiscardAutoCompleteResult = !doAutocomplete;

            if (doAutocomplete && mAutoCompleteResult.startsWith(text)) {
                // If this text already matches our autocomplete text, autocomplete likely
                // won't change. Just reuse the old autocomplete value.
                onAutocomplete(mAutoCompleteResult);
                doAutocomplete = false;
            } else {
                // Otherwise, remove the old autocomplete text
                // until any new autocomplete text gets added.
                removeAutocomplete(editable);
            }

            if (mFilterListener != null) {
                mFilterListener.onFilter(text, doAutocomplete ? InlineAutocompleteEditText.this : null);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            textLengthBeforeChange = s.length();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // do nothing
        }
    }

    private class KeyListener implements View.OnKeyListener {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return true;
                }

                if (mCommitListener != null) {
                    mCommitListener.onCommit();
                }

                return true;
            }

            if ((keyCode == KeyEvent.KEYCODE_DEL ||
                    (keyCode == KeyEvent.KEYCODE_FORWARD_DEL)) &&
                    removeAutocomplete(getText())) {
                // Delete autocomplete text when backspacing or forward deleting.
                return true;
            }

            return false;
        }
    }

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // The remote app doesn't fire this key event when entering characters into the url bar so
        // it must not be the remote app. Note that the remote app does fire this for focusing the
        // url bar though.
        setIsKeyFromRemoteAppOrSoftKeyboardClear(false);

        if (isAttachedToWindow()) {
            // We only want to process one event per tap
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                // If the edit text has a composition string, don't submit the text yet.
                // ENTER is needed to commit the composition string.
                final Editable content = getText();
                if (!hasCompositionString(content)) {
                    if (mCommitListener != null) {
                        mCommitListener.onCommit();
                    }

                    return true;
                }
            }

            if (keyCode == KeyEvent.KEYCODE_BACK) {
                removeAutocomplete(getText());
                if (mOnBackPressedListener != null) {
                    mOnBackPressedListener.onBackPressed();
                }
                return false;
            }

            return false;
        }

        return false;
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        if (isAttachedToWindow()) {
            // The user has repositioned the cursor somewhere. We need to adjust
            // the autocomplete text depending on where the new cursor is.

            final Editable text = getText();
            final int start = text.getSpanStart(AUTOCOMPLETE_SPAN);

            if (mSettingAutoComplete || start < 0 || (start == selStart && start == selEnd)) {
                // Do not commit autocomplete text if there is no autocomplete text
                // or if selection is still at start of autocomplete text
                return;
            }

            if (selStart <= start && selEnd <= start) {
                // The cursor is in user-typed text; remove any autocomplete text.
                removeAutocomplete(text);
            } else {
                // The cursor is in the autocomplete text; commit it so it becomes regular text.
                commitAutocomplete(text);
            }
        }

        super.onSelectionChanged(selStart, selEnd);
    }

    private void setIsKeyFromRemoteAppOrSoftKeyboardClear(final boolean isKeyFromRemoteApp) {
        isKeyFromRemoteAppOrSoftKeyboardClear = isKeyFromRemoteApp;
        if (isKeyFromRemoteApp) {
            resetAutocompleteState(); // We want a blank autocomplete result for telemetry.
            removeAutocomplete(getText()); // Perhaps not strictly necessary.
        }
    }
}
