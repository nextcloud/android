/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.operations.UploadFileOperation

class UploadFolderResolver(
    private val root: () -> OCFile,
    private val refresh: (OCFile) -> Unit,
    private val findChild: (OCFile, String) -> OCFile?,
    private val createChild: (OCFile, String) -> OCFile
) {
    fun resolve(path: String): OCFile {
        require(path.startsWith('/'))
        val segments = path.split('/').filter { it.isNotEmpty() }
        require(segments.none { it == "." || it == ".." })
        var folder = root()
        refresh(folder)
        for (name in segments) {
            folder = findChild(folder, name) ?: createChild(folder, name)
            check(folder.isFolder)
            refresh(folder)
        }
        return folder
    }

    companion object {
        @JvmStatic
        @Throws(UploadFolderException::class)
        fun resolve(operation: UploadFileOperation, client: OwnCloudClient, path: String): OCFile =
            UploadFolderBackend(operation, client).resolve(path)
    }
}
