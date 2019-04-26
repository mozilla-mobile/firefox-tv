/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.tv.firefox.hint

import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import android.view.View
import androidx.core.view.isVisible
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.hint_bar.view.hintBarText
import mozilla.components.support.ktx.android.content.res.pxToDp
import java.util.concurrent.TimeUnit

private const val IMAGE = "\$IMAGE"
private const val IMAGE_SIZE_DP = 24

/**
 * Simple object that allows different consumers of [HintViewModel]s share binding code
 */
object HintBinder {

    fun bindHintsToView(vm: HintViewModel, hintContainer: View, animate: Boolean): List<Disposable> {
        val displayedDisposable = if (!animate) {
            vm.isDisplayed
                    .doOnDispose { hintContainer.isVisible = false }
                    .subscribe { hintContainer.isVisible = it }
        } else {
            vm.isDisplayed
                    .doOnDispose { hintContainer.isVisible = false }
                    .doOnSubscribe {
                        hintContainer.animate()
                                .setDuration(0)
                                .translationY(hintContainer.height.toFloat())
                                .start()
                    }
                    .subscribe { shouldDisplay ->
                        hintContainer.isVisible = true
                        val (translationY, alpha) = when (shouldDisplay) {
                            true -> Pair(0f, 1f)
                            false -> Pair(hintContainer.height.toFloat(), 0f)
                        }
                        hintContainer.animate()
                                .setDuration(250)
                                .setInterpolator(FastOutSlowInInterpolator())
                                .translationY(translationY)
                                .alpha(alpha)
                                .start()
                    }
        }

        val hintDisposable = vm.hints.subscribe {
            val hint = it.firstOrNull() ?: return@subscribe // For the first version, only one hint is shown
            val resources = hintContainer.context.resources

            val styledText = if (!hint.text.contains(IMAGE)) {
                hint.text
            } else {
                val spannableBuilder = SpannableStringBuilder(hint.text)
                val imageStart = hint.text.indexOf(IMAGE)
                val imageEnd = imageStart + IMAGE.length
                val image = hintContainer.context.getDrawable(hint.icon)!!
                val imageSize = resources.pxToDp(IMAGE_SIZE_DP)
                image.setBounds(0, 0, imageSize, imageSize)
                val imageSpan = ImageSpan(image, ImageSpan.ALIGN_BOTTOM)
                spannableBuilder.setSpan(imageSpan, imageStart, imageEnd, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                spannableBuilder
            }

            hintContainer.hintBarText.text = styledText
            hintContainer.hintBarText.contentDescription = hint.contentDescription
        }

        return listOf(displayedDisposable, hintDisposable)
    }
}
