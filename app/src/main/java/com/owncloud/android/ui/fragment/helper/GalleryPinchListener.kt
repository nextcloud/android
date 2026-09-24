/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.helper

import android.view.ScaleGestureDetector

private const val ZOOM_IN_THRESHOLD = 1.25f
private const val ZOOM_OUT_THRESHOLD = 0.8f
private const val NEUTRAL_SCALE = 1f

class GalleryPinchListener(private val onColumnStep: (Int) -> Unit) :
    ScaleGestureDetector.SimpleOnScaleGestureListener() {

    private var accumulatedScale = NEUTRAL_SCALE

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        accumulatedScale = NEUTRAL_SCALE
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        accumulatedScale *= detector.scaleFactor

        if (accumulatedScale >= ZOOM_IN_THRESHOLD) {
            accumulatedScale = NEUTRAL_SCALE
            onColumnStep(-1)
        } else if (accumulatedScale <= ZOOM_OUT_THRESHOLD) {
            accumulatedScale = NEUTRAL_SCALE
            onColumnStep(1)
        }

        return true
    }
}
