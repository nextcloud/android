/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist.item

import android.view.View
import android.widget.ImageView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.nextcloud.ui.component.FilenameView

interface FileViewHolder {
    val itemLayout: View
    val thumbnail: ImageView
    val shimmerThumbnail: LoaderImageView
    val checkbox: ImageView
    val favorite: ImageView
    val localFileIndicator: ImageView
    val shared: ImageView
    val unreadComments: ImageView
    val filename: FilenameView
}
