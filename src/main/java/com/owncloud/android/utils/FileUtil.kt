/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2020 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils

import android.text.TextUtils
import java.io.File

object FileUtil {
    /**
     * returns the file name of a given path.
     *
     * @param filePath (absolute) file path
     * @return the filename including its file extension, `empty String` for invalid input values
     */
    @JvmStatic
    fun getFilenameFromPathString(filePath: String?): String {
        return if (!TextUtils.isEmpty(filePath)) {
            val file = File(filePath)
            if (file.isFile) {
                file.name
            } else {
                ""
            }
        } else {
            ""
        }
    }
}
