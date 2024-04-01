/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper_ozturk@proton.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.core.text.HtmlCompat

@Suppress("NewLineAtEndOfFile")
fun TextView.setHtmlContent(value: String) {
    movementMethod = LinkMovementMethod.getInstance()
    text = HtmlCompat.fromHtml(value, HtmlCompat.FROM_HTML_MODE_LEGACY)
}
