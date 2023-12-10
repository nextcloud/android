/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2017 Andy Scherzinger
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
 */
package com.owncloud.android.datamodel

import android.util.SparseArray

/**
 * Types of media folder.
 */
enum class MediaFolderType(@JvmField val id: Int) {
    CUSTOM(0), IMAGE(1), VIDEO(2);

    companion object {
        private val reverseMap = SparseArray<MediaFolderType>(3)

        init {
            reverseMap.put(CUSTOM.id, CUSTOM)
            reverseMap.put(IMAGE.id, IMAGE)
            reverseMap.put(VIDEO.id, VIDEO)
        }

        @JvmStatic
        fun getById(id: Int?): MediaFolderType {
            return reverseMap[id!!]
        }
    }
}
