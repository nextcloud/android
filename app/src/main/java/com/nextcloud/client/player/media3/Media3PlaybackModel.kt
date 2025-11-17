/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3

import android.view.SurfaceView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import com.nextcloud.client.player.media3.common.MediaItemFactory
import com.nextcloud.client.player.media3.common.playbackFile
import com.nextcloud.client.player.media3.controller.MediaControllerFactory
import com.nextcloud.client.player.media3.controller.indexOfFirst
import com.nextcloud.client.player.media3.controller.setRepeatMode
import com.nextcloud.client.player.media3.controller.updateMediaItems
import com.nextcloud.client.player.media3.session.MediaSessionFactory
import com.nextcloud.client.player.media3.session.MediaSessionHolder
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.PlaybackModelCompositeListener
import com.nextcloud.client.player.model.PlaybackSettings
import com.nextcloud.client.player.model.error.PlaybackErrorStrategy
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.PlaybackFiles
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.RepeatMode
import com.nextcloud.client.player.util.PeriodicAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Optional
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@UnstableApi
@Suppress("LongParameterList")
class Media3PlaybackModel @Inject constructor(
    private val stateFactory: PlaybackStateFactory,
    private val mediaSessionFactory: MediaSessionFactory,
    private val controllerFactory: MediaControllerFactory,
    private val playbackSettings: PlaybackSettings,
    private val mediaItemFactory: MediaItemFactory,
    private val playbackErrorStrategy: PlaybackErrorStrategy
) : PlaybackModel,
    MediaSessionHolder {

    companion object {
        private const val CHECK_PROGRESS_INTERVAL = 1000L
    }

    private val modelCompositeListener = PlaybackModelCompositeListener()

    private val checkProgressPeriodicAction = PeriodicAction(CHECK_PROGRESS_INTERVAL) {
        state.ifPresent(modelCompositeListener::onPlaybackUpdate)
    }

    private val playerListener = PlaybackModelPlayerListener(
        checkProgressPeriodicAction,
        this::onPlaybackUpdate,
        this::onPlaybackError
    )

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            controller.removeListener(playerListener)
            controllerScope?.cancel()
            checkProgressPeriodicAction.stop()
            state.ifPresent(modelCompositeListener::onPlaybackUpdate)
        }
    }

    private var controllerScope: CoroutineScope? = null
    private var controller: Player? = null

    private var mediaSession: MediaSession? = null

    override val state: Optional<PlaybackState>
        get() {
            return stateFactory.create(controller)
        }

    override fun getMediaSession(): MediaSession = mediaSession ?: mediaSessionFactory.create().also {
        mediaSession = it
    }

    override suspend fun start() {
        controller = controllerFactory.create(controllerListener).apply {
            addListener(playerListener)
            setRepeatMode(playbackSettings.repeatMode)
            shuffleModeEnabled = playbackSettings.isShuffle
            controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        }
    }

    override fun setFilesFlow(filesFlow: Flow<PlaybackFiles>) {
        controllerScope?.launch {
            filesFlow
                .catch {
                    modelCompositeListener.onPlaybackError(it)
                    release()
                }
                .collectLatest { setFiles(it) }
        }
    }

    override fun setFiles(files: PlaybackFiles) {
        if (files.list.isEmpty()) {
            release()
            return
        }

        controller?.let { controller ->
            val currentFile = controller.currentMediaItem?.mediaMetadata?.playbackFile
            val mediaItems = files.list.map(mediaItemFactory::create)

            if (currentFile == null) {
                controller.setMediaItems(mediaItems)
            } else if (files.list.any { it.id == currentFile.id }) {
                controller.updateMediaItems(mediaItems)
            } else {
                val nextFileIndex = getNextFileIndex(files, currentFile)
                controller.setMediaItems(mediaItems, nextFileIndex, 0)
            }

            controller.prepare()
        }
    }

    private fun getNextFileIndex(files: PlaybackFiles, currentFile: PlaybackFile): Int = (files.list + currentFile)
        .sortedWith(files.comparator)
        .indexOfFirst { it.id == currentFile.id }
        .let { if (it in 0..files.list.lastIndex) it else 0 }

    override fun release() {
        controller?.release()
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
    }

    override fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        controller?.setVideoSurfaceView(surfaceView)
    }

    override fun addListener(listener: PlaybackModel.Listener) {
        modelCompositeListener.addListener(listener)
    }

    override fun removeListener(listener: PlaybackModel.Listener) {
        modelCompositeListener.removeListener(listener)
    }

    override fun play() {
        controller?.run {
            prepare()
            play()
        }
    }

    override fun pause() {
        controller?.pause()
    }

    override fun playNext() {
        controller?.run {
            seekToNextMediaItem()
            prepare()
        }
    }

    override fun playPrevious() {
        controller?.run {
            seekToPreviousMediaItem()
            prepare()
        }
    }

    override fun seekToPosition(positionInMilliseconds: Long) {
        controller?.seekTo(positionInMilliseconds)
    }

    override fun setRepeatMode(repeatMode: RepeatMode) {
        playbackSettings.setRepeatMode(repeatMode)
        controller?.setRepeatMode(repeatMode)
    }

    override fun setShuffle(shuffle: Boolean) {
        playbackSettings.setShuffle(shuffle)
        controller?.shuffleModeEnabled = shuffle
    }

    override fun switchToFile(file: PlaybackFile) {
        controller?.run {
            val mediaItemIndex = indexOfFirst { it.mediaId == file.id }
            if (mediaItemIndex >= 0 && mediaItemIndex != currentMediaItemIndex) {
                seekToDefaultPosition(mediaItemIndex)
                prepare()
            }
        }
    }

    private fun onPlaybackUpdate() {
        state.ifPresent(modelCompositeListener::onPlaybackUpdate)
    }

    private fun onPlaybackError(error: Throwable) {
        modelCompositeListener.onPlaybackError(error)
        state.ifPresent { state ->
            if (playbackErrorStrategy.switchToNextSource(error, state)) {
                playNext()
            }
        }
    }
}
