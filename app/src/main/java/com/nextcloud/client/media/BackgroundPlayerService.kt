/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Parneet Singh <gurayaparneet@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.media

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT
import androidx.media3.common.Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS
import androidx.media3.common.Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.DefaultMediaNotificationProvider.COMMAND_KEY_COMPACT_VIEW_INDEX
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
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

@OptIn(UnstableApi::class)
class BackgroundPlayerService :
    MediaSessionService(),
    Injectable {

    private val seekBackSessionCommand = SessionCommand(SESSION_COMMAND_ACTION_SEEK_BACK, Bundle.EMPTY)
    private val seekForwardSessionCommand = SessionCommand(SESSION_COMMAND_ACTION_SEEK_FORWARD, Bundle.EMPTY)

    val seekForward =
        CommandButton.Builder()
            .setDisplayName("Seek Forward")
            .setIconResId(CommandButton.getIconResIdForIconConstant(CommandButton.ICON_SKIP_FORWARD_15))
            .setSessionCommand(seekForwardSessionCommand)
            .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 2) })
            .build()

    val seekBackward =
        CommandButton.Builder()
            .setDisplayName("Seek Backward")
            .setIconResId(CommandButton.getIconResIdForIconConstant(CommandButton.ICON_SKIP_BACK_5))
            .setSessionCommand(seekBackSessionCommand)
            .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 0) })
            .build()

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var userAccountManager: UserAccountManager
    lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RELEASE_MEDIA_SESSION_BROADCAST_ACTION -> release()
                STOP_MEDIA_SESSION_BROADCAST_ACTION -> exoPlayer.stop()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        registerBroadcastReceiver(
            stopReceiver,
            IntentFilter().apply {
                addAction(RELEASE_MEDIA_SESSION_BROADCAST_ACTION)
                addAction(STOP_MEDIA_SESSION_BROADCAST_ACTION)
            },
            ReceiverFlag.NotExported
        )

        MainApp.getAppComponent().inject(this)
        initNextcloudExoPlayer()

        setMediaNotificationProvider(object : DefaultMediaNotificationProvider(this) {
            override fun getMediaButtons(
                session: MediaSession,
                playerCommands: Player.Commands,
                customLayout: ImmutableList<CommandButton>,
                showPauseButton: Boolean
            ): ImmutableList<CommandButton> {
                val playPauseButton =
                    CommandButton.Builder()
                        .setDisplayName("PlayPause")
                        .setIconResId(
                            CommandButton.getIconResIdForIconConstant(
                                if (mediaSession?.player?.isPlaying == true) {
                                    CommandButton.ICON_PAUSE
                                } else {
                                    CommandButton.ICON_PLAY
                                }
                            )
                        )
                        .setPlayerCommand(COMMAND_PLAY_PAUSE)
                        .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 1) })
                        .build()

                val myCustomButtonsLayout =
                    ImmutableList.of(seekBackward, playPauseButton, seekForward)
                return myCustomButtonsLayout
            }
        })
    }

    private fun initNextcloudExoPlayer() {
        runBlocking {
            var nextcloudClient: NextcloudClient
            withContext(Dispatchers.IO) {
                nextcloudClient = clientFactory.createNextcloudClient(userAccountManager.user)
            }
            nextcloudClient.let {
                exoPlayer = createNextcloudExoplayer(this@BackgroundPlayerService, nextcloudClient)
                mediaSession =
                    MediaSession.Builder(applicationContext, exoPlayer)
                        // set id to distinct this session to avoid crash
                        // in case session release delayed a bit and
                        // we start another session for eg. video
                        .setId(BACKGROUND_MEDIA_SESSION_ID)
                        .setCustomLayout(listOf(seekBackward, seekForward))
                        .setCallback(object : MediaSession.Callback {
                            override fun onConnect(
                                session: MediaSession,
                                controller: MediaSession.ControllerInfo
                            ): ConnectionResult = AcceptedResultBuilder(mediaSession!!)
                                .setAvailablePlayerCommands(
                                    ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                                        .remove(COMMAND_SEEK_TO_NEXT)
                                        .remove(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                                        .remove(COMMAND_SEEK_TO_PREVIOUS)
                                        .remove(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                                        .build()
                                )
                                .setAvailableSessionCommands(
                                    ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                                        .addSessionCommands(
                                            listOf(seekBackSessionCommand, seekForwardSessionCommand)
                                        ).build()
                                )
                                .build()

                            override fun onPostConnect(session: MediaSession, controller: MediaSession.ControllerInfo) {
                                session.setCustomLayout(listOf(seekBackward, seekForward))
                            }

                            override fun onCustomCommand(
                                session: MediaSession,
                                controller: MediaSession.ControllerInfo,
                                customCommand: SessionCommand,
                                args: Bundle
                            ): ListenableFuture<SessionResult> = when (customCommand.customAction) {
                                SESSION_COMMAND_ACTION_SEEK_FORWARD -> {
                                    session.player.seekForward()
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                SESSION_COMMAND_ACTION_SEEK_BACK -> {
                                    session.player.seekBack()
                                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                                }

                                else -> super.onCustomCommand(session, controller, customCommand, args)
                            }
                        })
                        .build()
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        release()
    }

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    private fun release() {
        val player = mediaSession?.player
        if (player?.playWhenReady == true) {
            // Make sure the service is not in foreground.
            player.pause()
        }
        // Bug in Android 14, https://github.com/androidx/media/issues/805
        // that sometimes onTaskRemove() doesn't get called immediately
        // eventually gets called so the service stops but the notification doesn't clear out.
        // [WORKAROUND] So, explicitly removing the notification here.
        // TODO revisit after bug solved!
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
        stopSelf()
    }

    override fun onGetSession(p0: MediaSession.ControllerInfo): MediaSession? = mediaSession

    companion object {
        private const val SESSION_COMMAND_ACTION_SEEK_BACK = "SESSION_COMMAND_ACTION_SEEK_BACK"
        private const val SESSION_COMMAND_ACTION_SEEK_FORWARD = "SESSION_COMMAND_ACTION_SEEK_FORWARD"

        private const val BACKGROUND_MEDIA_SESSION_ID = "com.nextcloud.client.media.BACKGROUND_MEDIA_SESSION_ID"

        const val RELEASE_MEDIA_SESSION_BROADCAST_ACTION = "com.nextcloud.client.media.RELEASE_MEDIA_SESSION"
        const val STOP_MEDIA_SESSION_BROADCAST_ACTION = "com.nextcloud.client.media.STOP_MEDIA_SESSION"
    }
}
