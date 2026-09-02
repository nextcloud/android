/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.util

import android.app.AppOpsManager
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackItemMetadata
import com.nextcloud.client.player.model.state.PlaybackItemState
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.model.state.PlayerState
import com.nextcloud.client.player.model.state.RepeatMode
import com.nextcloud.client.player.model.state.VideoSize
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Collections

@Suppress("TooManyFunctions")
object PlayerUtil {

    const val REMOTE_FILE_SCHEME = "remoteFile"

    private const val PLAYBACK_FILE_KEY = "playback_file"
    private const val NO_INDEX = -1
    private const val UNKNOWN_CONTENT_LENGTH = -1L
    private const val SECOND_IN_MILLISECONDS = 1000L

    private val playbackJson = Json { ignoreUnknownKeys = true }

    fun PlaybackFile.toMediaItem(): MediaItem = MediaItem
        .Builder()
        .setMediaId(id)
        .setUri(uri)
        .setMediaMetadata(MediaMetadata.Builder().setExtras(this).build())
        .setMimeType(mimeType)
        .build()

    fun MediaMetadata.Builder.setExtras(playbackFile: PlaybackFile): MediaMetadata.Builder = setExtras(
        Bundle().apply {
            putPlaybackFile(PLAYBACK_FILE_KEY, playbackFile)
        }
    )

    val MediaMetadata.playbackFile: PlaybackFile?
        get() = extras.getPlaybackFile(PLAYBACK_FILE_KEY)

    fun Player.indexOfFirst(satisfies: (MediaItem) -> Boolean): Int {
        for (index in 0..<mediaItemCount) {
            val mediaItem = getMediaItemAt(index)
            if (satisfies(mediaItem)) {
                return index
            }
        }
        return NO_INDEX
    }

    fun Player.updateMediaItems(newMediaItems: List<MediaItem>) {
        val oldCurrentMediaItemIndex = currentMediaItemIndex.takeIf { it >= 0 }

        val newCurrentMediaItemIndex = currentMediaItem
            ?.mediaId
            ?.let { currentMediaId -> newMediaItems.indexOfFirst { it.mediaId == currentMediaId } }
            ?.takeIf { it >= 0 }

        if (oldCurrentMediaItemIndex != null && newCurrentMediaItemIndex != null) {
            if (oldCurrentMediaItemIndex < mediaItemCount - 1) {
                removeMediaItems(oldCurrentMediaItemIndex + 1, mediaItemCount)
            }
            if (newCurrentMediaItemIndex < newMediaItems.size - 1) {
                val itemsToAdd = newMediaItems.subList(newCurrentMediaItemIndex + 1, newMediaItems.size)
                addMediaItems(itemsToAdd)
            }
            if (oldCurrentMediaItemIndex > 0) {
                removeMediaItems(0, oldCurrentMediaItemIndex)
            }
            if (newCurrentMediaItemIndex > 0) {
                val itemsToAdd = newMediaItems.subList(0, newCurrentMediaItemIndex)
                addMediaItems(0, itemsToAdd)
            }
            replaceMediaItem(newCurrentMediaItemIndex, newMediaItems[newCurrentMediaItemIndex])
        } else {
            setMediaItems(newMediaItems)
        }
    }

    fun Player.setRepeatMode(mode: RepeatMode) {
        repeatMode = when (mode) {
            RepeatMode.SINGLE -> Player.REPEAT_MODE_ONE
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
        }
    }

    fun Player.toPlaybackState(currentFiles: List<PlaybackFile>): PlaybackState = PlaybackState(
        currentFiles = currentFiles,
        currentItemState = getCurrentItemState(),
        repeatMode = mapRepeatMode(),
        shuffle = shuffleModeEnabled
    )

    fun Player.readMediaIds(): List<String> = buildList {
        for (i in 0 until mediaItemCount) {
            add(getMediaItemAt(i).mediaId)
        }
    }

    fun Player.readCurrentFiles(): List<PlaybackFile> = buildList {
        for (i in 0 until mediaItemCount) {
            getMediaItemAt(i).mediaMetadata.playbackFile?.let(::add)
        }
    }

    private fun Player.getCurrentItemState(): PlaybackItemState? {
        val currentFile = currentMediaItem?.mediaMetadata?.playbackFile ?: return null
        return PlaybackItemState(
            file = currentFile,
            playerState = mapPlayerState(),
            metadata = if (mediaMetadata.playbackFile?.id == currentFile.id) mapMetadata(currentFile) else null,
            videoSize = mapVideoSize(),
            currentTimeInMilliseconds = currentPosition,
            maxTimeInMilliseconds = duration
        )
    }

    private fun Player.mapPlayerState(): PlayerState = when (playbackState) {
        Player.STATE_IDLE -> PlayerState.IDLE
        Player.STATE_ENDED -> PlayerState.COMPLETED
        Player.STATE_BUFFERING, Player.STATE_READY -> if (playWhenReady) PlayerState.PLAYING else PlayerState.PAUSED
        else -> PlayerState.NONE
    }

