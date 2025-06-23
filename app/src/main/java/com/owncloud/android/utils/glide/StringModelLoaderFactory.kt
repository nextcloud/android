/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils.glide

import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import java.io.InputStream

class StringModelLoaderFactory : ModelLoaderFactory<String, InputStream> {
    override fun build(unused: MultiModelLoaderFactory): ModelLoader<String, InputStream> {
        return GlideStringStreamLoader()
    }

    override fun teardown() {
        // Do nothing.
    }
}
