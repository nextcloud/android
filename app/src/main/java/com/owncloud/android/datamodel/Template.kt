/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
        DOCUMENT,
        SPREADSHEET,
        PRESENTATION,
        UNKNOWN;

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
