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
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.player.media3.resumption.PlaybackResumptionConfigStore
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.model.file.toVirtualFolderType
import com.nextcloud.client.player.ui.audio.AudioPlayerActivity
import com.nextcloud.client.player.util.PlayerUtil.playbackFile
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.ui.preview.PreviewImageActivity
import dagger.android.AndroidInjection
import javax.inject.Inject

@UnstableApi
class PlaybackService : MediaSessionService() {

    companion object {
        private const val SESSION_ACTIVITY_REQUEST_CODE = 1
    }

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var userAccountManager: UserAccountManager

    @Inject
    lateinit var playbackResumptionConfigStore: PlaybackResumptionConfigStore

    private var bindingCount: Int = 0

    private var sessionActivityMediaId: String? = null

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
        val mediaItem = session.player.currentMediaItem?.takeIf { it.mediaId != sessionActivityMediaId } ?: return
        val intent = mediaItem.mediaMetadata.playbackFile?.let(::createSessionIntent) ?: return
        sessionActivityMediaId = mediaItem.mediaId
        session.setSessionActivity(createSessionActivity(intent))
    }

    private fun createSessionIntent(playbackFile: PlaybackFile): Intent? {
        val fileType = PlaybackFileType.entries.firstOrNull {
            playbackFile.mimeType.startsWith(it.value, ignoreCase = true)
        }

        return when (fileType) {
            PlaybackFileType.AUDIO -> AudioPlayerActivity.createIntent(this)
            PlaybackFileType.VIDEO -> createVideoPreviewIntent(playbackFile)
            null -> null
        }
    }

    private fun createVideoPreviewIntent(playbackFile: PlaybackFile): Intent? {
        val user = userAccountManager.user
        val file = playbackFile.id.toLongOrNull()
            ?.let { FileDataStorageManager(user, contentResolver).getFileByLocalId(it) }
            ?: return null
        val virtualFolderType = playbackResumptionConfigStore.loadConfig()?.collection?.toVirtualFolderType()

        return PreviewImageActivity.previewFileIntent(this, user, file).apply {
            putExtra(PreviewImageActivity.EXTRA_VIRTUAL_TYPE, virtualFolderType)
        }
    }

    private fun createSessionActivity(intent: Intent): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(this, SESSION_ACTIVITY_REQUEST_CODE, intent, flags)
    }
}
