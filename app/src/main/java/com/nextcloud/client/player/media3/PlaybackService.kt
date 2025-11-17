/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3

import android.content.Intent
import android.os.IBinder
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.MediaSessionService
import com.nextcloud.client.player.media3.session.MediaSessionActivityFactory
import com.nextcloud.client.player.media3.session.MediaSessionHolder
import dagger.android.AndroidInjection
import javax.inject.Inject

@UnstableApi
class PlaybackService : MediaSessionService() {

    @Inject
    lateinit var mediaSessionHolder: MediaSessionHolder

    @Inject
    lateinit var mediaSessionActivityFactory: MediaSessionActivityFactory

    private var bindingCount: Int = 0

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
        setMediaNotificationProvider(MediaNotificationProvider(this))
    }

    override fun onGetSession(controllerInfo: ControllerInfo): MediaSession? = mediaSessionHolder.getMediaSession()

    override fun onUpdateNotification(session: MediaSession, startInForegroundRequired: Boolean) {
        val currentMediaItem = session.player.currentMediaItem
        mediaSessionActivityFactory.create(currentMediaItem)?.let(session::setSessionActivity)
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
        mediaSessionHolder.release()
        stopSelf()
    }

    override fun onDestroy() {
        mediaSessionHolder.release()
        super.onDestroy()
    }
}
