/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.CheckBox
import android.widget.Checkable
import android.widget.ImageButton

private val checkedStateSet = intArrayOf(android.R.attr.state_checked)

/**
 * ImageButton that implements Checkable.
 *
 * NB: We need a checkable button with a centered drawable for the overlay.
 * CheckBox uses a non-centered drawable, and we can't use ToggleButton
 * because it uses android:background to set the image, which we need for button state,
 * so we make a custom button.
 *
 * We need to override onCreateDrawableState (see CompoundButton source) to add
 * the checked state to the attrs.
 */
class CheckableImageButton @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : ImageButton(context, attrs, defStyle), Checkable {

    private var internalIsChecked = false
        set(value) {
            field = value
            refreshDrawableState()
        }

    override fun isChecked() = internalIsChecked

    override fun toggle() {
        isChecked = !internalIsChecked
    }

    override fun setChecked(checked: Boolean) {
        internalIsChecked = checked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        if (internalIsChecked) {
            View.mergeDrawableStates(drawableState, checkedStateSet)
        }
        return drawableState
    }

    override fun performClick(): Boolean {
        toggle()
        return super.performClick()
    }

    // The system provides special accessibility announcements based on the value returned
    // from this method, e.g. overriding this method is the only difference between CheckBox
    // and its CompoundButton super class:
    // http://androidxref.com/8.0.0_r4/xref/frameworks/base/core/java/android/widget/CheckBox.java
    //
    // We want to act like a checkbox so we return checkbox here. We also need to update the
    // checked state in onInitializeAccessibilityNodeInfo (below).
    override fun getAccessibilityClassName(): CharSequence = CheckBox::class.java.name

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo?) {
        super.onInitializeAccessibilityNodeInfo(info)
        info?.isChecked = isChecked
        // Issue #528 - isCheckable is intended to announce checked state on focus, but doesn't
        // work with VoiceView
        info?.isCheckable = true
    }
}
