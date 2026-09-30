/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.utils.MimeTypeUtil

fun OCFile.resolveMimeType(): String = mimeType
    ?.takeIf { it.isNotEmpty() }
    ?: remotePath?.let { MimeTypeUtil.getMimeTypeFromPath(it) }
    ?: ""
