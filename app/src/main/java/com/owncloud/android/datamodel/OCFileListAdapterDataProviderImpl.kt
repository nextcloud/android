/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.ui.adapter.helper.OCFileListAdapterDataProvider

class OCFileListAdapterDataProviderImpl(private val storageManager: FileDataStorageManager) :
    OCFileListAdapterDataProvider {
    override fun convertToOCFiles(id: Long): List<OCFile> =
        storageManager.offlineOperationsRepository.convertToOCFiles(id)

    override suspend fun getFolderContent(id: Long): List<FileEntity> =
        storageManager.fileDao.getFolderContentSuspended(id)

    override fun createFileInstance(entity: FileEntity): OCFile = storageManager.createFileInstance(entity)
}
