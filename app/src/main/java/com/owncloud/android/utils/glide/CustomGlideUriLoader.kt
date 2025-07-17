/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils.glide

import android.net.Uri
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.signature.ObjectKey
import java.io.InputStream

/**
 * Custom Model for authenticated fetching from Uri
 */
class CustomGlideUriLoader : ModelLoader<Uri, InputStream> {
    override fun buildLoadData(uri: Uri, width: Int, height: Int, options: Options): LoadData<InputStream> =
        LoadData<InputStream>(ObjectKey(uri), HttpStreamFetcher(uri.toString()))

    override fun handles(uri: Uri): Boolean = false
}
