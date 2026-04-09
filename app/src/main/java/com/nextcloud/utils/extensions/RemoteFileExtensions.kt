/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.utils.OCFileUtils
import com.nextcloud.utils.TimeConstants
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileUtil
import com.owncloud.android.utils.MimeTypeUtil

fun RemoteFile.isSame(path: String?): Boolean {
    val localFile = path?.toFile() ?: return false

    // remote file timestamp in millisecond not microsecond
    val localLastModifiedTimestamp = localFile.lastModified() / TimeConstants.MILLIS_PER_SECOND
    val localCreationTimestamp = FileUtil.getCreationTimestamp(localFile)
    val localSize: Long = localFile.length()

    return size == localSize &&
        localCreationTimestamp != null &&
        localCreationTimestamp == creationTimestamp &&
        modifiedTimestamp == localLastModifiedTimestamp * TimeConstants.MILLIS_PER_SECOND &&
        this.areImageDimensionsSame(path)
}

@Suppress("ReturnCount")
private fun RemoteFile.areImageDimensionsSame(path: String): Boolean {
    if (!MimeTypeUtil.isImage(mimeType)) {
        // can't compare it's not image
        return true
    }

    val localFileImageDimension = OCFileUtils.getExifSize(path) ?: OCFileUtils.getBitmapSize(path)
    if (localFileImageDimension == null) {
        // can't compare local file image dimension is not determined
        return true
    }

    return localFileImageDimension.first.toFloat() == imageDimension?.width &&
        localFileImageDimension.second.toFloat() == imageDimension?.height
}
