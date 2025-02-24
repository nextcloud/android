/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.owncloud.android.lib.common.utils.Log_OC

class ClipboardClearWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {
    private val tag = ClipboardClearWorker::class.java.name

    companion object {
        const val CLIPBOARD_TEXT = "clipboard_text"
    }

    @Suppress("TooGenericExceptionCaught", "ReturnCount")
    override fun doWork(): Result {
        try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val currentClip = clipboardManager.primaryClip ?: return Result.success()
            val clipboardText = currentClip.getItemAt(0).text?.toString() ?: return Result.success()
            val copiedText = inputData.getString(CLIPBOARD_TEXT)
            if (copiedText != clipboardText) {
                return Result.success()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboardManager.clearPrimaryClip()
            } else {
                val newEmptyClip = ClipData.newPlainText("EmptyClipContent", "")
                clipboardManager.setPrimaryClip(newEmptyClip)
            }

            return Result.success()
        } catch (e: Exception) {
            Log_OC.e(tag, "Error in clipboard clear worker", e)
            return Result.retry()
        }
    }
}
