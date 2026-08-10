/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.helper

import com.nextcloud.android.common.ui.network.auth.ServerCredentials
import com.nextcloud.android.common.ui.share.avatar.ShareAvatarRepository
import com.nextcloud.android.common.ui.share.model.api.share.Share
import com.nextcloud.client.account.User
import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.utils.extensions.filterFilenames
import com.nextcloud.utils.extensions.isTempFile
import com.nextcloud.utils.extensions.toServerCredentials
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        // cancel previous job to not have two jobs running
        job?.cancel()

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

    fun getAvatarSharees(file: OCFile, user: User?, userId: String?, onComplete: (List<ShareeUser>) -> Unit) {
        scope.launch {
            val result = if (supportsUnifiedShare(user) && user != null) {
                val credentials = getServerCredentials(user) ?: return@launch
                val sourceId = file.remoteId
                val repository = ShareAvatarRepository(credentials).fetchShareAvatars(sourceId)
                repository?.toAvatarSharees() ?: listOf()
            } else {
                val sharees = file.sharees
                val ownerId = file.ownerId

                val ownerSharee = if (!ownerId.isNullOrEmpty() && ownerId != userId) {
                    ShareeUser(ownerId, file.ownerDisplayName, ShareType.USER).takeIf { it !in sharees }
                } else {
                    null
                }

                listOfNotNull(ownerSharee) + sharees.asReversed()
            }

            withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }

    private fun supportsUnifiedShare(user: User?): Boolean =
        user?.server?.version?.isNewerOrEqual(NextcloudVersion.nextcloud_34) == true

    private fun List<Share>.toAvatarSharees(): List<ShareeUser> = asSequence()
        .flatMap { share -> share.invitedRecipients }
        .distinctBy { recipient -> recipient.value }
        .map { recipient -> ShareeUser(recipient.value, recipient.displayName, ShareType.USER) }
        .toList()

    @Suppress("TooGenericExceptionCaught")
    private fun getServerCredentials(user: User): ServerCredentials? = try {
        OwnCloudClientManagerFactory
            .getDefaultSingleton()
            .getClientFor(user.toOwnCloudAccount(), MainApp.getAppContext())
            .toServerCredentials(user.server.uri.toString())
    } catch (e: Exception) {
        Log_OC.e(TAG, "Failed to create client for share avatars", e)
        null
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
        val localIdMap: Map<String, OCFile> = files
            .filter { it.localId != 0L && it.localId != -1L }
            .associateBy { it.localId.toString() }

        val filesToRemove = mutableSetOf<OCFile>()

        for (file in files) {
            val linkedId = file.linkedFileIdForLivePhoto ?: continue

            // no match, skip
            val linkedFile = localIdMap[linkedId] ?: continue

            when {
                MimeTypeUtil.isVideo(linkedFile.mimeType) -> {
                    file.livePhotoVideo = linkedFile
                    filesToRemove.add(linkedFile)
                }

                MimeTypeUtil.isVideo(file.mimeType) -> {
                    linkedFile.livePhotoVideo = file
                    filesToRemove.add(file)
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

    companion object {
        private val TAG = OCFileListAdapterHelper::class.java.simpleName
    }
}
