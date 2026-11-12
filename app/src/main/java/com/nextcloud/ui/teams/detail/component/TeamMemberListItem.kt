/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.annotation.StringRes
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.android.lib.resources.teams.model.TeamRole
import com.owncloud.android.R

@Composable
fun TeamMemberListItem(member: TeamMember) {
    ListItem(
        headlineContent = {
            Text(
                text = member.displayName.ifEmpty { member.userId.orEmpty() },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = { Text(stringResource(roleTitleId(member.role))) },
        leadingContent = { TeamMemberIcon(member.type) }
    )
}

@StringRes
private fun roleTitleId(role: TeamRole): Int = when (role) {
    TeamRole.OWNER -> R.string.team_role_owner
    TeamRole.ADMIN -> R.string.team_role_admin
    TeamRole.MODERATOR -> R.string.team_role_moderator
    TeamRole.MEMBER -> R.string.team_role_member
}
