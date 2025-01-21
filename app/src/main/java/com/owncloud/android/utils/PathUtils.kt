/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import com.owncloud.android.datamodel.OCFile
import java.io.File

object PathUtils {
    /**
     * Returns `true` if [folderPath] is a direct parent of [filePath], `false` otherwise
     */
    fun isDirectParent(folderPath: String, filePath: String): Boolean = File(folderPath).path == File(filePath).parent

    /**
     * Returns `true` if [folderPath] is an ancestor of [filePath], `false` otherwise
     *
     * If [isDirectParent] is `true` for the same arguments, this function should return `true` as well
     */
    fun isAncestor(folderPath: String, filePath: String): Boolean {
        if (folderPath.isEmpty() || filePath.isEmpty()) {
            return false
        }
        val folderPathWithSlash =
            if (folderPath.endsWith(OCFile.PATH_SEPARATOR)) folderPath else folderPath + OCFile.PATH_SEPARATOR
        return filePath.startsWith(folderPathWithSlash)
    }
}
