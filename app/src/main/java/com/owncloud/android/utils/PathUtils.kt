/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.utils

import com.owncloud.android.datamodel.OCFile
import java.io.File

object PathUtils {
    /**
     * Returns `true` if [folderPath] is a direct parent of [filePath], `false` otherwise
     */
    fun isDirectParent(folderPath: String, filePath: String): Boolean {
        return File(folderPath).path == File(filePath).parent
    }

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
