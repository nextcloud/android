/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.media3.resumption.PlaybackResumptionConfigStore
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.model.file.PlaybackFiles
import com.nextcloud.client.player.model.file.PlaybackFilesComparator
import com.nextcloud.client.player.model.file.PlaybackFilesRepository
import com.nextcloud.client.player.util.PlayerUtil.toPlaybackFile
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

    /**
     * Starts playback and opens [PlayerActivity] on top of [activity].
     */
    fun launch(activity: AppCompatActivity, file: OCFile, searchType: SearchType?) {
        run(activity) {
            val fileType = prepareQueue(file, searchType)
            playbackModel.play()
            activity.startActivity(PlayerActivity.createIntent(activity, fileType))
        }
    }

    /**
     * Loads [file] and the media of its collection into the player without opening [PlayerActivity], so that a host
     * screen can render the playback itself.
     */
    fun prepare(owner: LifecycleOwner, file: OCFile, searchType: SearchType?, autoplay: Boolean = false) {
        run(owner) {
            prepareQueue(file, searchType)
            if (autoplay) {
                playbackModel.play()
            }
        }
    }

    private fun run(owner: LifecycleOwner, block: suspend () -> Unit) {
        currentLaunchJob?.cancel()
        currentLaunchJob = owner.lifecycleScope.launch {
            runCatching { block() }.onFailure {
                if (it is CancellationException) throw it
                logger.e(PlayerLauncher::class.java.simpleName, "Error launching player", it)
            }
        }
    }

    private suspend fun prepareQueue(file: OCFile, searchType: SearchType?): PlaybackFileType {
        val fileType = PlaybackFileType.ofMimeType(file.mimeType)
        playbackResumptionConfigStore.saveConfig(file.localId.toString(), file.parentId, fileType, searchType)

        playbackModel.start()
        playbackModel.setFiles(PlaybackFiles(listOf(file.toPlaybackFile()), PlaybackFilesComparator.NONE))
        playbackModel.setFilesFlow(playbackFilesRepository.observe(file.parentId, fileType, searchType))
        return fileType
    }
}
