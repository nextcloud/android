/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.player.media3.resumption.PlaybackResumptionConfigStore
import com.nextcloud.client.player.model.file.PlaybackCollection
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.model.file.PlaybackFiles
import com.nextcloud.client.player.model.file.PlaybackFilesComparator
import com.nextcloud.client.player.model.file.PlaybackFilesRepository
import com.nextcloud.client.player.util.PlayerUtil.toPlaybackFile
import com.nextcloud.utils.extensions.resolveMimeType
import com.owncloud.android.datamodel.OCFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject

class PlaybackQueueLoader @Inject constructor(
    private val playbackResumptionConfigStore: PlaybackResumptionConfigStore,
    private val playbackFilesRepository: PlaybackFilesRepository,
    private val playbackModel: PlaybackModel,
    private val logger: Logger
) {
    private var loadJob: Job? = null

    fun load(owner: LifecycleOwner, file: OCFile, collection: PlaybackCollection, onLoaded: () -> Unit = {}) {
        loadJob?.cancel()
        loadJob = owner.lifecycleScope.launch {
            runCatching {
                loadQueue(file, collection)
                onLoaded()
            }.onFailure {
                if (it is CancellationException) throw it
                logger.e(PlaybackQueueLoader::class.java.simpleName, "Error loading playback queue", it)
            }
        }
    }

    private suspend fun loadQueue(file: OCFile, collection: PlaybackCollection) {
        val fileType = PlaybackFileType.ofMimeType(file.resolveMimeType())
        playbackResumptionConfigStore.saveConfig(file.localId.toString(), file.parentId, fileType, collection)

        playbackModel.start()
        playbackModel.setFiles(PlaybackFiles(listOf(file.toPlaybackFile()), PlaybackFilesComparator.NONE))
        playbackModel.setFilesFlow(playbackFilesRepository.observe(file.parentId, fileType, collection))
    }
}
