/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.resumption

import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession.MediaItemsWithStartPosition
import com.nextcloud.client.player.media3.common.MediaItemFactory
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.PlaybackFilesRepository
import com.nextcloud.client.player.model.file.getPlaybackUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import javax.inject.Inject

@UnstableApi
class PlaybackResumptionLauncher @Inject constructor(
    private val playbackResumptionConfigStore: PlaybackResumptionConfigStore,
    private val playbackFilesRepository: PlaybackFilesRepository,
    private val mediaItemFactory: MediaItemFactory,
    private val playbackModel: PlaybackModel
) {

    suspend fun launch(): MediaItemsWithStartPosition = runCatching {
        val (currentFileId, folderId, fileType, searchType) = playbackResumptionConfigStore.loadConfig()
            ?: throw IllegalStateException("Playback resumption config is null")
        val playbackFilesFlow = playbackFilesRepository.observe(folderId, fileType, searchType)
        val playbackFiles = playbackFilesFlow.first().list.ifEmpty {
            throw IllegalStateException("Playback files are empty")
        }
        withContext(Dispatchers.Main) {
            playbackModel.start()
            playbackModel.setFilesFlow(playbackFilesFlow.drop(1))
        }
        playbackFiles.toMediaItemsWithStartPosition(currentFileId)
    }.getOrElse {
        if (it is CancellationException) throw it
        val stubPlaybackFile = getStubPlaybackFile()
        val stubPlaybackFiles = listOf(stubPlaybackFile)
        withContext(Dispatchers.Main) {
            playbackModel.start()
        }
        stubPlaybackFiles.toMediaItemsWithStartPosition(stubPlaybackFile.id)
    }

    private fun List<PlaybackFile>.toMediaItemsWithStartPosition(currentFileId: String) = MediaItemsWithStartPosition(
        map { mediaItemFactory.create(it) },
        indexOfFirst { it.id == currentFileId },
        0
    )

    /**
     * Workaround to avoid internal media3 crash
     */
    private fun getStubPlaybackFile() = PlaybackFile(
        id = "0",
        uri = getPlaybackUri(0L).toString(),
        name = "",
        mimeType = "audio/mpeg",
        contentLength = 0L,
        lastModified = 0L,
        isFavorite = false
    )
}
