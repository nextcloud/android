/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.filter.crop

import android.graphics.Bitmap
import com.ionos.scanbot.entity.SelectedContour
import com.ionos.scanbot.filter.Filter
import io.scanbot.sdk.process.ImageProcessor


internal data class CropFilter(val contour: SelectedContour) : Filter {

    override fun apply(bitmap: Bitmap): Bitmap? {
        return ImageProcessor(bitmap)
            .crop(contour.normalizedPolygon)
            .processedBitmap()
    }
}