/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.os.Build
import android.util.Rational
import android.view.View
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.state.VideoSize
import com.nextcloud.client.player.util.PlayerUtil.isPictureInPictureAllowed

private const val ASPECT_RATIO_WIDTH = 16
private const val ASPECT_RATIO_HEIGHT = 9
private const val MIN_ASPECT_RATIO = 0.42f
private const val MAX_ASPECT_RATIO = 2.39f

class VideoPictureInPicture(
    private val activity: Activity,
    private val playbackModel: PlaybackModel,
    private val autoEnter: Boolean
) {

    private val defaultAspectRatio = Rational(ASPECT_RATIO_WIDTH, ASPECT_RATIO_HEIGHT)

    val isAllowed: Boolean get() = activity.isPictureInPictureAllowed()

    fun enter(sourceView: View): Boolean = isAllowed && activity.enterPictureInPictureMode(createParams(sourceView))

    private fun createParams(sourceView: View): PictureInPictureParams {
        val aspectRatio = getAspectRatio(playbackModel.state?.currentItemState?.videoSize)
        return PictureInPictureParams.Builder().let {
            it.setAspectRatio(aspectRatio)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                it.setAutoEnterEnabled(autoEnter)
            }
            it.setSourceRectHint(getSourceRectHint(sourceView, aspectRatio))
            it.build()
        }
    }

    private fun getAspectRatio(videoSize: VideoSize?): Rational {
        if (videoSize == null) return defaultAspectRatio

        val ratio = videoSize.width.toFloat() / videoSize.height.toFloat()
        return if (ratio < MIN_ASPECT_RATIO || ratio > MAX_ASPECT_RATIO) {
            defaultAspectRatio
        } else {
            Rational(videoSize.width, videoSize.height)
        }
    }

    private fun getSourceRectHint(sourceView: View, aspectRatio: Rational): Rect {
        val sourceRect = Rect()
        sourceView.getGlobalVisibleRect(sourceRect)
        val heightHint = (sourceRect.width() / aspectRatio.toFloat()).toInt()
        return Rect(
            sourceRect.left,
            sourceRect.top + (sourceRect.height() - heightHint) / 2,
            sourceRect.right,
            sourceRect.top + (sourceRect.height() + heightHint) / 2
        )
    }
}
