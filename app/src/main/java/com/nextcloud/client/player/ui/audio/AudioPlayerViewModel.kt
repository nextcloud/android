/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.audio

import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.ui.fileactions.FileAction
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class AudioPlayerViewModel @Inject constructor(
    private val playbackModel: PlaybackModel,
    private val storageManager: FileDataStorageManager,
    private val userAccountManager: UserAccountManager,
    private val backgroundJobManager: BackgroundJobManager,
    private val logger: Logger
) : ViewModel() {

    private val eventChannel = Channel<AudioPlayerScreenEvent>(Channel.BUFFERED)
    val eventFlow: Flow<AudioPlayerScreenEvent> = eventChannel.receiveAsFlow()

    fun onMoreButtonClick() {
        viewModelScope.launch {
            val file = getCurrentOCFile() ?: return@launch
            val actionsToHide = FileAction.getFilePreviewActions(file)
            eventChannel.trySend(AudioPlayerScreenEvent.ShowFileActions(file, actionsToHide))
        }
    }

    fun onFileActionChosen(file: OCFile, actionId: Int) {
        when (actionId) {
            R.id.action_see_details -> eventChannel.trySend(AudioPlayerScreenEvent.ShowFileDetails(file))

            R.id.action_download_file -> startFileDownloading(file)

            R.id.action_export_file -> startFileExport(file)

            R.id.action_send_share_file -> eventChannel.trySend(AudioPlayerScreenEvent.ShowShareFileDialog(file))

            R.id.action_remove_file -> eventChannel.trySend(AudioPlayerScreenEvent.ShowRemoveFileDialog(file))

            R.id.action_open_file_with -> onOpenFileWithClick(file)

            R.id.action_stream_media -> onStreamFileClick(file)

            R.id.action_lock_file -> eventChannel.trySend(
                AudioPlayerScreenEvent.ToggleFileLock(file, shouldBeLocked = true)
            )

            R.id.action_unlock_file -> eventChannel.trySend(
                AudioPlayerScreenEvent.ToggleFileLock(file, shouldBeLocked = false)
            )

            R.id.action_add_to_album -> eventChannel.trySend(AudioPlayerScreenEvent.AddFileToAlbum(file))
        }
    }

    private suspend fun getCurrentOCFile(): OCFile? {
        val currentFileId = playbackModel.state?.currentItemState?.file?.id
        return currentFileId
            ?.takeIf { it.isDigitsOnly() }
            ?.let { getOCFile(it.toLong()) }
    }

    private suspend fun getOCFile(localId: Long): OCFile? = withContext(Dispatchers.IO) {
        runCatching {
            storageManager.getFileByLocalId(localId)
        }.getOrElse {
            if (it is CancellationException) throw it
            logger.e(AudioPlayerViewModel::class.java.simpleName, "Failed to get file by localId: $localId", it)
            null
        }
    }

    private fun startFileDownloading(file: OCFile) {
        val user = userAccountManager.user
        FileDownloadHelper.instance().downloadFileIfNotStartedBefore(user, file)
    }

    private fun startFileExport(file: OCFile) {
        backgroundJobManager.startImmediateFilesExportJob(listOf(file))
        eventChannel.trySend(AudioPlayerScreenEvent.ShowFileExportStartedMessage)
    }

    private fun onOpenFileWithClick(file: OCFile) {
        playbackModel.pause()
        eventChannel.trySend(AudioPlayerScreenEvent.LaunchOpenFileIntent(file))
    }

    private fun onStreamFileClick(file: OCFile) {
        playbackModel.pause()
        eventChannel.trySend(AudioPlayerScreenEvent.LaunchStreamFileIntent(file))
    }
}
