/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.client.media

import android.content.Context
import android.content.DialogInterface
import android.view.View
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC

class ExoplayerListener(private val context: Context, private val playerView: View, private val exoPlayer: ExoPlayer) :
    Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_ENDED) {
            onCompletion()
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        Log_OC.d(TAG, "Exoplayer keep screen on: $isPlaying")
        playerView.keepScreenOn = isPlaying
    }

    private fun onCompletion() {
        exoPlayer.let {
            it.seekToDefaultPosition()
            it.pause()
        }
    }

    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        Log_OC.e(TAG, "Exoplayer error", error)
        val message = ErrorFormat.toString(context, error)
        MaterialAlertDialogBuilder(context)
            .setMessage(message)
            .setPositiveButton(R.string.common_ok) { _: DialogInterface?, _: Int ->
                onCompletion()
            }
            .setCancelable(false)
            .show()
    }

    companion object {
        private const val TAG = "ExoplayerListener"
    }
}
