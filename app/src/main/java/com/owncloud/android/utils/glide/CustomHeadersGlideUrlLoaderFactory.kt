/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.glide

import android.content.Context
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream

class CustomHeadersGlideUrlLoaderFactory(private val context: Context) : ModelLoaderFactory<GlideUrl, InputStream> {
    override fun build(unused: MultiModelLoaderFactory): ModelLoader<GlideUrl, InputStream> =
        CustomHeadersGlideUrlLoader(context.applicationContext)

    override fun teardown() = Unit
}
