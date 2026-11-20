/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment.autoUpload.util

import androidx.annotation.DrawableRes
import com.owncloud.android.R
import com.owncloud.android.datamodel.MediaFolderType

@DrawableRes
fun MediaFolderType?.iconId(): Int = when (this) {
    MediaFolderType.VIDEO -> R.drawable.video_32dp
    MediaFolderType.IMAGE -> R.drawable.image_32dp
    MediaFolderType.CUSTOM, null -> R.drawable.folder_star_32dp
}
