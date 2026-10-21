/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.text

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle

object SpanFormatter {

    @JvmStatic
    fun styleLast(text: String, part: String?, style: CharacterStyle): SpannableStringBuilder {
        val builder = SpannableStringBuilder(text)
        if (part == null) {
            return builder
        }

        val start = text.lastIndexOf(part)
        if (start >= 0) {
            builder.setSpan(style, start, start + part.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)
        }
        return builder
    }
}
