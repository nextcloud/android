/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.tags.repository

import com.owncloud.android.lib.resources.tags.Tag

interface TagManagementRepository {
    suspend fun fetch(fileId: Long, currentTags: List<Tag>): List<Tag>
    suspend fun assignTag(fileId: Long, tag: Tag): Boolean
    suspend fun unassignTag(fileId: Long, tag: Tag): Boolean
    suspend fun createAndAssignTag(
        fileId: Long,
        name: String,
    ): Pair<List<Tag>,String>?
}
