/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.owncloud.android.datamodel

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Template for creating a file from it via RichDocuments app
 */
@Parcelize
data class Template(
    val id: Long,
    val name: String,
    val thumbnailLink: String,
    val type: Type,
    val extension: String
) : Parcelable {
    enum class Type {
        DOCUMENT, SPREADSHEET, PRESENTATION, UNKNOWN;

        companion object {
            @JvmStatic
            fun parse(name: String) = try {
                valueOf(name.uppercase())
            } catch (_: IllegalArgumentException) {
                UNKNOWN
            }
        }
    }
}
