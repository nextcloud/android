/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.glide

import android.net.Uri
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream

class UriModelLoaderFactory : ModelLoaderFactory<Uri, InputStream> {
    override fun build(unused: MultiModelLoaderFactory): ModelLoader<Uri, InputStream> = CustomGlideUriLoader()

    override fun teardown() = Unit
}
