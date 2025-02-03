/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.filter.rotate

import android.graphics.Bitmap
import com.ionos.scanbot.filter.Filter
import com.ionos.scanbot.util.graphics.rotate

internal data class RotateFilter(val degrees: Float) : Filter {
    override fun apply(bitmap: Bitmap) = bitmap.rotate(degrees)
}