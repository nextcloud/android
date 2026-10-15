/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.utils.TimeConstants
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.lib.resources.tags.Tag
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.FileUtil
import com.owncloud.android.utils.MimeTypeUtil

fun RemoteFile.isSame(path: String?): Boolean {
    val localFile = path?.toFile() ?: return false

    // remote file timestamp in millisecond not microsecond
    val localLastModifiedTimestamp = localFile.lastModified() / TimeConstants.MILLIS_PER_SECOND
    val localCreationTimestamp = FileUtil.getCreationTimestamp(localFile)
    val localSize: Long = localFile.length()

    val isTheSame = size == localSize &&
        localCreationTimestamp != null &&
        localCreationTimestamp == creationTimestamp &&
        modifiedTimestamp == localLastModifiedTimestamp * TimeConstants.MILLIS_PER_SECOND &&
        this.areImageDimensionsSame(path)

    Log_OC.d(
        "105127",
        "RemoteFileIsSame field:[local|remote] -> size:[$localSize|$size], " +
            "creation:[$localCreationTimestamp|$creationTimestamp], " +
            "modif:[${localLastModifiedTimestamp * TimeConstants.MILLIS_PER_SECOND}|$modifiedTimestamp], " +
            "areImageDimensionsSame: [${this.areImageDimensionsSame(path)}], isTheSame:[$isTheSame]"
    )

    return isTheSame
}

fun RemoteFile.sharedViaLink(): Boolean = sharees?.any { it.shareType?.isLink == true } ?: false

fun RemoteFile.sharedWithSharee(): Boolean = sharees?.isNotEmpty() ?: false

fun RemoteFile.getShareeList(): List<ShareeUser> = sharees?.toList() ?: emptyList()

fun RemoteFile.tags(): List<Tag> = tags?.mapNotNull { it } ?: emptyList()

/**
 * Album responses carry no remote id, so the local id doubles as one to keep thumbnail generation working.
 *
 * Shared by [com.owncloud.android.operations.albums.ReadAlbumItemsOperation], which stores album items, and by the
 * album screens falling back to a non persisted item, so both end up with the same file.
 */
fun RemoteFile.toAlbumItem(): OCFile = FileStorageUtils.fillOCFile(this).apply {
    remoteId = this@toAlbumItem.localId.toString()
}

@Suppress("ReturnCount")
private fun RemoteFile.areImageDimensionsSame(path: String): Boolean {
    if (!MimeTypeUtil.isImage(mimeType)) {
        // can't compare it's not image
        return true
    }

    val localFileImageDimension = path.getExifSize() ?: path.getBitmapSize()
    if (localFileImageDimension == null) {
        // can't compare local file image dimension is not determined
        return true
    }

    val dimAreTheSame = localFileImageDimension.first.toFloat() == imageDimension?.width &&
        localFileImageDimension.second.toFloat() == imageDimension?.height

    Log_OC.d(
        "105127",
        "areImageDimensionsSame field:[local|remote] -> " +
            "width:[${localFileImageDimension.first.toFloat()}|${imageDimension?.width}], " +
            "width:[${localFileImageDimension.second.toFloat()}|${imageDimension?.height}]," +
            "dimAreTheSame:[$dimAreTheSame]"
    )

    return dimAreTheSame
}
