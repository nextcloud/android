/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.OCFile
import java.io.File
import java.nio.file.Files

object EncryptedLocalCopy {
    fun discardRawCopy(previous: OCFile, refreshed: OCFile, accountDirectory: File) {
        if (previous.isEncrypted || previous.isFolder) return
        previous.storagePath?.let { path ->
            val root = accountDirectory.canonicalFile
            val expected = File(root.path + previous.decryptedRemotePath).canonicalFile
            val cached = File(path).canonicalFile
            if (cached == expected && cached.toPath().startsWith(root.toPath()) && cached.isFile) {
                Files.delete(cached.toPath())
            }
        }
        refreshed.storagePath = null
        refreshed.lastSyncDateForData = 0
    }
}
