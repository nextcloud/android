/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import android.net.Uri
import android.webkit.MimeTypeMap
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.utils.MimeTypeUtil
import java.io.File

val File.mimeType: String?
    get() {
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(this).toString().lowercase())

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
    }

/**
 * Also matches the mime type derived from the remote path.
 */
inline fun ServerFileInterface.matchesMimeType(predicate: (String?) -> Boolean): Boolean =
    predicate(mimeType) || predicate(MimeTypeUtil.getMimeTypeFromPath(remotePath))

fun OCFile.resolveMimeType(): String = mimeType
    ?.takeIf { it.isNotEmpty() }
    ?: remotePath?.let { MimeTypeUtil.getMimeTypeFromPath(it) }
    ?: ""
