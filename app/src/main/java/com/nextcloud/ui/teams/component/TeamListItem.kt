/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.component

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.compose.component.avatar.RemoteAvatar
import com.nextcloud.ui.teams.util.avatarPath
import com.owncloud.android.R

@Composable
fun TeamListItem(team: Team, client: NextcloudClient, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = {
            Text(text = team.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(pluralStringResource(R.plurals.teams_member_count, team.population, team.population))
        },
        leadingContent = {
            RemoteAvatar(
                client = client,
                path = team.avatarPath,
                name = team.displayName,
                size = dimensionResource(R.dimen.team_avatar_size)
            )
        }
    )
}
