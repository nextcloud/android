/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component.filename.model

import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.text.TextUtils
import androidx.annotation.StyleRes
import com.owncloud.android.R
import com.google.android.material.R as MaterialR

data class FilenameStyle(
    @StyleRes val textAppearance: Int,
    val textColor: ColorStateList?,
    val textSize: Float?,
    val titleMaxLines: Int,
    val titleEllipsize: TextUtils.TruncateAt
) {
    // The extension is always one line and cut from the start, so its end stays visible.
    val extensionMaxLines: Int = SINGLE_LINE
    val extensionEllipsize: TextUtils.TruncateAt = TextUtils.TruncateAt.START

    companion object {
        const val SINGLE_LINE = 1

        private const val TITLE_MAX_LINES = 3

        private const val ELLIPSIZE_START = 1
        private const val ELLIPSIZE_MIDDLE = 2
        private const val ELLIPSIZE_END = 3

        // Bottom sheet sets nothing, so it gets the defaults: big headline text, up to 3 lines, cut at the end.
        // Rows set their own size, color, 1 line and cut in the middle in their layout XML.
        // Color and size stay null when not set, so the text appearance decides them.
        fun from(typedArray: TypedArray): FilenameStyle {
            val textSize = if (typedArray.hasValue(R.styleable.FilenameView_filenameTextSize)) {
                typedArray.getDimension(R.styleable.FilenameView_filenameTextSize, 0f)
            } else {
                null
            }

            return FilenameStyle(
                textAppearance = typedArray.getResourceId(
                    R.styleable.FilenameView_filenameTextAppearance,
                    MaterialR.style.TextAppearance_Material3_HeadlineSmall
                ),
                textColor = typedArray.getColorStateList(R.styleable.FilenameView_filenameTextColor),
                textSize = textSize,
                titleMaxLines = typedArray.getInt(R.styleable.FilenameView_filenameMaxLines, TITLE_MAX_LINES),
                titleEllipsize = truncateAtOf(
                    typedArray.getInt(R.styleable.FilenameView_filenameEllipsize, ELLIPSIZE_END)
                )
            )
        }

        private fun truncateAtOf(ellipsize: Int): TextUtils.TruncateAt = when (ellipsize) {
            ELLIPSIZE_START -> TextUtils.TruncateAt.START
            ELLIPSIZE_MIDDLE -> TextUtils.TruncateAt.MIDDLE
            else -> TextUtils.TruncateAt.END
        }
    }
}
