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
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nextcloud.client.media.ExoplayerListener
import com.nextcloud.client.media.NextcloudExoPlayer
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogPreviewVideoBinding
import com.owncloud.android.lib.common.utils.Log_OC

@UnstableApi /**
 * Transfers a previously playing video to a fullscreen dialog, and handles the switch back to the previous player
 * when closed
 *
 * @param activity the Activity hosting the original non-fullscreen player
 * @param sourceExoPlayer the ExoPlayer playing the video
 * @param sourceView the original non-fullscreen surface that [sourceExoPlayer] is linked to
 */
class PreviewVideoFullscreenDialog(
    private val activity: Activity,
    nextcloudClient: NextcloudClient,
    private val sourceExoPlayer: ExoPlayer,
    private val sourceView: PlayerView
) : Dialog(sourceView.context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {

    private val binding: DialogPreviewVideoBinding = DialogPreviewVideoBinding.inflate(layoutInflater)
    private var playingStateListener: Player.Listener? = null

    /**
     * exoPlayer instance used for this view, either the original one or a new one in specific cases.
     * @see getShouldUseRotatedVideoWorkaround
     */
    private val mExoPlayer: ExoPlayer

    /**
     * Videos with rotation metadata present a bug in sdk < 30 where they are rotated incorrectly and stretched when
     * the video is resumed on a new surface. To work around this, in those circumstances we'll create a new ExoPlayer
     * instance, which is slower but should avoid the bug.
     */
    private val shouldUseRotatedVideoWorkaround
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R && isRotatedVideo()

    init {
        addContentView(
            binding.root,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        mExoPlayer = getExoPlayer(nextcloudClient)
        if (shouldUseRotatedVideoWorkaround) {
            sourceExoPlayer.currentMediaItem?.let { mExoPlayer.setMediaItem(it, sourceExoPlayer.currentPosition) }
            binding.videoPlayer.player = mExoPlayer
            mExoPlayer.prepare()
        }
    }

    private fun isRotatedVideo(): Boolean {
        val videoFormat = sourceExoPlayer.videoFormat
        return videoFormat != null && videoFormat.rotationDegrees != 0
    }

    private fun getExoPlayer(nextcloudClient: NextcloudClient): ExoPlayer {
        return if (shouldUseRotatedVideoWorkaround) {
            Log_OC.d(TAG, "Using new ExoPlayer instance to deal with rotated video")
            NextcloudExoPlayer
                .createNextcloudExoplayer(sourceView.context, nextcloudClient)
                .apply {
                    addListener(ExoplayerListener(sourceView.context, binding.videoPlayer, this))
                }
        } else {
            sourceExoPlayer
        }
    }

    override fun show() {
        val isPlaying = sourceExoPlayer.isPlaying
        if (isPlaying) {
            sourceExoPlayer.pause()
        }
        setOnShowListener {
            enableImmersiveMode()
            switchTargetViewFromSource()
            setListeners()
            if (isPlaying) {
                mExoPlayer.play()
            }
            binding.videoPlayer.showController()
        }
        super.show()
    }

    private fun switchTargetViewFromSource() {
        if (shouldUseRotatedVideoWorkaround) {
            mExoPlayer.seekTo(sourceExoPlayer.currentPosition)
        } else {
            PlayerView.switchTargetView(sourceExoPlayer, sourceView, binding.videoPlayer)
        }
    }

    private fun setListeners() {
        binding.root.findViewById<View>(R.id.exo_exit_fs).setOnClickListener { onBackPressed() }
        val pauseButton: View = binding.root.findViewById(R.id.exo_pause)
        pauseButton.setOnClickListener { sourceExoPlayer.pause() }
        val playButton: View = binding.root.findViewById(R.id.exo_play)
        playButton.setOnClickListener { sourceExoPlayer.play() }

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
        mExoPlayer.addListener(playListener)
        playingStateListener = playListener

        // Run once to set initial state of play or pause buttons
        playListener.onIsPlayingChanged(sourceExoPlayer.isPlaying)
    }

    override fun onBackPressed() {
        val isPlaying = mExoPlayer.isPlaying
        if (isPlaying) {
            mExoPlayer.pause()
        }
        setOnDismissListener {
            disableImmersiveMode()
            playingStateListener?.let {
                mExoPlayer.removeListener(it)
            }
            switchTargetViewToSource()
            if (isPlaying) {
                sourceExoPlayer.play()
            }
            sourceView.showController()
        }
        dismiss()
    }

    private fun switchTargetViewToSource() {
        if (shouldUseRotatedVideoWorkaround) {
            sourceExoPlayer.seekTo(mExoPlayer.currentPosition)
        } else {
            PlayerView.switchTargetView(sourceExoPlayer, binding.videoPlayer, sourceView)
        }
    }

    private fun enableImmersiveMode() {
        activity.window?.let {
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

    companion object {
        private val TAG = PreviewVideoFullscreenDialog::class.simpleName
    }
}
