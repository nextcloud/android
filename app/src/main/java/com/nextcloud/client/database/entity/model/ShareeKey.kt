/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.entity.model

import androidx.room.ColumnInfo
import com.owncloud.android.lib.resources.shares.ShareType

data class ShareeKey(
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "shate_with") val shareWith: String?,
    @ColumnInfo(name = "share_type") val shareType: Int
) {
    companion object {
        val shareableShareTypeValues = listOf(
            ShareType.USER,
            ShareType.GROUP,
            ShareType.EMAIL,
            ShareType.FEDERATED,
            ShareType.FEDERATED_GROUP,
            ShareType.ROOM,
            ShareType.CIRCLE
        ).map { it.value }
    }
}
