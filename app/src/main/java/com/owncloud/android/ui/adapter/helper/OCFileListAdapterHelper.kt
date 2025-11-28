/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.helper

import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.filterFilenames
import com.nextcloud.utils.extensions.isTempFile
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class OCFileListAdapterHelper {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    @Suppress("LongParameterList")
    fun prepareFileList(
        directory: OCFile,
        dataProvider: OCFileListAdapterDataProvider,
        onlyOnDevice: Boolean,
        limitToMimeType: String,
        preferences: AppPreferences,
        userId: String,
        onComplete: (List<OCFile>, FileSortOrder) -> Unit
    ) {
        job = scope.launch {
            val (sortedList, sortOrder) = prepareFileList(
                directory,
                dataProvider,
                onlyOnDevice,
                limitToMimeType,
                preferences,
                userId
            )
            withContext(Dispatchers.Main) {
                onComplete(sortedList, sortOrder)
            }
        }
    }

    suspend fun prepareFileList(
        directory: OCFile,
        dataProvider: OCFileListAdapterDataProvider,
        onlyOnDevice: Boolean,
        limitToMimeType: String,
        preferences: AppPreferences,
        userId: String
    ): Pair<List<OCFile>, FileSortOrder> {
        val showHiddenFiles = preferences.isShowHiddenFilesEnabled()
        val hasMimeTypeFilter = limitToMimeType.isNotEmpty()
        val isRootAndPersonalOnly = (OCFile.ROOT_PATH == directory.remotePath && MainApp.isOnlyPersonFiles())
        val isSharedView = (DrawerActivity.menuItemId == R.id.nav_shared)
        val isFavoritesView = (DrawerActivity.menuItemId == R.id.nav_favorites)

        val rawResult = getFolderContent(directory, dataProvider, onlyOnDevice)
        val filtered = ArrayList<OCFile>(rawResult.size)

        for (file in rawResult) {
            if (!showHiddenFiles && file.isHidden) {
                continue
            }

            if (hasMimeTypeFilter && !(file.isFolder || file.mimeType.startsWith(limitToMimeType))) {
                continue
            }

            if (isRootAndPersonalOnly) {
                val isPersonal = file.ownerId?.let { ownerId ->
                    ownerId == userId && !file.isSharedWithMe && !file.mounted()
                } == true

                if (!isPersonal) {
                    continue
                }
            }

            if (isSharedView && !file.isShared) {
                continue
            }

            if (isFavoritesView && !file.isFavorite) {
                continue
            }

            if (file.isTempFile()) {
                continue
            }

            filtered.add(file)
        }

        val afterFilenameFilter = filtered.filterFilenames()
        val merged = mergeOCFilesForLivePhoto(afterFilenameFilter)
        val finalList = addOfflineOperations(merged, directory.fileId, dataProvider)
        return sortData(directory, finalList, preferences)
    }

    private fun addOfflineOperations(
        files: List<OCFile>,
        fileId: Long,
        dataProvider: OCFileListAdapterDataProvider
    ): List<OCFile> {
        val offlineOperations = dataProvider.convertToOCFiles(fileId)
        if (offlineOperations.isEmpty()) return files

        val newFiles = offlineOperations.filter { offlineFile ->
            files.none { it.decryptedRemotePath == offlineFile.decryptedRemotePath }
        }

        return files + newFiles
    }

    @Suppress("NestedBlockDepth")
    private fun mergeOCFilesForLivePhoto(files: List<OCFile>): List<OCFile> {
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

    private suspend fun sortData(
        directory: OCFile,
        files: List<OCFile>,
        preferences: AppPreferences
    ): Pair<List<OCFile>, FileSortOrder> = withContext(
        Dispatchers.IO
    ) {
        val sortOrder = preferences.getSortOrderByFolder(directory)
        val foldersBeforeFiles: Boolean = preferences.isSortFoldersBeforeFiles()
        val favoritesFirst: Boolean = preferences.isSortFavoritesFirst()
        return@withContext sortOrder.sortCloudFiles(files.toMutableList(), foldersBeforeFiles, favoritesFirst)
            .toList() to sortOrder
    }

    private suspend fun getFolderContent(
        ocFile: OCFile,
        dataProvider: OCFileListAdapterDataProvider,
        onlyOnDevice: Boolean
    ): List<OCFile> = withContext(Dispatchers.IO) {
        if (!ocFile.isFolder || !ocFile.fileExists()) {
            return@withContext emptyList()
        }

        val fileEntities: List<FileEntity> = dataProvider.getFolderContent(ocFile.fileId)

        return@withContext fileEntities.mapNotNull { fileEntity ->
            val file = dataProvider.createFileInstance(fileEntity)
            if (!onlyOnDevice || file.existsOnDevice()) {
                file
            } else {
                null
            }
        }
    }

    fun cleanup() {
        job?.cancel()
        job = null
    }
}
