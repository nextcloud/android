/**
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
        val i = Intent(context, PlayerService::class.java)
        i.putExtra(PlayerService.EXTRA_USER, user)
        i.putExtra(PlayerService.EXTRA_FILE, file)
        i.putExtra(PlayerService.EXTRA_AUTO_PLAY, playImmediately)
        i.putExtra(PlayerService.EXTRA_START_POSITION_MS, position)
        i.action = PlayerService.ACTION_PLAY
        startForegroundService(i)
    }

    fun stop(file: OCFile) {
        val i = Intent(context, PlayerService::class.java)
        i.putExtra(PlayerService.EXTRA_FILE, file)
        i.action = PlayerService.ACTION_STOP_FILE
        startForegroundService(i)
    }

    fun stop() {
        val i = Intent(context, PlayerService::class.java)
        i.action = PlayerService.ACTION_STOP
        startForegroundService(i)
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
