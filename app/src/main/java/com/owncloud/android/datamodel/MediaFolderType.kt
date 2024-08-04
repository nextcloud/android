/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import android.util.SparseArray

/**
 * Types of media folder.
 */
enum class MediaFolderType(@JvmField val id: Int) {
    CUSTOM(0),
    IMAGE(1),
    VIDEO(2);

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
