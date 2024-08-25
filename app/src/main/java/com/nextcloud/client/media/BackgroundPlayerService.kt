/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.media

import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.media.NextcloudExoPlayer.createNextcloudExoplayer
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.MainApp
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

    override fun onCreate() {
        super.onCreate()
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
                println(exoPlayer)
                mediaSession =
                    MediaSession.Builder(applicationContext, exoPlayer).setCallback(object : MediaSession.Callback {
                        override fun onDisconnected(session: MediaSession, controller: MediaSession.ControllerInfo) {
                            stopSelf()
                        }
                    }).build()
            }
            println("created client $nextcloudClient")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player!!.playWhenReady) {
            // Make sure the service is not in foreground.
            player.pause()
        }
        stopSelf()
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(p0: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }
}
