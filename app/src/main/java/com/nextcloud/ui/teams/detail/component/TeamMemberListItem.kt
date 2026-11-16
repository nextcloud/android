/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.compose.component.avatar.RemoteAvatar
import com.nextcloud.ui.teams.util.avatarPath
import com.nextcloud.ui.teams.util.iconId
import com.nextcloud.ui.teams.util.titleId
import com.owncloud.android.R

@Composable
fun TeamMemberListItem(member: TeamMember, client: NextcloudClient?) {
    val name = member.displayName.ifEmpty { member.userId.orEmpty() }

    ListItem(
        headlineContent = { Text(text = name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(stringResource(member.role.titleId())) },
        leadingContent = { TeamMemberAvatar(member, name, client) }
    )
}

@Composable
private fun TeamMemberAvatar(member: TeamMember, name: String, client: NextcloudClient?) {
    val size = dimensionResource(R.dimen.team_avatar_size)
    val path = member.avatarPath(with(LocalDensity.current) { size.roundToPx() })

    if (path != null) {
        RemoteAvatar(client = client, path = path, name = name, size = size)
    } else {
        TeamMemberTypeAvatar(member, size)
    }
}

@Composable
private fun TeamMemberTypeAvatar(member: TeamMember, size: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
    ) {
        Icon(
            painter = painterResource(member.type.iconId()),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
