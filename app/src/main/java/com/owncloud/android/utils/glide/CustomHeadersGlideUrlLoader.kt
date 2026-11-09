/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.glide

import android.content.Context
import androidx.core.net.toUri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.signature.ObjectKey
import com.nextcloud.common.CustomHeaders
import com.nextcloud.utils.headers.CustomHeadersHttpClient
import java.io.InputStream

/**
 * Glide's default loader repeats all request headers on redirects, so URLs of an origin with custom headers are
 * loaded through the library client instead. All other URLs fall through to the default loader.
 */
class CustomHeadersGlideUrlLoader(private val context: Context) : ModelLoader<GlideUrl, InputStream> {
    override fun buildLoadData(model: GlideUrl, width: Int, height: Int, options: Options): LoadData<InputStream> =
        LoadData(ObjectKey(model), CustomHeadersGlideUrlFetcher(model, CustomHeadersHttpClient.get(context)))

    override fun handles(model: GlideUrl): Boolean {
        val uri = model.toStringUrl().toUri()
        return CustomHeaders.forDestination(uri.scheme, uri.host, uri.port).isNotEmpty()
    }
}
