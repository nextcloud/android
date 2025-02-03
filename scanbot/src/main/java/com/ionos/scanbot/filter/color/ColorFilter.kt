/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.filter.color

import android.graphics.Bitmap
import com.ionos.scanbot.filter.Filter
import io.scanbot.sdk.imagefilters.BrightnessFilter
import io.scanbot.sdk.imagefilters.ContrastFilter
import io.scanbot.sdk.imagefilters.LegacyFilter
import io.scanbot.sdk.imagefilters.WhiteBlackPointFilter
import io.scanbot.sdk.process.ImageProcessor

internal data class ColorFilter(val colorFilterType: ColorFilterType) : Filter {

    override fun apply(bitmap: Bitmap): Bitmap? {
        val brightness = convertValueToMatchFilterTune(colorFilterType.brightness)
        val contrast = convertValueToMatchFilterTune(colorFilterType.contrast)
        val sharpness = convertValueToMatchFilterTune(colorFilterType.sharpness)

        return ImageProcessor(bitmap).applyFilter(LegacyFilter(colorFilterType.scanbotFilter.code))
            .applyFilter(BrightnessFilter(brightness)).applyFilter(ContrastFilter(contrast))
            .applyFilter(WhiteBlackPointFilter(sharpness)).processedBitmap()
    }

    private fun convertValueToMatchFilterTune(value: Int): Double {
        return ((value - 50) * 2).toDouble() / 100
    }
}