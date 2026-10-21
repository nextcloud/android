/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.model.file

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import com.nextcloud.client.player.util.PlayerUtil.observeContentChanges
import com.nextcloud.client.player.util.PlayerUtil.toPlaybackFile
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val FETCH_DATA_DEBOUNCE_MS = 250L

class PlaybackFilesRepository @Inject constructor(
    private val context: Context,
    private val storageManager: FileDataStorageManager,
    private val preferences: AppPreferences
) {

    fun observe(folderId: Long, fileType: PlaybackFileType, collection: PlaybackCollection): Flow<PlaybackFiles> =
        when (collection) {
            PlaybackCollection.FAVORITES -> observeFavoritePlaybackFiles(fileType)
            PlaybackCollection.GALLERY -> observeGalleryPlaybackFiles(fileType)
            PlaybackCollection.SHARED -> observeSharedPlaybackFiles(fileType)
            PlaybackCollection.ALBUM -> observeAlbumPlaybackFiles(fileType)
            PlaybackCollection.FOLDER -> observeFolderPlaybackFiles(folderId, fileType, MainApp.isOnlyOnDevice())
        }

    private fun observeAlbumPlaybackFiles(fileType: PlaybackFileType): Flow<PlaybackFiles> =
        observeData(ProviderTableMeta.CONTENT_URI, true) {
            withContext(Dispatchers.IO) {
                storageManager.getVirtualFolderContent(VirtualFolderType.ALBUM, false)
                    .asSequence()
                    .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
                    .map { it.toPlaybackFile() }
                    .sortedWith(PlaybackFilesComparator.ALBUM)
                    .let { PlaybackFiles(it.toList(), PlaybackFilesComparator.ALBUM) }
            }
        }

    private fun observeFavoritePlaybackFiles(fileType: PlaybackFileType): Flow<PlaybackFiles> =
        observeData(ProviderTableMeta.CONTENT_URI, true) {
            withContext(Dispatchers.IO) {
                storageManager.favoriteFiles
                    .asSequence()
                    .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
                    .map { it.toPlaybackFile() }
                    .sortedWith(PlaybackFilesComparator.FAVORITE)
                    .let { PlaybackFiles(it.toList(), PlaybackFilesComparator.FAVORITE) }
            }
        }

    private fun observeGalleryPlaybackFiles(fileType: PlaybackFileType): Flow<PlaybackFiles> =
        observeData(ProviderTableMeta.CONTENT_URI, true) {
            withContext(Dispatchers.IO) {
                val mediaFolderPath = preferences.getLastSelectedMediaFolder()
                storageManager.allGalleryItems
                    .asSequence()
                    .filter { it.remotePath.startsWith(mediaFolderPath) }
                    .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
                    .map { it.toPlaybackFile() }
                    .sortedWith(PlaybackFilesComparator.GALLERY)
                    .let { PlaybackFiles(it.toList(), PlaybackFilesComparator.GALLERY) }
            }
        }

    private fun observeSharedPlaybackFiles(fileType: PlaybackFileType): Flow<PlaybackFiles> =
        observeData(ProviderTableMeta.CONTENT_URI_SHARE, false) {
            withContext(Dispatchers.IO) {
                storageManager.shares
                    .asSequence()
                    .distinctBy { it.fileSource }
                    .map { it.toPlaybackFile() }
                    .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
                    .sortedWith(PlaybackFilesComparator.SHARED)
                    .let { PlaybackFiles(it.toList(), PlaybackFilesComparator.SHARED) }
            }
        }

    private fun observeFolderPlaybackFiles(
        folderId: Long,
        fileType: PlaybackFileType,
        onDeviceOnly: Boolean
    ): Flow<PlaybackFiles> = flow {
        val uri = ContentUris.withAppendedId(ProviderTableMeta.CONTENT_URI_DIR, folderId)
        val comparator = withContext(Dispatchers.IO) {
            preferences.getSortOrderByFolder(getFolder(folderId)).toPlaybackFilesComparator()
        }
        val playbackFiles = observeData(uri, false) {
            withContext(Dispatchers.IO) {
                storageManager.getFolderContent(getFolder(folderId), onDeviceOnly)
                    .asSequence()
                    .filter { it.mimeType.startsWith(fileType.value, ignoreCase = true) }
                    .map { it.toPlaybackFile() }
                    .sortedWith(comparator)
                    .let { PlaybackFiles(it.toList(), comparator) }
            }
        }
        emitAll(playbackFiles)
    }

    private fun getFolder(folderId: Long) = storageManager.getFileById(folderId)
        ?: throw IllegalStateException("Folder not found")

    private fun <T> observeData(uri: Uri, notifyForDescendants: Boolean, fetchData: suspend () -> T): Flow<T> =
        context.contentResolver.observeContentChanges(uri, notifyForDescendants)
            .debounce(FETCH_DATA_DEBOUNCE_MS) // Debounce to avoid too frequent data fetching for batch updates
            .map { fetchData() }
            .onStart { emit(fetchData()) }
            .distinctUntilChanged()
}
