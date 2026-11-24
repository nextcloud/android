/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.audio

import androidx.annotation.IdRes
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun onFileActionChosen(@IdRes itemId: Int) {
        when (itemId) {
            R.id.action_open_file_with, R.id.action_stream_media -> playbackModel.pause()
            R.id.action_export_file -> eventChannel.trySend(AudioPlayerScreenEvent.ShowFileExportStartedMessage)
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
}
