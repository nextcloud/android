/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.content.Context
import android.content.DialogInterface
import android.view.View
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC

class ExoplayerListener(
    private val context: Context,
    private val playerView: View,
    private val exoPlayer: ExoPlayer,
    private val onCompleted: () -> Unit = { }
) : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        if (playbackState == Player.STATE_ENDED) {
            onCompletion()
            onCompleted()
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
