/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.ui.teams.util.iconId
import com.nextcloud.ui.teams.util.titleId

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
        supportingContent = { Text(stringResource(member.role.titleId())) },
        leadingContent = {
            Icon(
                painter = painterResource(member.type.iconId()),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )
}
