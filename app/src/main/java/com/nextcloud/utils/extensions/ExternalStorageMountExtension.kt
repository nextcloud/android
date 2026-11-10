/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.android.lib.resources.externalstorage.model.ExternalStorageMount
import com.owncloud.android.datamodel.OCFile

fun ExternalStorageMount.remotePath(): String {
    val segments = listOf(path.trim(OCFile.PATH_SEPARATOR[0]), name).filter { it.isNotEmpty() }
    return segments.joinToString(OCFile.PATH_SEPARATOR, OCFile.PATH_SEPARATOR, OCFile.PATH_SEPARATOR)
}