/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.model.file

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import com.nextcloud.client.player.util.observeContentChanges
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.utils.FileSortOrder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PlaybackFilesRepository(
    private val storageManager: FileDataStorageManager,
    private val preferences: AppPreferences,
    private val dispatcher: CoroutineDispatcher,
    private val contentObserver: (uri: Uri, notifyForDescendants: Boolean) -> Flow<Boolean>
) {

    @Inject
    constructor(context: Context, storageManager: FileDataStorageManager, preferences: AppPreferences) : this(
        storageManager,
        preferences,
        Dispatchers.IO,
        context.contentResolver::observeContentChanges
    )

    companion object {
        private const val FETCH_DATA_DEBOUNCE_MS = 250L
    }

    fun observe(folderId: Long, fileType: PlaybackFileType, searchType: SearchType?): Flow<PlaybackFiles> =
        when (searchType) {
            SearchType.FAVORITE_SEARCH -> observeFavoritePlaybackFiles(fileType)
            SearchType.GALLERY_SEARCH -> observeGalleryPlaybackFiles(fileType)
            SearchType.SHARED_FILTER -> observeSharedPlaybackFiles(fileType)
            else -> observeFolderPlaybackFiles(folderId, fileType, MainApp.isOnlyOnDevice())
        }

    suspend fun get(folderId: Long, fileType: PlaybackFileType, searchType: SearchType?): PlaybackFiles =
        when (searchType) {
            SearchType.FAVORITE_SEARCH -> getFavoritePlaybackFiles(fileType)
            SearchType.GALLERY_SEARCH -> getGalleryPlaybackFiles(fileType)
            SearchType.SHARED_FILTER -> getSharedPlaybackFiles(fileType)
            else -> getFolderPlaybackFiles(folderId, fileType, MainApp.isOnlyOnDevice())
        }

    private fun observeFavoritePlaybackFiles(fileType: PlaybackFileType): Flow<PlaybackFiles> {
        val uri = ProviderTableMeta.CONTENT_URI
        return observeData(uri, true) {
            getFavoritePlaybackFiles(fileType)
        }
    }

    private suspend fun getFavoritePlaybackFiles(fileType: PlaybackFileType): PlaybackFiles = withContext(dispatcher) {
        storageManager.favoriteFiles
            .asSequence()
            .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
            .map { it.toPlaybackFile() }
            .sortedWith(PlaybackFilesComparator.FAVORITE)
            .let { PlaybackFiles(it.toList(), PlaybackFilesComparator.FAVORITE) }
    }

    private fun observeGalleryPlaybackFiles(fileType: PlaybackFileType): Flow<PlaybackFiles> {
        val uri = ProviderTableMeta.CONTENT_URI
        return observeData(uri, true) {
            getGalleryPlaybackFiles(fileType)
        }
    }

    private suspend fun getGalleryPlaybackFiles(fileType: PlaybackFileType): PlaybackFiles = withContext(dispatcher) {
        storageManager.allGalleryItems
            .asSequence()
            .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
            .map { it.toPlaybackFile() }
            .sortedWith(PlaybackFilesComparator.GALLERY)
            .let { PlaybackFiles(it.toList(), PlaybackFilesComparator.GALLERY) }
    }

    private fun observeSharedPlaybackFiles(fileType: PlaybackFileType): Flow<PlaybackFiles> {
        val uri = ProviderTableMeta.CONTENT_URI_SHARE
        return observeData(uri, false) {
            getSharedPlaybackFiles(fileType)
        }
    }

    private suspend fun getSharedPlaybackFiles(fileType: PlaybackFileType): PlaybackFiles = withContext(dispatcher) {
        storageManager.shares
            .asSequence()
            .distinctBy { it.fileSource }
            .map { it.toPlaybackFile() }
            .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
            .sortedWith(PlaybackFilesComparator.SHARED)
            .let { PlaybackFiles(it.toList(), PlaybackFilesComparator.SHARED) }
    }

    private fun observeFolderPlaybackFiles(
        folderId: Long,
        fileType: PlaybackFileType,
        onDeviceOnly: Boolean
    ): Flow<PlaybackFiles> {
        val uri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, folderId)
        val sortOrderFlow = flow {
            emit(getFolderSortOrder(folderId))
        }
        return sortOrderFlow.flatMapConcat { sortOrder ->
            val comparator = sortOrder.toPlaybackFilesComparator()
            observeData(uri, false) {
                getFolderPlaybackFiles(folderId, fileType, onDeviceOnly, comparator)
            }
        }
    }

    private suspend fun getFolderPlaybackFiles(
        folderId: Long,
        fileType: PlaybackFileType,
        onDeviceOnly: Boolean,
        comparator: PlaybackFilesComparator? = null
    ): PlaybackFiles = withContext(dispatcher) {
        val folder = storageManager.getFileById(folderId) ?: throw IllegalStateException("Folder not found")
        val comparator = comparator ?: preferences.getSortOrderByFolder(folder).toPlaybackFilesComparator()
        storageManager.getFolderContent(folder, onDeviceOnly)
            .asSequence()
            .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
            .map { it.toPlaybackFile() }
            .sortedWith(comparator)
            .let { PlaybackFiles(it.toList(), comparator) }
    }

    private suspend fun getFolderSortOrder(folderId: Long): FileSortOrder = withContext(dispatcher) {
        val folder = storageManager.getFileById(folderId) ?: throw IllegalStateException("Folder not found")
        preferences.getSortOrderByFolder(folder)
    }

    private fun <T> observeData(uri: Uri, notifyForDescendants: Boolean, fetchData: suspend () -> T): Flow<T> =
        contentObserver(uri, notifyForDescendants)
            .debounce(FETCH_DATA_DEBOUNCE_MS) // Debounce to avoid too frequent data fetching for batch updates
            .map { fetchData() }
            .onStart { emit(fetchData()) }
            .distinctUntilChanged()
}
