/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview

import android.app.Dialog
import android.content.DialogInterface
import android.os.Build
import android.view.ViewGroup
import android.view.Window
import androidx.activity.addCallback
import androidx.annotation.OptIn
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nextcloud.client.media.ExoplayerListener
import com.nextcloud.client.media.NextcloudExoPlayer
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.extensions.applyControlsInsets
import com.nextcloud.utils.extensions.setFullscreenButton
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogPreviewVideoBinding
import com.owncloud.android.lib.common.utils.Log_OC

/**
 * Transfers a previously playing video to a fullscreen dialog, and handles the switch back to the previous player
 * when closed
 *
 * @param activity the Activity hosting the original non-fullscreen player
 * @param sourceExoPlayer the ExoPlayer playing the video
 * @param sourceView the original non-fullscreen surface that [sourceExoPlayer] is linked to
 */
@OptIn(UnstableApi::class)
class PreviewVideoFullscreenDialog(
    private val activity: FragmentActivity,
    nextcloudClient: NextcloudClient,
    private val sourceExoPlayer: ExoPlayer,
    private val sourceView: PlayerView,
    private val forceSourcePlayer: Boolean = false
) : Dialog(sourceView.context, R.style.Dialog_FullscreenVideo) {

    private val binding: DialogPreviewVideoBinding = DialogPreviewVideoBinding.inflate(layoutInflater)

    private val playerView: PlayerView
        get() = binding.videoPlayer.root

    private var playingStateListener: androidx.media3.common.Player.Listener? = null
    private var externalDismissListener: DialogInterface.OnDismissListener? = null
    private var wasPlayingBeforeDismiss = false

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
        get() = !forceSourcePlayer && Build.VERSION.SDK_INT < Build.VERSION_CODES.R && isRotatedVideo()

    init {
        addContentView(
            binding.root,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        mExoPlayer = getExoPlayer(nextcloudClient)
        if (shouldUseRotatedVideoWorkaround) {
            sourceExoPlayer.currentMediaItem?.let { mExoPlayer.setMediaItem(it, sourceExoPlayer.currentPosition) }
            playerView.player = mExoPlayer
            mExoPlayer.prepare()
        }
        super.setOnDismissListener {
            restoreSourcePlayer()
            externalDismissListener?.onDismiss(this)
        }
        handleOnBackPressed()
    }

    /**
     * Keeps the caller's listener instead of letting it replace the internal one, which has to run first to hand the
     * playback back to [sourceView].
     */
    override fun setOnDismissListener(listener: DialogInterface.OnDismissListener?) {
        externalDismissListener = listener
    }

    private fun isRotatedVideo(): Boolean {
        val videoFormat = sourceExoPlayer.videoFormat
        return videoFormat != null && videoFormat.rotationDegrees != 0
    }

    private fun getExoPlayer(nextcloudClient: NextcloudClient): ExoPlayer = if (shouldUseRotatedVideoWorkaround) {
        Log_OC.d(TAG, "Using new ExoPlayer instance to deal with rotated video")
        NextcloudExoPlayer
            .createNextcloudExoplayer(sourceView.context, nextcloudClient)
            .apply {
                addListener(ExoplayerListener(sourceView.context, playerView, this))
            }
    } else {
        sourceExoPlayer
    }

    override fun show() {
        val isPlaying = sourceExoPlayer.isPlaying
        if (isPlaying) {
            sourceExoPlayer.pause()
        }
        setOnShowListener {
            enableImmersiveMode()
            keepControlsClearOfSystemBars()
            switchTargetViewFromSource()
            playerView.setFullscreenButton(isFullscreen = true) {
                activity.onBackPressedDispatcher.onBackPressed()
            }
            if (isPlaying) {
                mExoPlayer.play()
            }
        }
        super.show()
    }

    private fun switchTargetViewFromSource() {
        if (shouldUseRotatedVideoWorkaround) {
            mExoPlayer.seekTo(sourceExoPlayer.currentPosition)
        } else {
            PlayerView.switchTargetView(sourceExoPlayer, sourceView, playerView)
        }
    }

    private fun handleOnBackPressed() {
        activity.onBackPressedDispatcher.addCallback(activity) {
            wasPlayingBeforeDismiss = mExoPlayer.isPlaying
            if (wasPlayingBeforeDismiss) {
                mExoPlayer.pause()
            }
            dismiss()
            isEnabled = false
        }
    }

    private fun restoreSourcePlayer() {
        playingStateListener?.let {
            mExoPlayer.removeListener(it)
        }
        switchTargetViewToSource()
        if (wasPlayingBeforeDismiss) {
            sourceExoPlayer.play()
        }
        sourceView.showController()
    }

    private fun switchTargetViewToSource() {
        if (shouldUseRotatedVideoWorkaround) {
            sourceExoPlayer.seekTo(mExoPlayer.currentPosition)
        } else {
            PlayerView.switchTargetView(sourceExoPlayer, playerView, sourceView)
        }
    }

    private fun enableImmersiveMode() {
        val dialogWindow = window ?: return
        WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
        hideInset(dialogWindow, WindowInsetsCompat.Type.systemBars())
    }

    private fun hideInset(window: Window, type: Int) {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(type)
    }

    private fun keepControlsClearOfSystemBars() {
        ViewCompat.setOnApplyWindowInsetsListener(playerView) { _, windowInsets ->
            playerView.applyControlsInsets(
                windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(playerView)
    }

    companion object {
        private val TAG = PreviewVideoFullscreenDialog::class.simpleName
    }
}
