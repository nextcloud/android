/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui

import android.app.Activity
import android.app.PictureInPictureParams
import android.os.Build
import android.util.Rational
import com.nextcloud.client.player.model.state.VideoSize
import com.nextcloud.client.player.util.PlayerUtil.isPictureInPictureAllowed
import com.owncloud.android.lib.common.utils.Log_OC

private const val TAG = "VideoPictureInPicture"
private val MIN_ASPECT_RATIO = Rational(41841, 100000)
private val MAX_ASPECT_RATIO = Rational(239, 100)

class VideoPictureInPicture(private val activity: Activity) {

    private var aspectRatio: Rational? = null
    private var isAutoEnterEnabled = false

    val isAllowed: Boolean get() = activity.isPictureInPictureAllowed()

    fun enter(): Boolean = isAllowed && activity.enterPictureInPictureMode(createParams())

    fun enterOnUserLeave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S || !isAutoEnterEnabled) return
        enter()
    }

    fun update(videoSize: VideoSize?, isPlaying: Boolean) {
        val newAspectRatio = videoSize?.toAspectRatio() ?: aspectRatio
        if (newAspectRatio == aspectRatio && isPlaying == isAutoEnterEnabled) return

        aspectRatio = newAspectRatio
        isAutoEnterEnabled = isPlaying
        applyParams()
    }

    fun disableAutoEnter() {
        if (!isAutoEnterEnabled) return

        isAutoEnterEnabled = false
        applyParams()
    }

    private fun applyParams() {
        if (!isAllowed) return

        try {
            activity.setPictureInPictureParams(createParams())
        } catch (e: IllegalStateException) {
            Log_OC.e(TAG, "setPictureInPictureParams failed", e)
        }
    }

    private fun createParams(): PictureInPictureParams = PictureInPictureParams.Builder().apply {
        aspectRatio?.let(::setAspectRatio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setAutoEnterEnabled(isAutoEnterEnabled)
        }
    }.build()

    private fun VideoSize.toAspectRatio(): Rational? {
        if (width <= 0 || height <= 0) return null
        return Rational(width, height).coerceIn(MIN_ASPECT_RATIO, MAX_ASPECT_RATIO)
    }
}
