/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.elyeproj.loaderviewlibrary.LoaderImageView

interface ListViewHolder {
    val thumbnail: ImageView
    fun showVideoOverlay()
    val shimmerThumbnail: LoaderImageView
    val favorite: ImageView
    val localFileIndicator: ImageView
    val imageFileName: TextView?
    val shared: ImageView
    val checkbox: ImageView
    val itemLayout: View
    val unreadComments: ImageView
    val more: ImageButton?
    val fileFeaturesLayout: LinearLayout?
    val gridLivePhotoIndicator: ImageView?
    val livePhotoIndicator: TextView?
    val livePhotoIndicatorSeparator: TextView?
}
