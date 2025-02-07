/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

private const val tag = "FileExtensions"

fun OCFile?.logFileSize(tag: String) {
    val size = DisplayUtils.bytesToHumanReadable(this?.fileLength ?: -1)
    val rawByte = this?.fileLength ?: -1
    Log_OC.d(tag, "onSaveInstanceState: $size, raw byte $rawByte")
}

fun File?.logFileSize(tag: String) {
    val size = DisplayUtils.bytesToHumanReadable(this?.length() ?: -1)
    val rawByte = this?.length() ?: -1
    Log_OC.d(tag, "onSaveInstanceState: $size, raw byte $rawByte")
}

fun File?.loadThumbnail(context: Context, viewThemeUtils: ViewThemeUtils, imageView: ImageView) {
    if (this == null || !exists()) {
        Log_OC.d(tag, "cannot load thumbnail file is null or not exists")
        return
    }

    val placeholder = MimeTypeUtil.getFileTypeIcon(this, context, viewThemeUtils)

    Glide
        .with(context)
        .load(this)
        .centerCrop()
        .placeholder(placeholder)
        .error(R.drawable.file)
        .diskCacheStrategy(DiskCacheStrategy.ALL)
        .into(imageView)
}