    private fun Player.mapMetadata(currentFile: PlaybackFile) = PlaybackItemMetadata(
        title = mediaMetadata.title?.takeIf { it.isNotEmpty() } ?: currentFile.getNameWithoutExtension(),
        artist = mediaMetadata.artist,
        album = mediaMetadata.albumTitle,
        genre = mediaMetadata.genre,
        year = mediaMetadata.recordingYear,
        description = mediaMetadata.description,
        artworkData = mediaMetadata.artworkData,
        artworkUri = mediaMetadata.artworkUri?.toString()
    )

    private fun Player.mapVideoSize(): VideoSize? = videoSize
        .takeIf { it.width > 0 && it.height > 0 }
        ?.let { VideoSize(width = it.width, height = it.height) }

    private fun Player.mapRepeatMode(): RepeatMode = when (repeatMode) {
        Player.REPEAT_MODE_ONE -> RepeatMode.SINGLE
        Player.REPEAT_MODE_ALL -> RepeatMode.ALL
        else -> RepeatMode.OFF
    }

    fun OCFile.toPlaybackFile() = PlaybackFile(
        id = localId.toString(),
        uri = getPlaybackUri().toString(),
        name = fileName,
        mimeType = mimeType,
        contentLength = fileLength,
        lastModified = modificationTimestamp,
        isFavorite = isFavorite
    )

    fun OCShare.toPlaybackFile() = PlaybackFile(
        id = fileSource.toString(),
        uri = getPlaybackUri().toString(),
        name = path?.let { File(it).name } ?: "",
        mimeType = getMimeType(),
        contentLength = UNKNOWN_CONTENT_LENGTH,
        lastModified = sharedDate * SECOND_IN_MILLISECONDS,
        isFavorite = isFavorite
    )

    private fun OCShare.getMimeType(): String = mimetype
        ?.takeIf { it.isNotEmpty() }
        ?: path?.let { MimeTypeUtil.getMimeTypeFromPath(it) }
        ?: ""

    fun OCFile.getPlaybackUri(): Uri = getPlaybackUri(localId)

    fun OCShare.getPlaybackUri(): Uri = getPlaybackUri(fileSource)

    fun getPlaybackUri(fileId: Long): Uri = Uri.Builder()
        .scheme(REMOTE_FILE_SCHEME)
        .authority("")
        .appendPath(fileId.toString())
        .build()

    fun Uri.getRemoteFileId(): Long? = scheme
        ?.takeIf { it == REMOTE_FILE_SCHEME }
        ?.let { pathSegments.firstOrNull()?.toLongOrNull() }

    fun Bundle.putPlaybackFile(key: String, playbackFile: PlaybackFile) {
        putString(key, playbackJson.encodeToString(playbackFile))
    }

    fun Bundle?.getPlaybackFile(key: String): PlaybackFile? {
        val encoded = this?.getString(key) ?: return null
        return runCatching { playbackJson.decodeFromString<PlaybackFile>(encoded) }.getOrNull()
    }

    fun ContentResolver.observeContentChanges(uri: Uri, notifyForDescendants: Boolean) = callbackFlow {
        val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(selfChange)
            }
        }
        registerContentObserver(uri, notifyForDescendants, contentObserver)
        awaitClose { unregisterContentObserver(contentObserver) }
    }

    fun Context.isPictureInPictureAllowed(): Boolean {
        val appOpsManager = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
        if (appOpsManager == null || !packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            return false
        }

        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), packageName)
        } else {
            @Suppress("DEPRECATION")
            appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE, Process.myUid(), packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun <T> List<T>.rotate(shift: Int): List<T> {
        val copy = ArrayList(this)
        Collections.rotate(copy, shift)
        return copy
    }

    /**
     * Letterboxes the surface inside its container so the video keeps its aspect ratio.
     */
    fun SurfaceView.applyVideoSize(videoSize: VideoSize) {
        val container = parent as? View
        val containerWidth = container?.width ?: 0
        val containerHeight = container?.height ?: 0
        if (containerWidth <= 0 || containerHeight <= 0) return

        val containerProportion = containerWidth.toFloat() / containerHeight.toFloat()
        val videoProportion = videoSize.width.toFloat() / videoSize.height.toFloat()

        val targetWidth: Int
        val targetHeight: Int
        if (containerProportion < videoProportion) {
            targetWidth = ViewGroup.LayoutParams.MATCH_PARENT
            targetHeight = (containerWidth.toFloat() / videoProportion).toInt()
        } else {
            targetWidth = (videoProportion * containerHeight.toFloat()).toInt()
            targetHeight = ViewGroup.LayoutParams.MATCH_PARENT
        }

        if (layoutParams.width == targetWidth && layoutParams.height == targetHeight) return

        layoutParams = layoutParams.apply {
            width = targetWidth
            height = targetHeight
        }
    }
}
