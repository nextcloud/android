/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.preview

import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog.MediaState
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreviewMediaFilesLoader(
    private val storageManager: FileDataStorageManager,
    private val preferences: AppPreferences
) {

    suspend fun forFolder(parentFolderId: Long?, onlyOnDevice: Boolean): List<OCFile> = withContext(Dispatchers.IO) {
        val parentFolder = parentFolderId?.let { storageManager.getFileById(it) }
            ?: storageManager.getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
            ?: return@withContext emptyList()

        val mediaFiles = storageManager.getFolderImagesAndVideos(parentFolder, onlyOnDevice).toMutableList()

        preferences.getSortOrderByFolder(parentFolder).sortCloudFiles(
            mediaFiles,
            preferences.isSortFoldersBeforeFiles(),
            preferences.isSortFavoritesFirst()
        )
    }

    suspend fun forVirtualFolder(type: VirtualFolderType, mediaState: MediaState?): List<OCFile> =
        withContext(Dispatchers.IO) {
            val mediaFiles = readVirtualFolder(type, mediaState)

            if (type != VirtualFolderType.FAVORITE) {
                return@withContext FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(mediaFiles)
            }

            preferences.getSortOrderByType(FileSortOrder.Type.favoritesListView).sortCloudFiles(
                mediaFiles,
                preferences.isSortFoldersBeforeFiles(),
                preferences.isSortFavoritesFirst()
            )
        }

    private fun readVirtualFolder(type: VirtualFolderType, mediaState: MediaState?): MutableList<OCFile> {
        val source = if (type == VirtualFolderType.GALLERY) {
            val mediaFolderPath = preferences.getLastSelectedMediaFolder()
            storageManager.allGalleryItems.filter { it.remotePath.startsWith(mediaFolderPath) }
        } else {
            storageManager.getVirtualFolderContent(type, false)
        }

        return source
            .filter {
                when (mediaState) {
                    MediaState.MEDIA_STATE_PHOTOS_ONLY -> MimeTypeUtil.isImage(it)
                    MediaState.MEDIA_STATE_VIDEOS_ONLY -> MimeTypeUtil.isVideo(it)
                    else -> MimeTypeUtil.isImageOrVideo(it)
                }
            }
            .toMutableList()
    }
}
