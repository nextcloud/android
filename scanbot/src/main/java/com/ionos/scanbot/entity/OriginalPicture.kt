/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.entity

import android.graphics.Bitmap
import com.ionos.scanbot.filter.FilterType
import com.ionos.scanbot.filter.Filterable
import com.ionos.scanbot.filter.color.ColorFilter
import com.ionos.scanbot.filter.crop.CropFilter
import com.ionos.scanbot.filter.rotate.RotateFilter

internal data class OriginalPicture(
    val id: String,
    private val cropFilter: CropFilter,
    private val colorFilter: ColorFilter,
    private val rotateFilter: RotateFilter,
) : Filterable {

    override fun getCropFilter(): CropFilter = cropFilter

    override fun getColorFilter(): ColorFilter = colorFilter

    override fun getRotateFilter(): RotateFilter = rotateFilter

    override fun applyFilters(
        filterTypes: Set<FilterType>,
        bitmap: Bitmap
    ): Bitmap? {
        var bitmapWithFilters: Bitmap? = bitmap

        filterTypes.forEach { filterType ->
            when (filterType) {
                FilterType.COLOR -> {
                    bitmapWithFilters = bitmapWithFilters?.let {
                        colorFilter.apply(it)
                    }
                }
                FilterType.CROP -> {
                    bitmapWithFilters = bitmapWithFilters?.let {
                        cropFilter.apply(it)
                    }
                }
                FilterType.ROTATE -> {
                    bitmapWithFilters = bitmapWithFilters?.let {
                        rotateFilter.apply(it)
                    }
                }
            }
        }

        return bitmapWithFilters
    }
}