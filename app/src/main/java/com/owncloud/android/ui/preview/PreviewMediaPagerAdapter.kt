/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
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
import com.nextcloud.client.player.model.file.PlaybackCollection
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.MimeTypeUtil

class PreviewMediaPagerAdapter(
    fragmentActivity: FragmentActivity,
    files: List<OCFile>,
    private val playbackCollection: PlaybackCollection,
    private val user: User,
    private val selectedFile: OCFile? = null
) : FragmentStateAdapter(fragmentActivity) {

    var autoplayFileId: Long? = null

    private val mediaFiles: MutableList<OCFile> = files.toMutableList()
    private val obsoleteFragments: MutableSet<Any> = HashSet()
    private val obsoletePositions: MutableSet<Int> = HashSet()
    private val downloadErrors: MutableSet<Int> = HashSet()
    private val cachedFragments: SparseArray<FileFragment> = SparseArray()

    fun delete(position: Int) {
        if (position < 0 || position >= mediaFiles.size) {
            return
        }

        cachedFragments[position]?.let {
            obsoleteFragments.add(it)
        }

        obsoletePositions.add(position)

        mediaFiles.removeAt(position)
        downloadErrors.remove(position)
        cachedFragments.remove(position)

        notifyItemRemoved(position)
    }

    @Suppress("TooGenericExceptionCaught")
    fun getFileAt(position: Int): OCFile? = try {
        mediaFiles[position]
    } catch (_: IndexOutOfBoundsException) {
        null
    }

    private fun addVideoOfLivePhoto(file: OCFile) {
        file.livePhotoVideo = selectedFile
    }

    fun getItem(i: Int): Fragment {
        val fragment = fragmentFor(getFileAt(i), i, obsoletePositions.contains(i))
        obsoletePositions.remove(i)
        return fragment
    }

    private fun fragmentFor(file: OCFile?, position: Int, ignoreFirstSavedState: Boolean): Fragment = when {
        file == null -> PreviewImageErrorFragment.newInstance()
        file.isDown -> fragmentForDownloaded(file, ignoreFirstSavedState)
        else -> fragmentForNotDownloaded(file, position, ignoreFirstSavedState)
    }

    private fun fragmentForDownloaded(file: OCFile, ignoreFirstSavedState: Boolean): Fragment =
        if (file.isAudioOrVideo()) {
            PreviewPlaybackFragment.newInstance(file, playbackCollection, takeAutoplay(file))
        } else {
            PreviewImageFragment.newInstance(file, ignoreFirstSavedState, false)
        }

    private fun fragmentForNotDownloaded(file: OCFile, position: Int, ignoreFirstSavedState: Boolean): Fragment {
        addVideoOfLivePhoto(file)

        return when {
            downloadErrors.remove(position) ->
                FileDownloadFragment.newInstance(file, user, true).apply { setError(true) }

            // The FileDownloadFragment is used exclusively for encrypted files, as they cannot be previewed
            // without first being downloaded.
            file.isEncrypted -> FileDownloadFragment.newInstance(file, user, ignoreFirstSavedState)

            file.isAudioOrVideo() -> PreviewPlaybackFragment.newInstance(file, playbackCollection, takeAutoplay(file))

            else -> PreviewImageFragment.newInstance(file, ignoreFirstSavedState, true)
        }
    }

    private fun takeAutoplay(file: OCFile): Boolean {
        if (file.fileId != autoplayFileId) {
            return false
        }

        autoplayFileId = null
        return true
    }

    private fun OCFile.isAudioOrVideo(): Boolean = MimeTypeUtil.isAudio(this) || MimeTypeUtil.isVideo(this)

    fun getFilePosition(file: OCFile): Int = mediaFiles.indexOf(file)

    fun getPositionByLocalId(localId: Long): Int = mediaFiles.indexOfFirst { it.localId == localId }

    fun updateFile(position: Int, file: OCFile) {
        if (position < 0 || position >= mediaFiles.size) {
            return
        }

        cachedFragments[position]?.let { obsoleteFragments.add(it) }
        obsoletePositions.add(position)
        mediaFiles[position] = file
    }

    fun pendingErrorAt(position: Int): Boolean = downloadErrors.contains(position)

    override fun createFragment(position: Int): Fragment = getItem(position)

    override fun getItemCount(): Int = mediaFiles.size

    override fun getItemId(position: Int): Long {
        // The item ID function is needed to detect whether the deletion of the current item needs a UI update
        return mediaFiles.getOrNull(position)?.fileId ?: position.toLong()
    }

    override fun containsItem(itemId: Long): Boolean = mediaFiles.any { it.fileId == itemId }
}
