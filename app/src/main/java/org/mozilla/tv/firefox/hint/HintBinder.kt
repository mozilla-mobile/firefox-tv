/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.hint

import android.view.View
import android.widget.Space
import androidx.core.view.isVisible
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.hint_bar.view.hintBarContainer
import kotlinx.android.synthetic.main.hint_bar.view.hintBarIcon
import kotlinx.android.synthetic.main.hint_bar.view.hintBarText

/**
 * Simple object that allows different consumers of [HintViewModel]s share binding code
 */
object HintBinder {

    fun bindHintsToView(vm: HintViewModel, hintContainer: View): Array<Disposable> {
        val displayedDisposable = vm.isDisplayed
                .doOnDispose { hintContainer.hintBarContainer.isVisible = false }
                .subscribe { hintContainer.hintBarContainer.isVisible = it }

        val hintDisposable = vm.hints.subscribe {
            val hint = it.first() // For the first version, only one hint is shown
            val context = hintContainer.context
            hintContainer.hintBarIcon.setImageDrawable(context.getDrawable(hint.icon))
            hintContainer.hintBarIcon.contentDescription = context.getString(hint.contentDescription)
            hintContainer.hintBarText.text = context.getString(hint.text)
        }

        return arrayOf(displayedDisposable, hintDisposable)
    }
}
