/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import com.nextcloud.client.database.entity.FileEntity
import com.owncloud.android.ui.adapter.helper.OCFileListAdapterDataProvider

@Suppress("ReturnCount")
class OCFileListAdapterDataProviderImpl(private val storageManager: FileDataStorageManager) :
    OCFileListAdapterDataProvider {
    override fun convertToOCFiles(id: Long): List<OCFile> =
        storageManager.offlineOperationsRepository.convertToOCFiles(id)

    override suspend fun getFolderContent(id: Long): List<FileEntity> =
        storageManager.fileDao.getFolderContentSuspended(id)

    override fun createFileInstance(entity: FileEntity): OCFile = storageManager.createFileInstance(entity)

    override suspend fun hasFavoriteParent(fileId: Long): Boolean {
        var currentId: Long? = fileId

        while (currentId != null) {
            val parentId = storageManager.fileDao.getParentId(currentId) ?: return false
            val isFavorite = storageManager.fileDao.isFavoriteFolder(parentId) == 1
            if (isFavorite) return true
            currentId = parentId
        }

        return false
    }

    override suspend fun hasSharedParent(fileId: Long): Boolean {
        var currentId: Long? = fileId

        while (currentId != null) {
            val parentId = storageManager.fileDao.getParentId(currentId) ?: return false
            val isShared = storageManager.fileDao.isSharedFolder(parentId) == true
            if (isShared) return true
            currentId = parentId
        }

        return false
    }
}
