/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.thumbnail

import com.nextcloud.utils.extensions.toFile
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.utils.MimeTypeUtil

internal fun localUploadThumbnail(upload: OCUpload): OCFile? {
    if (!MimeTypeUtil.isImageOrVideo(upload.mimeType)) return null
    val local = upload.localPath.toFile()?.takeIf { it.isFile && it.canRead() } ?: return null
    return OCFile(upload.remotePath).apply {
        storagePath = local.absolutePath
        mimeType = upload.mimeType
    }
}
