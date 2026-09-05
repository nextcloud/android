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
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.ui.PlayerActivity
import com.nextcloud.client.player.util.PlayerUtil.playbackFile
import dagger.android.AndroidInjection
import javax.inject.Inject

@UnstableApi
class PlaybackService : MediaSessionService() {

    companion object {
        private const val SESSION_ACTIVITY_REQUEST_CODE = 1
    }

    @Inject
    lateinit var playbackModel: PlaybackModel

    private var bindingCount: Int = 0

    private var sessionActivityFileType: PlaybackFileType? = null

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
        setMediaNotificationProvider(MediaNotificationProvider(this))
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession? = playbackModel.getMediaSession()

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        updateSessionActivity(session)
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

    private fun updateSessionActivity(session: MediaSession) {
        val fileType = session.player.currentMediaItem.playbackFileType() ?: return
        if (fileType == sessionActivityFileType) {
            return
        }

        sessionActivityFileType = fileType
        session.setSessionActivity(createSessionActivity(fileType))
    }

    private fun MediaItem?.playbackFileType(): PlaybackFileType? {
        val mimeType = this?.mediaMetadata?.playbackFile?.mimeType ?: return null
        return PlaybackFileType.entries.firstOrNull { mimeType.startsWith(it.value, ignoreCase = true) }
    }

    private fun createSessionActivity(fileType: PlaybackFileType): PendingIntent {
        val intent = PlayerActivity.createIntent(this, fileType)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(this, SESSION_ACTIVITY_REQUEST_CODE, intent, flags)
    }
}
