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
    containerActivity: FileFragment.ContainerActivity,
    fragment: OCFileListFragment,
    private val remoteOperation: RemoteOperation<List<Any>>,
    private val currentUser: User,
    private val event: SearchEvent,
    private val taskTimeout: Long,
    private val preferences: AppPreferences
) {
    companion object {
        private const val TAG = "OCFileListSearchTask"
    }

    private val activityReference: WeakReference<FileFragment.ContainerActivity> = WeakReference(containerActivity)
    private val fragmentReference: WeakReference<OCFileListFragment> = WeakReference(fragment)

    private val fileDataStorageManager: FileDataStorageManager?
        get() = activityReference.get()?.storageManager

    private var job: Job? = null

    @Suppress("TooGenericExceptionCaught", "DEPRECATION", "ReturnCount")
    fun execute() {
        Log_OC.d(TAG, "search task running, query: ${event.searchType}")
        val fragment = fragmentReference.get() ?: return

        job = fragment.lifecycleScope.launch(Dispatchers.IO) {
            val searchType = fragment.currentSearchType

            // using cached data
            val filesInDb = loadCachedDbFiles(event.searchType)
            val sortedFilesInDb = sortSearchData(filesInDb, searchType, null, setNewSortOrder = {
                fragment.adapter.setSortOrder(it)
            })
            updateAdapterData(fragment, sortedFilesInDb)

            // updating cache and refreshing adapter
            val result = fetchRemoteResults()
            if (result?.isSuccess == true) {
                if (result.resultData?.isEmpty() == true) {
                    withContext(Dispatchers.Main) {
                        fragment.setEmptyListMessage(SearchType.NO_SEARCH)
                        return@withContext
                    }

                    return@launch
                }

                fragment.adapter.prepareForSearchData(fileDataStorageManager, fragment.currentSearchType)

                val newList = if (searchType == SearchType.SHARED_FILTER) {
                    OCShareToOCFileConverter.parseAndSaveShares(
                        result.resultData ?: listOf(),
                        fileDataStorageManager,
                        currentUser.accountName
                    )
                } else {
                    parseAndSaveVirtuals(result.resultData ?: listOf(), fragment)
                    fragment.adapter.files
                }

                val sortedNewList = sortSearchData(newList, searchType, null, setNewSortOrder = {
                    fragment.adapter.setSortOrder(it)
                })

                updateAdapterData(fragment, sortedNewList)

                return@launch
            }

            withContext(Dispatchers.Main) {
                fragment.activity?.let {
                    DisplayUtils.showSnackMessage(it, R.string.error_fetching_sharees)
                }
            }
        }
    }

    fun cancel() = job?.cancel(null)

    fun isFinished(): Boolean = job?.isCompleted == true

    private suspend fun loadCachedDbFiles(searchType: SearchRemoteOperation.SearchType): List<OCFile> {
        val storage = fileDataStorageManager ?: return emptyList()

        val rows = when (searchType) {
            SearchRemoteOperation.SearchType.SHARED_FILTER ->
                storage.fileDao.getSharedFiles(currentUser.accountName)

            SearchRemoteOperation.SearchType.FAVORITE_SEARCH ->
                storage.fileDao.getFavoriteFiles(currentUser.accountName)

            else -> null
        } ?: return emptyList()

        return rows.mapNotNull { storage.createFileInstance(it) }
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

    private suspend fun sortSearchData(
        list: List<OCFile>,
        searchType: SearchType,
        folder: OCFile?,
        setNewSortOrder: (FileSortOrder) -> Unit
    ): List<OCFile> = withContext(Dispatchers.IO) {
        var newList = list.toMutableList()

        if (searchType == SearchType.GALLERY_SEARCH ||
            searchType == SearchType.RECENTLY_MODIFIED_SEARCH
        ) {
            return@withContext FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(newList)
        }

        if (searchType != SearchType.SHARED_FILTER) {
            val foldersBeforeFiles = preferences.isSortFoldersBeforeFiles()
            val favoritesFirst = preferences.isSortFavoritesFirst()

            val sortOrder =
                if (searchType == SearchType.FAVORITE_SEARCH) {
                    preferences.getSortOrderByType(FileSortOrder.Type.favoritesListView)
                } else {
                    preferences.getSortOrderByFolder(folder)
                }

            setNewSortOrder(sortOrder)
            newList = sortOrder.sortCloudFiles(newList, foldersBeforeFiles, favoritesFirst)
        }

        return@withContext newList
    }

    @Suppress("DEPRECATION")
    private suspend fun parseAndSaveVirtuals(data: List<Any>, fragment: OCFileListFragment) =
        withContext(Dispatchers.IO) {
            val fileDataStorageManager = fileDataStorageManager ?: return@withContext
            val activity = fragment.activity ?: return@withContext
            val now = System.currentTimeMillis()

            val (virtualType, onlyMedia) = when (fragment.currentSearchType) {
                SearchType.FAVORITE_SEARCH -> VirtualFolderType.FAVORITE to false
                SearchType.GALLERY_SEARCH -> VirtualFolderType.GALLERY to true
                else -> VirtualFolderType.NONE to false
            }

            val contentValuesList = ArrayList<ContentValues>()

            for (obj in data) {
                try {
                    val remoteFile = obj as? RemoteFile ?: continue
                    var ocFile = FileStorageUtils.fillOCFile(remoteFile)
                    FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, currentUser.accountName)
                    ocFile = fileDataStorageManager.saveFileWithParent(ocFile, activity)
                    ocFile = handleEncryptionIfNeeded(ocFile, fileDataStorageManager, activity)

                    if (fragment.currentSearchType != SearchType.GALLERY_SEARCH && ocFile.isFolder) {
                        RefreshFolderOperation(
                            ocFile,
                            now,
                            true,
                            false,
                            fileDataStorageManager,
                            currentUser,
                            activity
                        ).execute(currentUser, activity)
                    }

                    val isMediaAllowed =
                        !onlyMedia || MimeTypeUtil.isImage(ocFile) || MimeTypeUtil.isVideo(ocFile)

                    if (isMediaAllowed) {
                        fragment.adapter.addVirtualFile(ocFile)
                    }

                    val cv = ContentValues().apply {
                        put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, virtualType.toString())
                        put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.fileId)
                    }
                    contentValuesList.add(cv)
                } catch (_: Exception) {
                }
            }

            // Save timestamp + virtual entries
            preferences.setPhotoSearchTimestamp(System.currentTimeMillis())
            fileDataStorageManager.saveVirtuals(contentValuesList)
        }

    @Suppress("DEPRECATION")
    private fun handleEncryptionIfNeeded(
        ocFile: OCFile,
        fileDataStorage: FileDataStorageManager,
        activity: Activity
    ): OCFile {
        val parent = fileDataStorage.getFileById(ocFile.parentId)
            ?: return ocFile

        if (!ocFile.isEncrypted && !parent.isEncrypted) return ocFile

        val client = OwnCloudClientFactory.createOwnCloudClient(
            currentUser.toPlatformAccount(),
            activity
        )

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
}
