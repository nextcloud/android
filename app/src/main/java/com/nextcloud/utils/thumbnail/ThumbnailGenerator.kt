/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import android.widget.ImageView
import com.elyeproj.loaderviewlibrary.LoaderImageView
import com.owncloud.android.datamodel.OCFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThumbnailGenerator @Inject constructor(
    val fileThumbnailGenerator: FileThumbnailGenerator,
    val folderThumbnailGenerator: FolderThumbnailGenerator
) {

    @JvmOverloads
    fun setThumbnail(ocFile: OCFile?, view: ImageView?, isGrid: Boolean = false, shimmer: LoaderImageView? = null) {
        if (ocFile == null || view == null) {
            return
        }

        when {
            ocFile.isOfflineOperation -> fileThumbnailGenerator.setOfflineOperationThumbnail(ocFile, view)
            ocFile.isFolder -> folderThumbnailGenerator.setFolderThumbnail(ocFile, view, shimmer)
            else -> fileThumbnailGenerator.setThumbnail(ocFile, view, isGrid, shimmer)
        }
    }
}
