/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.image_loading

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.ionos.scanbot.image_loader.ImageLoaderOptions
import com.ionos.scanbot.image_loader.ImageRequestBuilder
import com.ionos.scanbot.image_loader.ScaleType
import java.io.File

class ImageRequestBuilderImpl(
    private val file: File,
) : ImageRequestBuilder {

    private var options: ImageLoaderOptions? = null

    override fun options(options: ImageLoaderOptions): ImageRequestBuilder {
        this.options = options
        return this
    }

    override fun into(target: ImageView) {
        Glide.with(target.context)
            .load(file)
            .run {
                when (options?.scaleType) {
                    ScaleType.CENTER_CROP -> centerCrop()
                    ScaleType.CENTER_INSIDE -> fitCenter()
                    else -> this
                }
            }
            .signature(ObjectKey(file.lastModified()))
            .into(target)
    }
}