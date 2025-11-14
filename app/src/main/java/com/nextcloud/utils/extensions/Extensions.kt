/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.Selection
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun mainThread(delay: Long = 1000, action: () -> Unit) {
    Handler(Looper.getMainLooper()).postDelayed({
        action()
    }, delay)
}

fun clickWithDebounce(view: View, debounceTime: Long = 600L, action: () -> Unit) {
    view.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime: Long = 0

        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) {
                return
            } else {
                action()
            }

            lastClickTime = SystemClock.elapsedRealtime()
        }
    })
}

fun TextView.makeLinks(vararg links: Pair<String, View.OnClickListener>) {
    val spannableString = SpannableString(this.text)
    var startIndexOfLink = -1
    for (link in links) {
        val clickableSpan = object : ClickableSpan() {
            override fun updateDrawState(textPaint: TextPaint) {
                // use this to change the link color
                textPaint.color = textPaint.linkColor
                // toggle below value to enable/disable
                // the underline shown below the clickable text
                // textPaint.isUnderlineText = true
            }

            override fun onClick(view: View) {
                Selection.setSelection((view as TextView).text as Spannable, 0)
                view.invalidate()
                link.second.onClick(view)
            }
        }
        startIndexOfLink = this.text.toString().indexOf(link.first, startIndexOfLink + 1)
        spannableString.setSpan(
            clickableSpan,
            startIndexOfLink,
            startIndexOfLink + link.first.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    this.movementMethod =
        LinkMovementMethod.getInstance() // without LinkMovementMethod, link can not click
    this.setText(spannableString, TextView.BufferType.SPANNABLE)
}

fun Long.isCurrentYear(yearToCompare: String?): Boolean {
    val simpleDateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
    val currentYear = simpleDateFormat.format(Date(this))
    return currentYear == yearToCompare
}

fun Long.getFormattedStringDate(format: String): String {
    val simpleDateFormat = SimpleDateFormat(format, Locale.getDefault())
    return simpleDateFormat.format(Date(this))
}

fun TrashbinFile.toOCFile(): OCFile {
    val ocFile = OCFile(this.remotePath)
    ocFile.mimeType = this.mimeType
    ocFile.fileLength = this.fileLength
    ocFile.remoteId = this.remoteId
    ocFile.fileName = this.fileName
    return ocFile
}
