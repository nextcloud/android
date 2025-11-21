/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.helper

import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.filterByMimeType
import com.nextcloud.utils.extensions.filterFilenames
import com.nextcloud.utils.extensions.filterHiddenFiles
import com.nextcloud.utils.extensions.filterTempFilter
import com.nextcloud.utils.extensions.limitToPersonalFiles
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OCFileListAdapterHelper {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun prepareFileList(
        directory: OCFile,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean,
        limitToMimeType: String,
        preferences: AppPreferences,
        userId: String,
        onComplete: (List<OCFile>, FileSortOrder) -> Unit
    ) {
        scope.launch {
            var result = getFolderContent(directory, storageManager, onlyOnDevice)

            if (!preferences.isShowHiddenFilesEnabled()) {
                result = result.filterHiddenFiles()
            }

            if (!limitToMimeType.isEmpty()) {
                result = result.filterByMimeType(limitToMimeType)
            }

            if (OCFile.ROOT_PATH == directory.remotePath && MainApp.isOnlyPersonFiles()) {
                result = result.limitToPersonalFiles(userId)
            }

            if (DrawerActivity.menuItemId == R.id.nav_shared) {
                result = result.filter { it.isShared }
            }

            if (DrawerActivity.menuItemId == R.id.nav_favorites) {
                result = result.filter { it.isFavorite }
            }

            result = result.filterTempFilter()
            result = result.filterFilenames()
            result = mergeOCFilesForLivePhoto(result)
            result = addOfflineOperations(result, directory.fileId, storageManager)
            val (newList, newSortOrder) = sortData(directory, result, preferences)

            withContext(Dispatchers.Main) {
                onComplete(newList, newSortOrder)
            }
        }
    }

    private fun addOfflineOperations(files: List<OCFile>, fileId: Long, storageManager: FileDataStorageManager): List<OCFile> {
        val offlineOperations = storageManager.offlineOperationsRepository.convertToOCFiles(fileId)
        if (offlineOperations.isEmpty()) return files

        val newFiles = offlineOperations.filter { offlineFile ->
            files.none { it.decryptedRemotePath == offlineFile.decryptedRemotePath }
        }

        return files + newFiles
    }

    fun mergeOCFilesForLivePhoto(files: List<OCFile>): List<OCFile> {
        val filesToRemove = mutableSetOf<OCFile>()

        for (i in files.indices) {
            val file = files[i]

            for (j in i + 1 until files.size) {
                val nextFile = files[j]
                val fileLocalId = file.localId.toString()
                val nextFileLinkedLocalId = nextFile.linkedFileIdForLivePhoto

                if (fileLocalId == nextFileLinkedLocalId) {
                    when {
                        MimeTypeUtil.isVideo(file.mimeType) -> {
                            nextFile.livePhotoVideo = file
                            filesToRemove.add(file)
                        }
                        MimeTypeUtil.isVideo(nextFile.mimeType) -> {
                            file.livePhotoVideo = nextFile
                            filesToRemove.add(nextFile)
                        }
                    }
                }
            }
        }

        return files.filter { it !in filesToRemove }
    }

    private suspend fun sortData(directory: OCFile, files: List<OCFile>, preferences: AppPreferences): Pair<List<OCFile>, FileSortOrder> = withContext(
        Dispatchers.IO) {
        val sortOrder = preferences.getSortOrderByFolder(directory)
        val foldersBeforeFiles: Boolean = preferences.isSortFoldersBeforeFiles()
        val favoritesFirst: Boolean = preferences.isSortFavoritesFirst()
        return@withContext sortOrder.sortCloudFiles(files.toMutableList(), foldersBeforeFiles, favoritesFirst).toList() to sortOrder
    }

    private suspend fun getFolderContent(ocFile: OCFile, storageManager: FileDataStorageManager, onlyOnDevice: Boolean): List<OCFile> = withContext(Dispatchers.IO) {
        if (!ocFile.isFolder || !ocFile.fileExists()) {
            return@withContext emptyList()
        }

        val fileEntities: List<FileEntity> = storageManager.fileDao.getFolderContentSuspended(ocFile.fileId)

        return@withContext fileEntities.mapNotNull { fileEntity ->
            val file = storageManager.createFileInstance(fileEntity)
            if (!onlyOnDevice || file.existsOnDevice()) {
                file
            } else {
                null
            }
        }
    }
}
