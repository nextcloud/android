/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations.repository

import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.model.OfflineOperationType
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil

class OfflineOperationsRepository(private val fileDataStorageManager: FileDataStorageManager) :
    OfflineOperationsRepositoryType {

    private val dao = fileDataStorageManager.offlineOperationDao
    private val pathSeparator = '/'

    @Suppress("NestedBlockDepth")
    override fun getAllSubEntities(fileId: Long): List<OfflineOperationEntity> {
        val result = mutableListOf<OfflineOperationEntity>()
        val queue = ArrayDeque<Long>()
        queue.add(fileId)
        val processedIds = mutableSetOf<Long>()

        while (queue.isNotEmpty()) {
            val currentFileId = queue.removeFirst()
            if (currentFileId in processedIds || currentFileId == 1L) continue

            processedIds.add(currentFileId)

            val subDirectories = dao.getSubEntitiesByParentOCFileId(currentFileId)
            result.addAll(subDirectories)

            subDirectories.forEach {
                val ocFile = fileDataStorageManager.getFileByDecryptedRemotePath(it.path)
                ocFile?.fileId?.let { newFileId ->
                    if (newFileId != 1L && newFileId !in processedIds) {
                        queue.add(newFileId)
                    }
                }
            }
        }

        return result
    }

    override fun deleteOperation(file: OCFile) {
        if (file.isFolder) {
            getAllSubEntities(file.fileId).forEach {
                dao.delete(it)
            }
        }

        file.decryptedRemotePath?.let {
            dao.deleteByPath(it)
        }

        fileDataStorageManager.removeFile(file, true, true)
    }

    override fun updateNextOperations(operation: OfflineOperationEntity) {
        val ocFile = fileDataStorageManager.getFileByDecryptedRemotePath(operation.path)
        val fileId = ocFile?.fileId ?: return

        getAllSubEntities(fileId)
            .mapNotNull { nextOperation ->
                nextOperation.parentOCFileId?.let { parentId ->
                    fileDataStorageManager.getFileById(parentId)?.let { ocFile ->
                        ocFile.decryptedRemotePath?.let { updatedPath ->
                            val newPath = updatedPath + nextOperation.filename + pathSeparator

                            if (newPath != nextOperation.path) {
                                nextOperation.apply {
                                    type = when (type) {
                                        is OfflineOperationType.CreateFile ->
                                            (type as OfflineOperationType.CreateFile).copy(
                                                remotePath = newPath
                                            )

                                        is OfflineOperationType.CreateFolder ->
                                            (type as OfflineOperationType.CreateFolder).copy(
                                                path = newPath
                                            )

                                        else -> type
                                    }
                                    path = newPath
                                }
                            } else {
                                null
                            }
                        }
                    }
                }
            }
            .forEach { dao.update(it) }
    }

    override fun convertToOCFiles(fileId: Long): List<OCFile> =
        dao.getSubEntitiesByParentOCFileId(fileId).map { entity ->
            OCFile(entity.path).apply {
                mimeType = if (entity.type is OfflineOperationType.CreateFolder) {
                    MimeType.DIRECTORY
                } else {
                    MimeTypeUtil.getMimeTypeFromPath(entity.path)
                }
            }
        }
}
