/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.TypedValue
import org.mozilla.focus.R
import org.mozilla.focus.utils.UrlUtils

class HomeTilePlaceholderGenerator {

    companion object {
        private val TEXT_SIZE_DP = 36f
        private val DEFAULT_ICON_CHAR = '?'

        @JvmStatic
        fun generate(context: Context, url: String?): Bitmap {
            val startingChar = getRepresentativeCharacter(url)
            return generateLauncherIconPreOreo(context, startingChar)
        }

        /*
         * This method needs to be separate from generateAdaptiveLauncherIcon so that we can generate
         * the pre-Oreo icon to display in the Add To Home screen Dialog
         */
        @JvmStatic
        fun generateLauncherIconPreOreo(context: Context, character: Char): Bitmap {
            val options = BitmapFactory.Options()
            options.inMutable = true
            val shape = BitmapFactory.decodeResource(context.resources, R.drawable.ic_homescreen_shape, options)
            return drawCharacterOnBitmap(context, character, shape)
        }

        private fun drawCharacterOnBitmap(context: Context, character: Char, bitmap: Bitmap): Bitmap {
            val canvas = Canvas(bitmap)

            val paint = Paint()

            val textSize = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DP, context.resources.displayMetrics)

            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = textSize
            paint.isAntiAlias = true

            canvas.drawText(character.toString(),
                    canvas.width / 2.0f,
                    canvas.height / 2.0f - (paint.descent() + paint.ascent()) / 2.0f,
                    paint)

            return bitmap
        }

        /**
         * Get a representative character for the given URL.
         *
         * For example this method will return "f" for "http://m.facebook.com/foobar".
         */
        @JvmStatic
        fun getRepresentativeCharacter(url: String?): Char {
            val firstChar = getRepresentativeSnippet(url)?.find { it.isLetterOrDigit() }?.toUpperCase()
            return (firstChar ?: DEFAULT_ICON_CHAR)
        }

        /**
         * Get the representative part of the URL. Usually this is the host (without common prefixes).
         *
         * @return the representative snippet or null if one could not be found.
         */
        private fun getRepresentativeSnippet(url: String?): String? {
            if (url == null || url.isEmpty()) return null

            val uri = Uri.parse(url)
            val snippet = if (!uri.host.isNullOrEmpty()) {
                uri.host // cached by Uri class.
            } else if (!uri.path.isNullOrEmpty()) { // The uri may not have a host for e.g. file:// uri
                uri.path // cached by Uri class.
            } else {
                return null
            }

            // Strip common prefixes that we do not want to use to determine the representative characters
            return UrlUtils.stripCommonSubdomains(snippet)
        }
    }
}
