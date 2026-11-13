/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.util

import androidx.annotation.StringRes
import com.nextcloud.android.lib.resources.teams.model.TeamRole
import com.owncloud.android.R

@StringRes
fun TeamRole.titleId(): Int = when (this) {
    TeamRole.OWNER -> R.string.team_role_owner
    TeamRole.ADMIN -> R.string.team_role_admin
    TeamRole.MODERATOR -> R.string.team_role_moderator
    TeamRole.MEMBER -> R.string.team_role_member
}
