/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.glide

import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

/**
 * Custom model for Nextcloud client
 */
class GlideStringStreamLoader : ModelLoader<String, InputStream> {
    override fun buildLoadData(url: String, width: Int, height: Int, options: Options): LoadData<InputStream> {
        return LoadData<InputStream>(ObjectKey(url), GlideStringStreamFetcher(url))
    }

    override fun handles(s: String): Boolean {
        return true
    }
}
