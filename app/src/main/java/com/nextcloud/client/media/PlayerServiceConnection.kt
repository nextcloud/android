/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.widget.MediaController
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.OCFile

@Suppress("TooManyFunctions") // implementing large interface
class PlayerServiceConnection(private val context: Context) : MediaController.MediaPlayerControl {

    var isConnected: Boolean = false
        private set

    private var binder: PlayerService.Binder? = null

    fun bind() {
        val intent = Intent(context, PlayerService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun unbind() {
        if (isConnected) {
            binder = null
            isConnected = false
            context.unbindService(connection)
        }
    }

    fun start(user: User, file: OCFile, playImmediately: Boolean, position: Long) {
        val i = Intent(context, PlayerService::class.java).apply {
            putExtra(PlayerService.EXTRA_USER, user)
            putExtra(PlayerService.EXTRA_FILE, file)
            putExtra(PlayerService.EXTRA_AUTO_PLAY, playImmediately)
            putExtra(PlayerService.EXTRA_START_POSITION_MS, position)
            action = PlayerService.ACTION_PLAY
        }

        startForegroundService(i)
    }

    fun stop(file: OCFile) {
        val i = Intent(context, PlayerService::class.java)
        i.putExtra(PlayerService.EXTRA_FILE, file)
        i.action = PlayerService.ACTION_STOP_FILE
        try {
            context.startService(i)
        } catch (ex: IllegalStateException) {
            // https://developer.android.com/about/versions/oreo/android-8.0-changes#back-all
            // ignore it - the service is not running and does not need to be stopped
        }
    }

    fun stop() {
        val i = Intent(context, PlayerService::class.java)
        i.action = PlayerService.ACTION_STOP
        try {
            context.startService(i)
        } catch (ex: IllegalStateException) {
            // https://developer.android.com/about/versions/oreo/android-8.0-changes#back-all
            // ignore it - the service is not running and does not need to be stopped
        }
    }

    private val connection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            isConnected = false
            binder = null
        }

        override fun onServiceConnected(name: ComponentName?, localBinder: IBinder?) {
            binder = localBinder as PlayerService.Binder
            isConnected = true
        }
    }

    // region Media controller

    override fun isPlaying(): Boolean {
        return binder?.player?.isPlaying ?: false
    }

    override fun canSeekForward(): Boolean {
        return binder?.player?.canSeekForward() ?: false
    }

    override fun getDuration(): Int {
        return binder?.player?.duration ?: 0
    }

    override fun pause() {
        binder?.player?.pause()
    }

    override fun getBufferPercentage(): Int {
        return binder?.player?.bufferPercentage ?: 0
    }

    override fun seekTo(pos: Int) {
        binder?.player?.seekTo(pos)
    }

    override fun getCurrentPosition(): Int {
        return binder?.player?.currentPosition ?: 0
    }

    override fun canSeekBackward(): Boolean {
        return binder?.player?.canSeekBackward() ?: false
    }

    override fun start() {
        binder?.player?.start()
    }

    override fun getAudioSessionId(): Int {
        return 0
    }

    override fun canPause(): Boolean {
        return binder?.player?.canPause() ?: false
    }

    // endregion

    private fun startForegroundService(i: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i)
        } else {
            context.startService(i)
        }
    }
}
