/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.preview

import android.util.SparseArray
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.nextcloud.client.account.User
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.VirtualFolderType
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.FileSortOrder
import com.owncloud.android.utils.FileStorageUtils

/**
 * Adapter class that provides Fragment instances
 */
class PreviewMediaPagerAdapter : FragmentStateAdapter {

    private var selectedFile: OCFile? = null
    private var imageFiles: MutableList<OCFile> = mutableListOf()
    private val user: User
    private val mObsoleteFragments: MutableSet<Any>
    private val mObsoletePositions: MutableSet<Int>
    private val mDownloadErrors: MutableSet<Int>
    private val mStorageManager: FileDataStorageManager
    private val mCachedFragments: SparseArray<FileFragment>

    /**
     * Constructor
     *
     * @param fragmentActivity [FragmentActivity] instance that will handle the [Fragment]s provided by the
     * adapter.
     * @param parentFolder    Folder where images will be searched for.
     * @param storageManager  Bridge to database.
     */
    @Suppress("LongParameterList")
    constructor(
        fragmentActivity: FragmentActivity,
        selectedFile: OCFile?,
        parentFolder: OCFile?,
        user: User,
        storageManager: FileDataStorageManager,
        onlyOnDevice: Boolean,
        preferences: AppPreferences
    ) : super(fragmentActivity) {
        requireNotNull(parentFolder) { "NULL parent folder" }

        this.user = user
        this.selectedFile = selectedFile
        mStorageManager = storageManager
        imageFiles = mStorageManager.getFolderImagesAndVideos(parentFolder, onlyOnDevice)

        val sortOrder = preferences.getSortOrderByFolder(parentFolder)
        val foldersBeforeFiles = preferences.isSortFoldersBeforeFiles()
        val favoritesFirst = preferences.isSortFavoritesFirst()
        imageFiles = sortOrder.sortCloudFiles(imageFiles.toMutableList(), foldersBeforeFiles, favoritesFirst)

        mObsoleteFragments = HashSet()
        mObsoletePositions = HashSet()
        mDownloadErrors = HashSet()
        mCachedFragments = SparseArray()
    }

    /**
     * Constructor
     *
     * @param fragmentActivity [FragmentActivity] instance that will handle the [Fragment]s provided by the
     * adapter.
     * @param type            Type of virtual folder, e.g. favorite or photos
     * @param storageManager  Bridge to database.
     */
    constructor(
        fragmentActivity: FragmentActivity,
        type: VirtualFolderType?,
        user: User,
        storageManager: FileDataStorageManager,
        preferences: AppPreferences
    ) : super(fragmentActivity) {
        requireNotNull(type) { "NULL parent folder" }
        require(type != VirtualFolderType.NONE) { "NONE virtual folder type" }

        this.user = user
        mStorageManager = storageManager

        when (type) {
            VirtualFolderType.GALLERY -> {
                imageFiles = mStorageManager.allGalleryItems
                imageFiles = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(imageFiles)
            }

            VirtualFolderType.ALBUM -> {
                imageFiles = mStorageManager.getVirtualFolderContent(type, false)
                imageFiles = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(imageFiles)
            }

            else -> {
                imageFiles = mStorageManager.getVirtualFolderContent(type, true)
            }
        }

        if (type == VirtualFolderType.FAVORITE) {
            val sortOrder = preferences.getSortOrderByType(FileSortOrder.Type.favoritesListView)
            val foldersBeforeFiles = preferences.isSortFoldersBeforeFiles()
            val favoritesFirst = preferences.isSortFavoritesFirst()
            imageFiles = sortOrder.sortCloudFiles(imageFiles.toMutableList(), foldersBeforeFiles, favoritesFirst)
        }

        mObsoleteFragments = HashSet()
        mObsoletePositions = HashSet()
        mDownloadErrors = HashSet()
        mCachedFragments = SparseArray()
    }

    fun delete(position: Int) {
        if (position < 0 || position >= imageFiles.size) {
            return
        }

        mCachedFragments[position]?.let {
            mObsoleteFragments.add(it)
        }

        mObsoletePositions.add(position)

        imageFiles.removeAt(position)
        mDownloadErrors.remove(position)
        mCachedFragments.remove(position)

        notifyItemRemoved(position)
    }

    @Suppress("TooGenericExceptionCaught")
    fun getFileAt(position: Int): OCFile? = try {
        imageFiles[position]
    } catch (_: IndexOutOfBoundsException) {
        null
    }

    private fun addVideoOfLivePhoto(file: OCFile) {
        file.livePhotoVideo = selectedFile
    }

    fun getItem(i: Int): Fragment {
        val fragment = fragmentFor(getFileAt(i), i, mObsoletePositions.contains(i))
        mObsoletePositions.remove(i)
        return fragment
    }

    private fun fragmentFor(file: OCFile?, position: Int, ignoreFirstSavedState: Boolean): Fragment = when {
        file == null -> PreviewImageErrorFragment.newInstance()
        file.isDown -> fragmentForDownloaded(file, ignoreFirstSavedState)
        else -> fragmentForNotDownloaded(file, position, ignoreFirstSavedState)
    }

    private fun fragmentForDownloaded(file: OCFile, ignoreFirstSavedState: Boolean): Fragment =
        if (PreviewMediaFragment.isAudioOrVideo(file)) {
            PreviewMediaFragment.newInstance(file, user)
        } else {
            PreviewImageFragment.newInstance(file, ignoreFirstSavedState, false)
        }

    private fun fragmentForNotDownloaded(file: OCFile, position: Int, ignoreFirstSavedState: Boolean): Fragment {
        addVideoOfLivePhoto(file)

        return when {
            mDownloadErrors.remove(position) ->
                FileDownloadFragment.newInstance(file, user, true).apply { setError(true) }

            // The FileDownloadFragment is used exclusively for encrypted files, as they cannot be previewed
            // without first being downloaded.
            file.isEncrypted -> FileDownloadFragment.newInstance(file, user, ignoreFirstSavedState)

            PreviewMediaFragment.isAudioOrVideo(file) ->
                PreviewMediaFragment.newInstance(file, user)

            else -> PreviewImageFragment.newInstance(file, ignoreFirstSavedState, true)
        }
    }

    fun getFilePosition(file: OCFile): Int = imageFiles.indexOf(file)

    fun updateFile(position: Int, file: OCFile) {
        val fragmentToUpdate = mCachedFragments[position]
        if (fragmentToUpdate != null) {
            mObsoleteFragments.add(fragmentToUpdate)
        }
        mObsoletePositions.add(position)
        imageFiles[position] = file
    }

    fun pendingErrorAt(position: Int): Boolean = mDownloadErrors.contains(position)

    override fun createFragment(position: Int): Fragment = getItem(position)

    override fun getItemCount(): Int = imageFiles.size

    override fun getItemId(position: Int): Long {
        // The item ID function is needed to detect whether the deletion of the current item needs a UI update
        return imageFiles.getOrNull(position)?.fileId ?: position.toLong()
    }

    override fun containsItem(itemId: Long): Boolean = imageFiles.any { it.fileId == itemId }
}
