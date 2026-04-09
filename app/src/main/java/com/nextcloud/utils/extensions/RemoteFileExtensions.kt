/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.utils.TimeConstants
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileUtil

fun RemoteFile.isSame(path: String?): Boolean {
    val localFile = path?.toFile() ?: return false

    // remote file timestamp in milli not micro sec
    val localLastModifiedTimestamp = localFile.lastModified() / TimeConstants.MILLIS_PER_SECOND
    val localCreationTimestamp = FileUtil.getCreationTimestamp(localFile)
    val localSize: Long = localFile.length()

    return size == localSize &&
        localCreationTimestamp != null &&
        localCreationTimestamp == creationTimestamp &&
        modifiedTimestamp == localLastModifiedTimestamp * TimeConstants.MILLIS_PER_SECOND
}
