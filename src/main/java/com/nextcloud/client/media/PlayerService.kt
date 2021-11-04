/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.media

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.os.IBinder
import android.widget.MediaController
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.theme.ThemeColorUtils
import dagger.android.AndroidInjection
import java.util.Locale
import javax.inject.Inject

class PlayerService : Service() {

    companion object {
        const val EXTRA_USER = "USER"
        const val EXTRA_FILE = "FILE"
        const val EXTRA_AUTO_PLAY = "EXTRA_AUTO_PLAY"
        const val EXTRA_START_POSITION_MS = "START_POSITION_MS"
        const val ACTION_PLAY = "PLAY"
        const val ACTION_STOP = "STOP"
        const val ACTION_TOGGLE = "TOGGLE"
        const val ACTION_STOP_FILE = "STOP_FILE"
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
            startForeground(file)
        }

        override fun onStart() {
            // empty
        }

        override fun onPause() {
            // empty
        }

        override fun onStop() {
            stopServiceAndRemoveNotification(null)
        }

        override fun onError(error: PlayerError) {
            Toast.makeText(this@PlayerService, error.message, Toast.LENGTH_SHORT).show()
        }
    }

    @Inject
    protected lateinit var audioManager: AudioManager

    @Inject
    protected lateinit var clientFactory: ClientFactory

    private lateinit var player: Player
    private lateinit var notificationBuilder: NotificationCompat.Builder

    override fun onCreate() {
        super.onCreate()
        AndroidInjection.inject(this)
        player = Player(applicationContext, clientFactory, playerListener, audioManager)
        notificationBuilder = NotificationCompat.Builder(this)
        notificationBuilder.color = ThemeColorUtils.primaryColor(this)

        val stop = Intent(this, PlayerService::class.java)
        stop.action = ACTION_STOP
        val pendingStop = PendingIntent.getService(this, 0, stop, 0)
        notificationBuilder.addAction(0, getString(R.string.player_stop).toUpperCase(Locale.getDefault()), pendingStop)

        val toggle = Intent(this, PlayerService::class.java)
        toggle.action = ACTION_TOGGLE
        val pendingToggle = PendingIntent.getService(this, 0, toggle, 0)
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
        if (player.isPlaying) {
            player.pause()
        } else {
            player.start()
        }
    }

    private fun onActionPlay(intent: Intent) {
        val user: User = intent.getParcelableExtra(EXTRA_USER) as User
        val file: OCFile = intent.getParcelableExtra(EXTRA_FILE) as OCFile
        val startPos = intent.getLongExtra(EXTRA_START_POSITION_MS, 0)
        val autoPlay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, true)
        val item = PlaylistItem(file = file, startPositionMs = startPos, autoPlay = autoPlay, user = user)
        player.play(item)
    }

    private fun onActionStop() {
        stopServiceAndRemoveNotification(null)
    }

    private fun onActionStopFile(args: Bundle?) {
        val file: OCFile = args?.getParcelable(EXTRA_FILE) ?: throw IllegalArgumentException("Missing file argument")

        stopServiceAndRemoveNotification(file)
    }

    private fun startForeground(currentFile: OCFile) {
        val ticker = String.format(getString(R.string.media_notif_ticker), getString(R.string.app_name))
        val content = getString(R.string.media_state_playing, currentFile.getFileName())
        notificationBuilder.setSmallIcon(R.drawable.ic_play_arrow)
        notificationBuilder.setWhen(System.currentTimeMillis())
        notificationBuilder.setOngoing(true)
        notificationBuilder.setContentTitle(ticker)
        notificationBuilder.setContentText(content)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_MEDIA)
        }

        startForeground(R.string.media_notif_ticker, notificationBuilder.build())
    }

    private fun stopServiceAndRemoveNotification(file: OCFile?) {
        if (file == null) {
            player.stop()
        } else {
            player.stop(file)
        }

        stopSelf()
        stopForeground(true)
    }
}
