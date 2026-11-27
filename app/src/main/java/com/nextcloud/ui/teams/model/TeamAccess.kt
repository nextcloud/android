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

enum class TeamAccess(val level: Int, @StringRes val titleId: Int) {
    PENDING(0, R.string.team_role_pending),
    MEMBER(1, R.string.team_role_member),
    MODERATOR(4, R.string.team_role_moderator),
    ADMIN(8, R.string.team_role_admin),
    OWNER(9, R.string.team_role_owner);

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
