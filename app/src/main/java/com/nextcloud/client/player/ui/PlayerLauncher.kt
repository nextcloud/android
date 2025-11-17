/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.player.media3.resumption.PlaybackResumptionConfigStore
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.model.file.PlaybackFiles
import com.nextcloud.client.player.model.file.PlaybackFilesComparator
import com.nextcloud.client.player.model.file.PlaybackFilesRepository
import com.nextcloud.client.player.model.file.toPlaybackFile
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.fragment.SearchType
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject

class PlayerLauncher @Inject constructor(
    private val playbackResumptionConfigStore: PlaybackResumptionConfigStore,
    private val playbackFilesRepository: PlaybackFilesRepository,
    private val playbackModel: PlaybackModel,
    private val logger: Logger
) {
    private var currentLaunchJob: Job? = null

    fun launch(activity: AppCompatActivity, file: OCFile, searchType: SearchType?) {
        currentLaunchJob?.cancel()
        currentLaunchJob = activity.lifecycleScope.launch {
            runCatching {
                val fileType = file.getPlaybackFileType()
                playbackResumptionConfigStore.saveConfig(file.localId.toString(), file.parentId, fileType, searchType)

                val currentPlaybackFile = file.toPlaybackFile()

                playbackModel.start()
                playbackModel.setFiles(PlaybackFiles(listOf(currentPlaybackFile), PlaybackFilesComparator.NONE))
                playbackModel.setFilesFlow(playbackFilesRepository.observe(file.parentId, fileType, searchType))
                playbackModel.play()

                val intent = PlayerActivity.createIntent(activity, fileType)
                activity.startActivity(intent)
            }.onFailure {
                if (it is CancellationException) throw it
                logger.e(PlayerLauncher::class.java.simpleName, "Error launching player", it)
            }
        }
    }

    private fun OCFile.getPlaybackFileType(): PlaybackFileType = PlaybackFileType.entries
        .firstOrNull { mimeType.startsWith(it.value, ignoreCase = true) }
        ?: throw IllegalArgumentException("Unsupported file type: $mimeType")
}
