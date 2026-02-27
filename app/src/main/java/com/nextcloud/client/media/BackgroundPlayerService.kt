/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
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
import com.owncloud.android.R
import com.owncloud.android.datamodel.ReceiverFlag
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(UnstableApi::class)
class BackgroundPlayerService :
    MediaSessionService(),
    Injectable {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val seekBackSessionCommand = SessionCommand(SESSION_COMMAND_ACTION_SEEK_BACK, Bundle.EMPTY)
    private val seekForwardSessionCommand = SessionCommand(SESSION_COMMAND_ACTION_SEEK_FORWARD, Bundle.EMPTY)

    private lateinit var seekForward: CommandButton
    private lateinit var seekBackward: CommandButton

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var userAccountManager: UserAccountManager

    private lateinit var exoPlayer: ExoPlayer
    private var mediaSession: MediaSession? = null

    private var isPlayerReady = false

    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                RELEASE_MEDIA_SESSION_BROADCAST_ACTION -> release()
                STOP_MEDIA_SESSION_BROADCAST_ACTION -> {
                    if (isPlayerReady) {
                        exoPlayer.stop()
                    } else {
                        stopSelf()
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onCreate() {
        super.onCreate()

        MainApp.getAppComponent().inject(this)

        seekForward = CommandButton.Builder()
            .setDisplayName(getString(R.string.media_player_seek_forward))
            .setIconResId(CommandButton.getIconResIdForIconConstant(CommandButton.ICON_SKIP_FORWARD_15))
            .setSessionCommand(seekForwardSessionCommand)
            .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 2) })
            .build()

        seekBackward = CommandButton.Builder()
            .setDisplayName(getString(R.string.media_player_seek_backward))
            .setIconResId(CommandButton.getIconResIdForIconConstant(CommandButton.ICON_SKIP_BACK_15))
            .setSessionCommand(seekBackSessionCommand)
            .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 0) })
            .build()

        exoPlayer = ExoPlayer.Builder(this).build()
        mediaSession = buildMediaSession(exoPlayer)

        setMediaNotificationProvider(buildNotificationProvider())

        registerBroadcastReceiver(
            stopReceiver,
            IntentFilter().apply {
                addAction(RELEASE_MEDIA_SESSION_BROADCAST_ACTION)
                addAction(STOP_MEDIA_SESSION_BROADCAST_ACTION)
            },
            ReceiverFlag.NotExported
        )

        initExoPlayer()
    }

    private fun initExoPlayer() {
        serviceScope.launch {
            try {
                val nextcloudClient: NextcloudClient = withContext(Dispatchers.IO) {
                    clientFactory.createNextcloudClient(userAccountManager.user)
                }

                val realPlayer = createNextcloudExoplayer(this@BackgroundPlayerService, nextcloudClient)

                exoPlayer.release()
                exoPlayer = realPlayer
                isPlayerReady = true

                // Update the session to use the real player
                mediaSession?.player = realPlayer
            } catch (e: Exception) {
                Log_OC.e(TAG, "Failed to initialise Nextcloud ExoPlayer: ${e.message}")
                stopSelf()
            }
        }
    }

    private fun buildMediaSession(player: ExoPlayer): MediaSession =
        MediaSession.Builder(applicationContext, player)
            .setId(BACKGROUND_MEDIA_SESSION_ID)
            .setCustomLayout(listOf(seekBackward, seekForward))
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ConnectionResult = AcceptedResultBuilder(mediaSession ?: session)
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

                override fun onPostConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ) {
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

    private fun buildNotificationProvider() = object : DefaultMediaNotificationProvider(this) {
        val isPlaying = mediaSession?.player?.isPlaying ?: false

        @Suppress("DEPRECATION")
        override fun getMediaButtons(
            session: MediaSession,
            playerCommands: Player.Commands,
            customLayout: ImmutableList<CommandButton>,
            showPauseButton: Boolean
        ): ImmutableList<CommandButton> {
            val playPauseButton =
                CommandButton.Builder()
                    .setDisplayName(
                        if (isPlaying) getString(R.string.media_player_pause)
                        else getString(R.string.media_player_play)
                    )
                    .setIconResId(
                        if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                    )
                    .setPlayerCommand(COMMAND_PLAY_PAUSE)
                    .setExtras(Bundle().apply { putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, 1) })
                    .build()

            return ImmutableList.of(seekBackward, playPauseButton, seekForward)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        release()
    }

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        serviceScope.cancel()
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
            player.pause()
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
        stopSelf()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    companion object {
        private val TAG = BackgroundPlayerService::class.java.simpleName

        private const val SESSION_COMMAND_ACTION_SEEK_BACK = "SESSION_COMMAND_ACTION_SEEK_BACK"
        private const val SESSION_COMMAND_ACTION_SEEK_FORWARD = "SESSION_COMMAND_ACTION_SEEK_FORWARD"
        private const val BACKGROUND_MEDIA_SESSION_ID =
            "com.nextcloud.client.media.BACKGROUND_MEDIA_SESSION_ID"

        const val RELEASE_MEDIA_SESSION_BROADCAST_ACTION =
            "com.nextcloud.client.media.RELEASE_MEDIA_SESSION"
        const val STOP_MEDIA_SESSION_BROADCAST_ACTION =
            "com.nextcloud.client.media.STOP_MEDIA_SESSION"
    }
}
