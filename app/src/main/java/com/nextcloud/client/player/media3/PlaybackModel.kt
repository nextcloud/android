/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3

import android.content.ComponentName
import android.content.Context
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.nextcloud.client.player.media3.session.MediaSessionFactory
import com.nextcloud.client.player.model.PlaybackSettings
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.PlaybackFiles
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.RepeatMode
import com.nextcloud.client.player.util.PeriodicAction
import com.nextcloud.client.player.util.PlayerUtil.indexOfFirst
import com.nextcloud.client.player.util.PlayerUtil.playbackFile
import com.nextcloud.client.player.util.PlayerUtil.readCurrentFiles
import com.nextcloud.client.player.util.PlayerUtil.readMediaIds
import com.nextcloud.client.player.util.PlayerUtil.setRepeatMode
import com.nextcloud.client.player.util.PlayerUtil.toMediaItem
import com.nextcloud.client.player.util.PlayerUtil.toPlaybackState
import com.nextcloud.client.player.util.PlayerUtil.updateMediaItems
import com.owncloud.android.datamodel.OCFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
@OptIn(markerClass = [UnstableApi::class])
@Suppress("TooManyFunctions")
class PlaybackModel @Inject constructor(
    private val context: Context,
    private val mediaSessionFactory: MediaSessionFactory,
    private val playbackSettings: PlaybackSettings
) {

    companion object {
        private const val CHECK_PROGRESS_INTERVAL = 1000L
    }

    interface Listener {
        fun onPlaybackUpdate(state: PlaybackState)
        fun onPlaybackError(error: Throwable) = Unit
    }

    private val listeners = mutableListOf<Listener>()

    private val checkProgressPeriodicAction = PeriodicAction(CHECK_PROGRESS_INTERVAL) {
        notifyPlaybackUpdate()
    }

    private val playerListener = PlaybackModelPlayerListener(
        checkProgressPeriodicAction,
        this::notifyPlaybackUpdate,
        this::onPlaybackError,
        this::onPlaylistChanged
    )

    private val controllerListener = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            controller.removeListener(playerListener)
            videoSurfaceView = null
            invalidateCurrentFiles()
            controllerScope?.cancel()
            checkProgressPeriodicAction.stop()
            notifyPlaybackUpdate()
        }
    }

    private var controllerScope: CoroutineScope? = null
    private var controller: Player? = null

    private var mediaSession: MediaSession? = null

    var onPictureInPictureClose: (() -> Unit)? = null

    private var videoSurfaceView: SurfaceView? = null

    private var cachedCurrentFiles: List<PlaybackFile>? = null
    private var cachedMediaIds: List<String>? = null

    val state: PlaybackState?
        get() = controller?.toPlaybackState(currentFiles())

    private fun currentFiles(): List<PlaybackFile> = cachedCurrentFiles
        ?: (controller?.readCurrentFiles() ?: emptyList()).also { cachedCurrentFiles = it }

    private fun onPlaylistChanged() {
        val mediaIds = controller?.readMediaIds() ?: emptyList()
        if (mediaIds == cachedMediaIds) return

        cachedMediaIds = mediaIds
        cachedCurrentFiles = null
    }

    private fun invalidateCurrentFiles() {
        cachedMediaIds = null
        cachedCurrentFiles = null
    }

    fun getMediaSession(): MediaSession = mediaSession ?: mediaSessionFactory.create().also {
        mediaSession = it
    }

    suspend fun start() {
        videoSurfaceView = null
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controller = MediaController.Builder(context, sessionToken)
            .setListener(controllerListener)
            .buildAsync()
            .await()
            .apply {
                addListener(playerListener)
                setRepeatMode(playbackSettings.repeatMode)
                shuffleModeEnabled = playbackSettings.isShuffle
                controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> ListenableFuture<T>.await(): T = suspendCancellableCoroutine { cont ->
        addListener(
            {
                try {
                    cont.resume(get())
                } catch (e: ExecutionException) {
                    cont.resumeWithException(e.cause ?: e)
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            },
            Runnable::run
        )

        cont.invokeOnCancellation {
            cancel(false)
        }
    }

    fun setFilesFlow(filesFlow: Flow<PlaybackFiles>) {
        controllerScope?.launch {
            filesFlow
                .catch {
                    notifyPlaybackError(it)
                    release()
                }
                .collectLatest { setFiles(it) }
        }
    }

    fun setFiles(files: PlaybackFiles) {
        if (files.list.isEmpty()) {
            release()
            return
        }

        controller?.let { controller ->
            val currentFile = controller.currentMediaItem?.mediaMetadata?.playbackFile
            val mediaItems = files.list.map { it.toMediaItem() }

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
        .let { if (it in files.list.indices) it else 0 }

    fun release() {
        videoSurfaceView = null
        controller?.release()
        mediaSession?.player?.release()
        mediaSession?.release()
        mediaSession = null
    }

    fun setVideoSurfaceView(surfaceView: SurfaceView?) {
        if (videoSurfaceView === surfaceView) {
            return
        }

        videoSurfaceView = surfaceView
        controller?.setVideoSurfaceView(surfaceView)
    }

    fun addListener(listener: Listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
    }

    fun removeListener(listener: Listener) {
        listeners.remove(listener)
    }

    fun play() {
        controller?.run {
            prepare()
            play()
        }
    }

    fun pause() {
        controller?.pause()
    }

    fun playNext() {
        controller?.run {
            seekToNextMediaItem()
            prepare()
        }
    }

    fun seekToPosition(positionInMilliseconds: Long) {
        controller?.seekTo(positionInMilliseconds)
    }

    fun setRepeatMode(repeatMode: RepeatMode) {
        playbackSettings.setRepeatMode(repeatMode)
        controller?.setRepeatMode(repeatMode)
    }

    fun setShuffle(shuffle: Boolean) {
        playbackSettings.setShuffle(shuffle)
        controller?.shuffleModeEnabled = shuffle
    }

    fun switchToFile(file: PlaybackFile) {
        controller?.run {
            val mediaItemIndex = indexOfFirst { it.mediaId == file.id }
            if (mediaItemIndex >= 0 && mediaItemIndex != currentMediaItemIndex) {
                seekToDefaultPosition(mediaItemIndex)
                prepare()
            }
        }
    }

    fun stopPlaying(file: OCFile) {
        controller?.run {
            val mediaItemIndex = indexOfFirst { it.mediaId == file.localId.toString() }
            if (mediaItemIndex >= 0) {
                release()
            }
        }
    }

    private fun notifyPlaybackUpdate() {
        val currentState = state ?: return
        for (i in listeners.indices) {
            listeners.getOrNull(i)?.onPlaybackUpdate(currentState)
        }
    }

    private fun notifyPlaybackError(error: Throwable) {
        for (i in listeners.indices) {
            listeners.getOrNull(i)?.onPlaybackError(error)
        }
    }

    private fun onPlaybackError(error: Throwable) {
        notifyPlaybackError(error)
        state?.let {
            if (shouldSwitchToNextSource(it)) {
                playNext()
            }
        }
    }

    private fun shouldSwitchToNextSource(state: PlaybackState): Boolean {
        val currentFile = state.currentItemState?.file
        val currentFiles = state.currentFiles
        val oneFileInQueue = currentFiles.size == 1
        val endOfQueue = currentFiles.indexOf(currentFile) == currentFiles.lastIndex
        return !oneFileInQueue && !endOfQueue
    }
}
