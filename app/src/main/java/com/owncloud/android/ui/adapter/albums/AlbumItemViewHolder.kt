/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.albums

import android.widget.ImageView
import android.widget.TextView
import com.elyeproj.loaderviewlibrary.LoaderImageView

interface AlbumItemViewHolder {
    val thumbnail: ImageView
    val shimmerThumbnail: LoaderImageView
    val albumName: TextView
    val albumInfo: TextView
}
