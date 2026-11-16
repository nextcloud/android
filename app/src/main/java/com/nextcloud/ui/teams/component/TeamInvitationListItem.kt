/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.android.lib.resources.teams.put.invitation.model.TeamInvitationResponse
import com.nextcloud.common.NextcloudClient
import com.nextcloud.ui.compose.component.avatar.RemoteAvatar
import com.nextcloud.ui.teams.util.avatarPath
import com.nextcloud.ui.teams.util.titleId
import com.owncloud.android.R

@Composable
fun TeamInvitationListItem(
    team: Team,
    client: NextcloudClient,
    isResponding: Boolean,
    onRespond: (TeamInvitationResponse) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(text = team.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_padding)),
                modifier = Modifier.padding(top = dimensionResource(R.dimen.standard_half_padding))
            ) {
                FilledTonalButton(enabled = !isResponding, onClick = { onRespond(TeamInvitationResponse.ACCEPT) }) {
                    Text(stringResource(TeamInvitationResponse.ACCEPT.titleId()))
                }
                OutlinedButton(enabled = !isResponding, onClick = { onRespond(TeamInvitationResponse.DECLINE) }) {
                    Text(stringResource(TeamInvitationResponse.DECLINE.titleId()))
                }
            }
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
