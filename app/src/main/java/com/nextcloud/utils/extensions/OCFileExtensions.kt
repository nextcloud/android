/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCFileDepth
import com.owncloud.android.datamodel.OCFileDepth.DeepLevel
import com.owncloud.android.datamodel.OCFileDepth.FirstLevel
import com.owncloud.android.datamodel.OCFileDepth.Root
import com.owncloud.android.utils.FileStorageUtils

fun List<OCFile>.filterFilenames(): List<OCFile> = distinctBy { it.fileName }

fun OCFile.isTempFile(): Boolean {
    val context = MainApp.getAppContext()
    val appTempPath = FileStorageUtils.getAppTempDirectoryPath(context)
    return storagePath?.startsWith(appTempPath) == true
}

fun OCFile?.isPNG(): Boolean {
    if (this == null) {
        return false
    }
    return "image/png".equals(mimeType, ignoreCase = true)
}

@Suppress("ReturnCount")
fun OCFile?.getDepth(): OCFileDepth? {
    if (this == null) {
        return null
    }

    // Check if it's the root directory
    if (this.isRootDirectory) {
        return Root
    }

    // If parent is root ("/"), this is a direct child of root
    val parentPath = this.parentRemotePath ?: return null
    if (parentPath == OCFile.ROOT_PATH) {
        return FirstLevel
    }

    // Otherwise, it's a subdirectory of a subdirectory
    return DeepLevel
}
