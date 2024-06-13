/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.MediaController
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.ForegroundServiceHelper
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.ForegroundServiceType
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.ui.preview.PreviewMediaActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import dagger.android.AndroidInjection
import java.util.Locale
import javax.inject.Inject

class PlayerService : Service() {

    companion object {
        private const val TAG = "PlayerService"

        const val EXTRA_USER = "USER"
        const val EXTRA_FILE = "FILE"
        const val EXTRA_AUTO_PLAY = "EXTRA_AUTO_PLAY"
        const val EXTRA_START_POSITION_MS = "START_POSITION_MS"
        const val ACTION_PLAY = "PLAY"
        const val ACTION_STOP = "STOP"
        const val ACTION_TOGGLE = "TOGGLE"
        const val ACTION_STOP_FILE = "STOP_FILE"

        const val IS_MEDIA_CONTROL_LAYOUT_READY = "IS_MEDIA_CONTROL_LAYOUT_READY"
    }

    class Binder(val service: PlayerService) : android.os.Binder() {

        /**
         * This property returns current instance of media player interface.
         * It is not cached and it is suitable for polling.
         */
        val player: MediaController.MediaPlayerControl get() = service.player
    }

    private val playerListener = object : Player.Listener {
        override fun onRunning(file: OCFile) {
            Log_OC.d(TAG, "PlayerService.onRunning()")
            val intent = Intent(PreviewMediaActivity.MEDIA_CONTROL_READY_RECEIVER).apply {
                putExtra(IS_MEDIA_CONTROL_LAYOUT_READY, false)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
            startForeground(file)
        }

        override fun onStart() {
            Log_OC.d(TAG, "PlayerService.onStart()")
            val intent = Intent(PreviewMediaActivity.MEDIA_CONTROL_READY_RECEIVER).apply {
                putExtra(IS_MEDIA_CONTROL_LAYOUT_READY, true)
            }
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)
        }

        override fun onPause() {
            Log_OC.d(TAG, "PlayerService.onPause()")
        }

        override fun onStop() {
            Log_OC.d(TAG, "PlayerService.onStop()")
            stopServiceAndRemoveNotification(null)
        }

        override fun onError(error: PlayerError) {
            Log_OC.d(TAG, "PlayerService.onError()")
            Toast.makeText(this@PlayerService, error.message, Toast.LENGTH_SHORT).show()
        }
    }

    @Inject
    lateinit var audioManager: AudioManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var player: Player
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()

        AndroidInjection.inject(this)
        player = Player(applicationContext, clientFactory, playerListener, audioManager)
        notificationBuilder = NotificationCompat.Builder(this)
        viewThemeUtils.androidx.themeNotificationCompatBuilder(this, notificationBuilder)

        val stop = Intent(this, PlayerService::class.java).apply {
            action = ACTION_STOP
        }

        val pendingStop = PendingIntent.getService(this, 0, stop, PendingIntent.FLAG_IMMUTABLE)
        notificationBuilder.addAction(0, getString(R.string.player_stop).toUpperCase(Locale.getDefault()), pendingStop)

        val toggle = Intent(this, PlayerService::class.java).apply {
            action = ACTION_TOGGLE
        }

        val pendingToggle = PendingIntent.getService(this, 0, toggle, PendingIntent.FLAG_IMMUTABLE)
        notificationBuilder.addAction(
            0,
            getString(R.string.player_toggle).toUpperCase(Locale.getDefault()),
            pendingToggle
        )
    }

    override fun onBind(intent: Intent?): IBinder? {
        return Binder(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_PLAY -> onActionPlay(intent)
            ACTION_STOP -> onActionStop()
            ACTION_STOP_FILE -> onActionStopFile(intent.extras)
            ACTION_TOGGLE -> onActionToggle()
        }
        return START_NOT_STICKY
    }

    private fun onActionToggle() {
        player.run {
            if (isPlaying) {
                pause()
            } else {
                start()
            }
        }
    }

    private fun onActionPlay(intent: Intent) {
        val user: User = intent.getParcelableArgument(EXTRA_USER, User::class.java)!!
        val file: OCFile = intent.getParcelableArgument(EXTRA_FILE, OCFile::class.java)!!
        val startPos = intent.getLongExtra(EXTRA_START_POSITION_MS, 0)
        val autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, true)
        val item = PlaylistItem(file = file, startPositionMs = startPos, autoPlay = autoPlay, user = user)
        player.play(item)
    }

    private fun onActionStop() {
        stopServiceAndRemoveNotification(null)
    }

    private fun onActionStopFile(args: Bundle?) {
        val file: OCFile = args?.getParcelableArgument(EXTRA_FILE, OCFile::class.java)
            ?: throw IllegalArgumentException("Missing file argument")
        stopServiceAndRemoveNotification(file)
    }

    private fun startForeground(currentFile: OCFile) {
        val ticker = String.format(getString(R.string.media_notif_ticker), getString(R.string.app_name))
        val content = getString(R.string.media_state_playing, currentFile.getFileName())

        notificationBuilder.run {
            setSmallIcon(R.drawable.ic_play_arrow)
            setWhen(System.currentTimeMillis())
            setOngoing(true)
            setContentTitle(ticker)
            setContentText(content)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_MEDIA)
            }
        }

        ForegroundServiceHelper.startService(
            this,
            R.string.media_notif_ticker,
            notificationBuilder.build(),
            ForegroundServiceType.MediaPlayback
        )

        isRunning = true
    }

    private fun stopServiceAndRemoveNotification(file: OCFile?) {
        if (file == null) {
            player.stop()
        } else {
            player.stop(file)
        }

        if (isRunning) {
            stopForeground(true)
            stopSelf()
            isRunning = false
        }
    }
}
