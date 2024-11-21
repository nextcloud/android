/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.TextUtils
import android.widget.Toast
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Helper implementation to copy a string into the system clipboard.
 */
object ClipboardUtil {
    private val TAG = ClipboardUtil::class.java.name

    @JvmStatic
    @JvmOverloads
    @Suppress("TooGenericExceptionCaught")
    fun copyToClipboard(activity: Activity, text: String?, showToast: Boolean = true) {
        if (!MDMConfig.clipBoardSupport(activity)) {
            return
        }

        if (!TextUtils.isEmpty(text)) {
            try {
                val clip = ClipData.newPlainText(
                    activity.getString(
                        R.string.clipboard_label,
                        activity.getString(R.string.app_name)
                    ),
                    text
                )
                (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                if (showToast) {
                    Toast.makeText(activity, R.string.clipboard_text_copied, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(activity, R.string.clipboard_unexpected_error, Toast.LENGTH_SHORT).show()
                Log_OC.e(TAG, "Exception caught while copying to clipboard", e)
            }
        } else {
            Toast.makeText(activity, R.string.clipboard_no_text_to_copy, Toast.LENGTH_SHORT).show()
        }
    }
}
