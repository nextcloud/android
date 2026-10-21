/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import com.nextcloud.utils.OCFileUtils

data class GalleryRowLayout(val columns: Int, val rowWidth: Int, val spacing: Int, val fallbackCellSize: Int) {

    fun measure(files: List<OCFile>): List<GalleryCellSize> {
        if (files.isEmpty()) return emptyList()

        val availableWidth = (rowWidth - spacing * (files.size - 1)).coerceAtLeast(files.size).toFloat()
        val aspectRatios = files.map { it.aspectRatio() }
        val height = (availableWidth / aspectRatios.sum()).toInt().coerceAtLeast(1)

        val widths = aspectRatios.map { (height * it).toInt().coerceAtLeast(1) }.toMutableList()
        val remainingPixels = availableWidth.toInt() - widths.sum()
        if (remainingPixels > 0) {
            widths[widths.lastIndex] += remainingPixels
        }

        return widths.map { GalleryCellSize(it, height) }
    }

    private fun OCFile.aspectRatio(): Float {
        val (width, height) = OCFileUtils.getImageSize(this, fallbackCellSize.toFloat())
        return if (height > 0) width.toFloat() / height else 1f
    }
}
