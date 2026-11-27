/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.model

import androidx.annotation.StringRes
import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.owncloud.android.R

private const val LEVEL_NONE = 0
private const val LEVEL_MEMBER = 1
private const val LEVEL_MODERATOR = 4
private const val LEVEL_ADMIN = 8
private const val LEVEL_OWNER = 9

enum class TeamAccess(val level: Int, @StringRes val titleId: Int) {
    PENDING(LEVEL_NONE, R.string.team_role_pending),
    MEMBER(LEVEL_MEMBER, R.string.team_role_member),
    MODERATOR(LEVEL_MODERATOR, R.string.team_role_moderator),
    ADMIN(LEVEL_ADMIN, R.string.team_role_admin),
    OWNER(LEVEL_OWNER, R.string.team_role_owner);

    val isManager: Boolean
        get() = this >= MODERATOR

    companion object {
        fun fromLevel(level: Int): TeamAccess = entries.lastOrNull { level >= it.level } ?: PENDING
    }
}

val TeamMember.access: TeamAccess
    get() = TeamAccess.fromLevel(level)

val Team.access: TeamAccess
    get() = initiator?.access ?: TeamAccess.PENDING
