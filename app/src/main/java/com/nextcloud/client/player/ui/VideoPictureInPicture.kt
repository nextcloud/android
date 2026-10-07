/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.util.Rational
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.state.VideoSize
import com.nextcloud.client.player.util.PlayerUtil.isPictureInPictureAllowed

private const val ASPECT_RATIO_WIDTH = 16
private const val ASPECT_RATIO_HEIGHT = 9
private const val MIN_ASPECT_RATIO = 0.42f
private const val MAX_ASPECT_RATIO = 2.39f

class VideoPictureInPicture(private val activity: Activity, private val playbackModel: PlaybackModel) {

    private val defaultAspectRatio = Rational(ASPECT_RATIO_WIDTH, ASPECT_RATIO_HEIGHT)

    fun enter(): Boolean = activity.isPictureInPictureAllowed() && activity.enterPictureInPictureMode(createParams())

    private fun createParams(): PictureInPictureParams = PictureInPictureParams.Builder()
        .setAspectRatio(getAspectRatio(playbackModel.state?.currentItemState?.videoSize))
        .build()

    private fun getAspectRatio(videoSize: VideoSize?): Rational {
        if (videoSize == null) return defaultAspectRatio

        val ratio = videoSize.width.toFloat() / videoSize.height.toFloat()
        return if (ratio !in MIN_ASPECT_RATIO..MAX_ASPECT_RATIO) {
            defaultAspectRatio
        } else {
            Rational(videoSize.width, videoSize.height)
        }
    }
}
