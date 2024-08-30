/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.jobs.offlineOperations.repository

import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile

class OfflineOperationsRepository(
    private val fileDataStorageManager: FileDataStorageManager
) : OfflineOperationsRepositoryType {

    private val dao = fileDataStorageManager.offlineOperationDao
    private val pathSeparator = '/'

    @Suppress("NestedBlockDepth")
    override fun getAllSubdirectories(fileId: Long): List<OfflineOperationEntity> {
        val result = mutableListOf<OfflineOperationEntity>()
        val queue = ArrayDeque<Long>()
        queue.add(fileId)
        val processedIds = mutableSetOf<Long>()

        while (queue.isNotEmpty()) {
            val currentFileId = queue.removeFirst()
            if (currentFileId in processedIds || currentFileId == 1L) continue

            processedIds.add(currentFileId)

            val subDirectories = dao.getSubDirectoriesByParentOCFileId(currentFileId)
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
        getAllSubdirectories(file.fileId).forEach {
            dao.delete(it)
        }

        file.decryptedRemotePath?.let {
            val entity = dao.getByPath(it)
            entity?.let {
                dao.delete(entity)
            }
        }

        fileDataStorageManager.removeFile(file, true, true)
    }

    override fun updateNextOperations(operation: OfflineOperationEntity) {
        val ocFile = fileDataStorageManager.getFileByDecryptedRemotePath(operation.path)
        val fileId = ocFile?.fileId ?: return

        getAllSubdirectories(fileId)
            .mapNotNull { nextOperation ->
                nextOperation.parentOCFileId?.let { parentId ->
                    fileDataStorageManager.getFileById(parentId)?.let { ocFile ->
                        ocFile.decryptedRemotePath?.let { updatedPath ->
                            val newParentPath = ocFile.parentRemotePath
                            val newPath = updatedPath + nextOperation.filename + pathSeparator

                            if (newParentPath != nextOperation.parentPath || newPath != nextOperation.path) {
                                nextOperation.apply {
                                    parentPath = newParentPath
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
}
