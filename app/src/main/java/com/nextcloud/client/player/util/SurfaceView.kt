/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.util

import android.view.SurfaceView
import android.view.ViewGroup
import com.nextcloud.client.player.model.state.VideoSize

/**
 * Letterboxes the surface inside its container so the video keeps its aspect ratio.
 */
fun SurfaceView.applyVideoSize(videoSize: VideoSize) {
    val screenWidth = context.getDisplayWidth()
    val screenHeight = context.getDisplayHeight()
    val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()
    val videoProportion = videoSize.width.toFloat() / videoSize.height.toFloat()

    layoutParams = layoutParams.apply {
        if (screenProportion < videoProportion) {
            width = ViewGroup.LayoutParams.MATCH_PARENT
            height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            width = (videoProportion * screenHeight.toFloat()).toInt()
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }
}
