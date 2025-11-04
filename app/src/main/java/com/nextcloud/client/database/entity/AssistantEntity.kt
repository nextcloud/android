/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.owncloud.android.db.ProviderMeta

@Entity(tableName = ProviderMeta.ProviderTableMeta.ASSISTANT_TABLE_NAME)
data class AssistantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val accountName: String?,
    val type: String?,
    val status: String?,
    val userId: String?,
    val appId: String?,
    val input: String? = null,
    val output: String? = null,
    val completionExpectedAt: Int? = null,
    var progress: Int? = null,
    val lastUpdated: Int? = null,
    val scheduledAt: Int? = null,
    val endedAt: Int? = null
)
