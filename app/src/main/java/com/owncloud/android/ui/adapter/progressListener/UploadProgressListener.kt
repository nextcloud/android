/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.progressListener

import android.widget.ProgressBar
import com.owncloud.android.db.OCUpload

class UploadProgressListener(val upload: OCUpload?, progressBar: ProgressBar?) : ProgressListener(progressBar) {

    override fun onTransferProgress(rate: Long, transferred: Long, total: Long, filename: String?) {
        progressBarRef.get()?.post {
            progressBarRef.get()?.isIndeterminate = false
            super.onTransferProgress(rate, transferred, total, filename)
        }
    }

    fun isWrapping(progressBar: ProgressBar?): Boolean {
        val wrappedProgressBar = progressBarRef.get()
        return wrappedProgressBar != null && wrappedProgressBar === progressBar
    }
}
