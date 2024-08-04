/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
