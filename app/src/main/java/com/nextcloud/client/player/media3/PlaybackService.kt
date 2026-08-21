/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.media3

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService
import com.nextcloud.client.player.media3.common.playbackFile
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.ui.PlayerActivity
import dagger.android.AndroidInjection
import javax.inject.Inject

@UnstableApi
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var playbackModel: PlaybackModel

    private var bindingCount: Int = 0

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
        setMediaNotificationProvider(MediaNotificationProvider(this))
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession? = playbackModel.getMediaSession()

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        createSessionActivity(session.player.currentMediaItem)?.let(session::setSessionActivity)
        super.onUpdateNotification(session, startInForegroundRequired)
    }

    override fun onBind(intent: Intent?): IBinder? {
        val result = super.onBind(intent)
        if (result != null) {
            bindingCount++
        }
        return result
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bindingCount--
        if (bindingCount == 0) {
            stopSelf()
        }
        return super.onUnbind(intent)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        playbackModel.release()
        stopSelf()
    }

    override fun onDestroy() {
        playbackModel.release()
        super.onDestroy()
    }

    private fun createSessionActivity(currentMediaItem: MediaItem?): PendingIntent? {
        val currentFile = currentMediaItem?.mediaMetadata?.playbackFile ?: return null
        val fileType = PlaybackFileType.entries
            .firstOrNull { currentFile.mimeType.startsWith(it.value, ignoreCase = true) }
            ?: throw IllegalArgumentException("Unsupported file type: ${currentFile.mimeType}")

        val intent = PlayerActivity.createIntent(this, fileType)
        val requestCode = System.currentTimeMillis().toInt()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }
}
