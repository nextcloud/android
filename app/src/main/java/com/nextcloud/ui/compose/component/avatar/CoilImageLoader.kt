/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.component.avatar

import android.content.Context
import coil3.ImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.nextcloud.common.NextcloudClient

object CoilImageLoader {
    @Volatile
    private var imageLoader: ImageLoader? = null

    fun get(context: Context, client: NextcloudClient): ImageLoader = imageLoader ?: synchronized(this) {
        imageLoader ?: create(context.applicationContext, client).also { imageLoader = it }
    }

    private fun create(context: Context, client: NextcloudClient): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(OkHttpNetworkFetcherFactory(callFactory = { client.client }, cacheStrategy = { CoilCacheStrategy() }))
        }
        .crossfade(true)
        .build()
}
