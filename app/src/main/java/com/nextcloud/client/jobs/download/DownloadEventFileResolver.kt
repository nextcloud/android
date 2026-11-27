/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.jobs.download

import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

object DownloadEventFileResolver {
    fun resolve(storage: FileDataStorageManager, accountName: String?, remotePath: String?): OCFile? {
        if (accountName == null || accountName != storage.user.accountName || remotePath.isNullOrEmpty()) return null
        return storage.getFileByEncryptedRemotePath(remotePath)
    }
}
