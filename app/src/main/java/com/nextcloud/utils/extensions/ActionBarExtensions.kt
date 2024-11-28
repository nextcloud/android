/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.ActionBar

fun ActionBar.setTitleColor(color: Int) {
    val text = SpannableString(title ?: "")
    text.setSpan(ForegroundColorSpan(color), 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
    title = text
}
