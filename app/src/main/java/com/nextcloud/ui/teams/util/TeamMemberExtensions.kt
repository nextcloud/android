/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.util

import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.android.lib.resources.teams.model.TeamMemberType
import com.nextcloud.ui.teams.model.TeamAccess
import com.nextcloud.ui.teams.model.access

val TeamMember.isUser: Boolean
    get() = type == TeamMemberType.USER

fun TeamMember.matches(query: String, accessFilter: TeamAccess?): Boolean {
    if (accessFilter != null && access != accessFilter) {
        return false
    }

    val trimmedQuery = query.trim()

    return displayName.contains(trimmedQuery, ignoreCase = true) ||
        userId.orEmpty().contains(trimmedQuery, ignoreCase = true)
}
