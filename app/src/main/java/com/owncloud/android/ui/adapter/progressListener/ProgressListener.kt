/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.progressListener

import android.widget.ProgressBar
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import java.lang.ref.WeakReference

@Suppress("MagicNumber")
abstract class ProgressListener(progressBar: ProgressBar?) : OnDatatransferProgressListener {
    private var lastPercent = 0
    protected val progressBarRef = WeakReference(progressBar)

    override fun onTransferProgress(
        progressRate: Long,
        totalTransferredSoFar: Long,
        totalToTransfer: Long,
        filename: String?
    ) {
        val percent = (100.0 * totalTransferredSoFar.toDouble() / totalToTransfer.toDouble()).toInt()

        if (percent != lastPercent) {
            progressBarRef.get()?.run {
                progress = percent
                postInvalidate()
            }
        }
        lastPercent = percent
    }
}
