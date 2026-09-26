/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.OnFilesRemovedListener

fun FileActivity.removeFiles(
    offlineFiles: List<OCFile>,
    files: List<OCFile>,
    onlyLocalCopy: Boolean,
    filesRemovedListener: OnFilesRemovedListener?
) {
    connectivityService.isNetworkAndServerAvailable { isAvailable ->
        if (isAvailable) {
            showLoadingDialog(getString(R.string.wait_a_moment))

            (this as? FileDisplayActivity)
                ?.deleteBatchTracker
                ?.startBatchDelete(files.size)

            if (files.isNotEmpty()) {
                val inBackground = (files.size != 1)
                fileOperationsHelper?.removeFiles(files, onlyLocalCopy, inBackground)
            }

            if (offlineFiles.isNotEmpty()) {
                filesRemovedListener?.onFilesRemoved()
            }

            dismissLoadingDialog()
        } else {
            if (onlyLocalCopy) {
                fileOperationsHelper?.removeFiles(files, true, true)
            } else {
                files.forEach(storageManager::addRemoveFileOfflineOperation)
            }

            filesRemovedListener?.onFilesRemoved()
        }
    }
}
