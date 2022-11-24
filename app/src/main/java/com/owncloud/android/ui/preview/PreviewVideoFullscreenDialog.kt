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

package com.owncloud.android.ui.preview

import android.app.Activity
import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogPreviewVideoBinding

/**
 * Transfers a previously playing video to a fullscreen dialog, and handles the switch back to the previous player
 * when closed
 *
 * @param activity the Activity hosting the original non-fullscreen player
 * @param exoPlayer the ExoPlayer playing the video
 * @param sourceView the original non-fullscreen surface that [exoPlayer] is linked to
 */
class PreviewVideoFullscreenDialog(
    private val activity: Activity,
    private val exoPlayer: ExoPlayer,
    private val sourceView: StyledPlayerView
) : Dialog(sourceView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private val binding: DialogPreviewVideoBinding = DialogPreviewVideoBinding.inflate(layoutInflater)
    private var playingStateListener: Player.Listener? = null

    init {
        addContentView(
            binding.root,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    override fun show() {
        goFullScreen()
        super.show()
        binding.videoPlayer.showController()
    }

    private fun goFullScreen() {
        setListeners(exoPlayer)
        StyledPlayerView.switchTargetView(exoPlayer, sourceView, binding.videoPlayer)
        enableImmersiveMode()
    }

    private fun setListeners(exoPlayer: ExoPlayer) {
        binding.root.findViewById<View>(R.id.exo_exit_fs).setOnClickListener { onBackPressed() }
        val pauseButton: View = binding.root.findViewById(R.id.exo_pause)
        pauseButton.setOnClickListener { exoPlayer.pause() }
        val playButton: View = binding.root.findViewById(R.id.exo_play)
        playButton.setOnClickListener { exoPlayer.play() }

        val playListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    playButton.visibility = View.GONE
                    pauseButton.visibility = View.VISIBLE
                } else {
                    playButton.visibility = View.VISIBLE
                    pauseButton.visibility = View.GONE
                }
            }
        }
        exoPlayer.addListener(playListener)
        playingStateListener = playListener
    }

    override fun onBackPressed() {
        playingStateListener?.let {
            exoPlayer.removeListener(it)
        }
        StyledPlayerView.switchTargetView(exoPlayer, binding.videoPlayer, sourceView)
        disableImmersiveMode()
        super.onBackPressed()
        sourceView.showController()
    }

    private fun enableImmersiveMode() {
        // for immersive mode to work properly, need to disable statusbar on activity window, but nav bar in dialog
        // otherwise dialog navbar is not hidden, or statusbar padding is the wrong color
        activity.window?.let {
            hideInset(it, WindowInsetsCompat.Type.statusBars())
        }
        window?.let {
            hideInset(it, WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun hideInset(window: Window, type: Int) {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(type)
    }

    private fun disableImmersiveMode() {
        activity.window?.let {
            val windowInsetsController =
                WindowCompat.getInsetsController(it, it.decorView)
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } ?: return
    }
}
