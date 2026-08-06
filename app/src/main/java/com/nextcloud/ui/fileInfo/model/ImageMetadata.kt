/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.fileInfo.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ImageMetadata(
    val fileSize: String? = null,
    val date: String? = null,
    val length: Int? = null,
    val width: Int? = null,
    val exposure: String? = null,
    val aperture: String? = null,
    val focalLen: String? = null,
    val iso: String? = null,
    val make: String? = null,
    val model: String? = null,
    val location: Pair<Double, Double>? = null
) : Parcelable
