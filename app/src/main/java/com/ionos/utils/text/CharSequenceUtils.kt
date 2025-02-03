/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

@file:JvmName("CharSequenceUtils")

package com.ionos.utils.text

import android.text.Annotation
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.view.View

fun CharSequence.convertAnnotatedTextToLinks(
    linkColor: Int,
    linkUnderline: Boolean,
    linkHandler: (type: String) -> Unit,
): SpannableString {
    val spannableString = SpannableString(this)
    val annotations = spannableString.getSpans(0, spannableString.length, Annotation::class.java)

    annotations.forEach { annotation ->
        val start = spannableString.getSpanStart(annotation)
        val end = spannableString.getSpanEnd(annotation)

        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(paint: TextPaint) {
                paint.color = linkColor
                paint.isUnderlineText = linkUnderline
            }

            override fun onClick(widget: View) {
                linkHandler.invoke(annotation.value)
            }
        }

        spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    return spannableString
}
