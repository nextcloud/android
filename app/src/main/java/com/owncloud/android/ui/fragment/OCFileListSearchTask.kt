/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.accounts.Account
import android.content.ContentValues
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.adapter.OCShareToOCFileConverter
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference

@Suppress("LongParameterList", "ReturnCount", "TooGenericExceptionCaught")
@SuppressLint("NotifyDataSetChanged")
class OCFileListSearchTask(
    fragment: OCFileListFragment,
    private val remoteOperation: RemoteOperation<List<Any>>,
    private val currentUser: User,
    private val event: SearchEvent,
    private val taskTimeout: Long,
    private val preferences: AppPreferences,
    private val storageManager: FileDataStorageManager
) {
    companion object {
        private const val TAG = "OCFileListSearchTask"
    }

    private val fragmentReference: WeakReference<OCFileListFragment> = WeakReference(fragment)
    private var job: Job? = null

    @Suppress("TooGenericExceptionCaught", "DEPRECATION", "ReturnCount")
    fun execute() {
        Log_OC.d(TAG, "search task running, query: ${event.searchType}")
        val fragment = fragmentReference.get() ?: return

        job = fragment.lifecycleScope.launch(Dispatchers.IO) {
            val searchType = fragment.currentSearchType

            val cachedFiles = loadSortedCachedDbFiles(event.searchType, searchType, fragment)
            if (cachedFiles.isNotEmpty()) {
                updateAdapterData(fragment, cachedFiles)
            }

            val result = fetchRemoteResults()?.takeIf { it.isSuccess } ?: run {
                showSnackbarError(fragment)
                return@launch
            }

            val resultData = result.resultData?.takeIf { it.isNotEmpty() } ?: run {
                withContext(Dispatchers.Main) { fragment.setEmptyListMessage(fragment.currentSearchType) }
                return@launch
            }

            fragment.adapter.prepareForSearchData(storageManager, fragment.currentSearchType)
            val remoteFiles = fetchAndSortRemoteFiles(searchType, cachedFiles, resultData, fragment)
            updateAdapterData(fragment, remoteFiles)
        }
    }

    private suspend fun showSnackbarError(fragment: OCFileListFragment) {
        withContext(Dispatchers.Main) {
            fragment.activity?.let {
                DisplayUtils.showSnackMessage(it, R.string.error_fetching_sharees)
            }
        }
    }

    private suspend fun loadSortedCachedDbFiles(
        searchType: SearchRemoteOperation.SearchType,
        fragmentSearchType: SearchType,
        fragment: OCFileListFragment
    ): List<OCFile> {
        val files = if (searchType == SearchRemoteOperation.SearchType.SHARED_FILTER) {
            storageManager.fileDao.getSharedFiles(currentUser.accountName)
        } else {
            storageManager.fileDao.getFavoriteFiles(currentUser.accountName)
        }.mapNotNull { storageManager.createFileInstance(it) }

        return sortSearchData(files, fragmentSearchType, fragment)
    }

    private suspend fun fetchAndSortRemoteFiles(
        searchType: SearchType,
        sortedFilesInDb: List<OCFile>,
        resultData: List<Any>,
        fragment: OCFileListFragment
    ): List<OCFile> {
        val newList = if (searchType == SearchType.SHARED_FILTER) {
            OCShareToOCFileConverter.parseAndSaveShares(
                sortedFilesInDb,
                resultData,
                storageManager,
                currentUser.accountName
            )
        } else {
            parseAndSaveVirtuals(resultData, fragment)
        }

        return sortSearchData(newList, searchType, fragment)
    }

    @Suppress("DEPRECATION")
    private suspend fun fetchRemoteResults(): RemoteOperationResult<List<Any>>? {
        val fragment = fragmentReference.get() ?: return null
        val context = fragment.context ?: return null

        return try {
            withTimeoutOrNull(taskTimeout) {
                remoteOperation.execute(currentUser, context)
            } ?: remoteOperation.executeNextcloudClient(currentUser, context)
        } catch (e: Exception) {
            Log_OC.e(TAG, "exception execute: ", e)
            null
        }
    }

    private suspend fun updateAdapterData(fragment: OCFileListFragment, newList: List<OCFile>) =
        withContext(Dispatchers.Main) {
            if (!fragment.isAdded || !fragment.searchFragment) {
                Log_OC.e(TAG, "cannot update adapter data, fragment is not ready")
                return@withContext
            }

            fragment.adapter.updateAdapter(newList, null)
        }

    private fun sortSearchData(list: List<OCFile>, searchType: SearchType, fragment: OCFileListFragment): List<OCFile> {
        if (searchType == SearchType.GALLERY_SEARCH ||
            searchType == SearchType.RECENT_FILES_SEARCH
        ) {
            return FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(list)
        }

        val foldersBeforeFiles = preferences.isSortFoldersBeforeFiles()
        val favoritesFirst = preferences.isSortFavoritesFirst()

        val sortOrder = when (searchType) {
            SearchType.FAVORITE_SEARCH -> {
                preferences.getSortOrderByType(FileSortOrder.Type.favoritesListView)
            }

            SearchType.SHARED_FILTER -> {
                FileSortOrder.SORT_A_TO_Z
            }

            else -> {
                preferences.getSortOrderByFolder(null)
            }
        }

        fragment.adapter.setSortOrder(sortOrder)
        return sortOrder.sortCloudFiles(list.toMutableList(), foldersBeforeFiles, favoritesFirst)
    }

    @Suppress("DEPRECATION")
    private suspend fun parseAndSaveVirtuals(data: List<Any>, fragment: OCFileListFragment): List<OCFile> =
        withContext(Dispatchers.IO) {
            val activity = fragment.activity ?: return@withContext emptyList()
            val now = System.currentTimeMillis()

            val (virtualType, onlyMedia) = when (fragment.currentSearchType) {
                SearchType.FAVORITE_SEARCH -> VirtualFolderType.FAVORITE to false
                SearchType.GALLERY_SEARCH -> VirtualFolderType.GALLERY to true
                else -> VirtualFolderType.NONE to false
            }

            val contentValuesList = ArrayList<ContentValues>()
            val resultFiles = ArrayList<OCFile>()
            var cachedClient: Account? = null

            for (obj in data) {
                try {
                    val remoteFile = obj as? RemoteFile ?: continue
                    val newFile = FileStorageUtils.fillOCFile(remoteFile)

                    val existingFile = storageManager.getFileByPath(newFile.remotePath)
                    if (existingFile != null) {
                        preserveLocalFieldsOverEmptyRemoteFile(newFile, existingFile)
                    }

                    var ocFile = newFile
                    FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, currentUser.accountName)
                    resolveLocalFileId(ocFile)
                    ocFile = storageManager.saveFileWithParent(ocFile, activity)
                    ocFile = handleEncryptionIfNeeded(ocFile, storageManager, activity) {
                        cachedClient ?: currentUser.toPlatformAccount().also { cachedClient = it }
                    }

                    if (fragment.currentSearchType != SearchType.GALLERY_SEARCH && ocFile.isFolder) {
                        RefreshFolderOperation(ocFile, now, true, false, storageManager, currentUser, activity)
                            .execute(currentUser, activity)
                    }

                    val isMediaAllowed = !onlyMedia || MimeTypeUtil.isImage(ocFile) || MimeTypeUtil.isVideo(ocFile)
                    if (isMediaAllowed) {
                        resultFiles.add(ocFile)
                    }

                    contentValuesList.add(
                        ContentValues().apply {
                            put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, virtualType.toString())
                            put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.fileId)
                        }
                    )
                } catch (e: Exception) {
                    Log_OC.e(TAG, "parseAndSaveVirtuals():", e)
                }
            }

            preferences.setPhotoSearchTimestamp(System.currentTimeMillis())
            storageManager.saveVirtuals(contentValuesList)

            return@withContext resultFiles
        }

    /**
     * Needed to prevent overwriting the valid local fields.
     */
    private fun preserveLocalFieldsOverEmptyRemoteFile(newFile: OCFile, existingFile: OCFile) {
        if (newFile.sharees.isNullOrEmpty()) {
            newFile.sharees = existingFile.sharees
        }
        if (!newFile.isSharedViaLink) {
            newFile.isSharedViaLink = existingFile.isSharedViaLink
        }
        if (!newFile.isSharedWithSharee) {
            newFile.isSharedWithSharee = existingFile.isSharedWithSharee
        }
        if (newFile.firstShareTimestamp == 0L) {
            newFile.firstShareTimestamp = existingFile.firstShareTimestamp
        }
        if (newFile.tags.isNullOrEmpty()) {
            newFile.tags = existingFile.tags
        }
        if (newFile.imageDimension == null) {
            newFile.imageDimension = existingFile.imageDimension
        }
    }

    @Suppress("DEPRECATION")
    private fun handleEncryptionIfNeeded(
        ocFile: OCFile,
        fileDataStorage: FileDataStorageManager,
        activity: Activity,
        accountProvider: () -> Account
    ): OCFile {
        val parent = fileDataStorage.getFileById(ocFile.parentId)
            ?: return ocFile

        if (!ocFile.isEncrypted && !parent.isEncrypted) return ocFile

        val client = OwnCloudClientFactory.createOwnCloudClient(accountProvider(), activity)

        val metadata = RefreshFolderOperation.getDecryptedFolderMetadata(
            true,
            parent,
            client,
            currentUser,
            activity
        ) ?: throw IllegalStateException("metadata is null")

        when (metadata) {
            is DecryptedFolderMetadataFileV1 ->
                RefreshFolderOperation.updateFileNameForEncryptedFileV1(
                    fileDataStorage,
                    metadata,
                    ocFile
                )

            is DecryptedFolderMetadataFile ->
                RefreshFolderOperation.updateFileNameForEncryptedFile(
                    fileDataStorage,
                    metadata,
                    ocFile
                )
        }

        return fileDataStorage.saveFileWithParent(ocFile, activity)
    }

    private fun resolveLocalFileId(ocFile: OCFile) {
        if (ocFile.fileId != -1L) return
        val localFile = storageManager.getFileByLocalId(ocFile.localId) ?: return
        ocFile.fileId = localFile.fileId
    }

    // region public methods
    fun cancel() = job?.cancel(null)

    fun isFinished(): Boolean = job?.isCompleted == true
    // endregion
}
