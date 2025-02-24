/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.text.TextUtils
import android.widget.Toast
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.nextcloud.client.jobs.clipboard.ClipboardClearWorker
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import java.util.concurrent.TimeUnit

object ClipboardUtil {
    private val TAG = ClipboardUtil::class.java.name
    private const val WORKER_CLEAR_DELAY = 15L

    @JvmStatic
    @JvmOverloads
    @Suppress("TooGenericExceptionCaught")
    fun copyToClipboard(activity: Activity, text: String?, showToast: Boolean = true) {
        if (!MDMConfig.clipBoardSupport(activity)) {
            return
        }

        if (TextUtils.isEmpty(text)) {
            Toast.makeText(activity, R.string.clipboard_no_text_to_copy, Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val clipboardLabel = activity.getString(R.string.clipboard_label, activity.getString(R.string.app_name))
            val clip = getClipData(clipboardLabel, text)
            val clipboardManager = (activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
            clipboardManager.setPrimaryClip(clip)

            if (showToast) {
                Toast.makeText(activity, R.string.clipboard_text_copied, Toast.LENGTH_SHORT).show()
            }

            scheduleClipboardClearWorker(activity, text)
        } catch (e: Exception) {
            Toast.makeText(activity, R.string.clipboard_unexpected_error, Toast.LENGTH_SHORT).show()
            Log_OC.e(TAG, "Exception caught while copying to clipboard", e)
        }
    }

    private fun getClipData(clipboardLabel: String, text: String?): ClipData {
        return ClipData.newPlainText(clipboardLabel, text).apply {
            description.extras = PersistableBundle().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                } else {
                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                }
            }
        }
    }

    private fun scheduleClipboardClearWorker(context: Context, text: String?) {
        val data = Data.Builder()
            .putString(ClipboardClearWorker.CLIPBOARD_TEXT, text)
            .build()

        val clearWorkRequest = OneTimeWorkRequestBuilder<ClipboardClearWorker>()
            .setInitialDelay(WORKER_CLEAR_DELAY, TimeUnit.SECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueue(clearWorkRequest)
    }
}
