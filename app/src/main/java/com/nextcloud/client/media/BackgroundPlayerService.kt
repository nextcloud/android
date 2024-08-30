/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Parneet Singh <gurayaparneet@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.media.NextcloudExoPlayer.createNextcloudExoplayer
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.extensions.registerBroadcastReceiver
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.ReceiverFlag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BackgroundPlayerService : MediaSessionService(), Injectable {

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var userAccountManager: UserAccountManager
    lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            release()
        }
    }

    override fun onCreate() {
        super.onCreate()

        registerBroadcastReceiver(
            stopReceiver,
            IntentFilter(STOP_MEDIA_SESSION_BROADCAST_ACTION),
            ReceiverFlag.NotExported
        )

        MainApp.getAppComponent().inject(this)
        initNextcloudExoPlayer()
    }

    private fun initNextcloudExoPlayer() {
        runBlocking {
            var nextcloudClient: NextcloudClient
            withContext(Dispatchers.IO) {
                nextcloudClient = clientFactory.createNextcloudClient(userAccountManager.user)
            }
            nextcloudClient?.let {
                exoPlayer = createNextcloudExoplayer(this@BackgroundPlayerService, nextcloudClient)
                mediaSession =
                    MediaSession.Builder(applicationContext, exoPlayer).build()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        unregisterReceiver(stopReceiver)
        val player = mediaSession?.player
        if (player!!.playWhenReady) {
            // Make sure the service is not in foreground.
            player.pause()
        }
        release()
    }

    private fun release() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        stopSelf()
    }

    override fun onGetSession(p0: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    companion object {
        const val STOP_MEDIA_SESSION_BROADCAST_ACTION = "com.nextcloud.client.media.STOP_MEDIA_SESSION"
    }
}
