/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.nextcloud.android.lib.resources.teams.model.TeamMemberType
import com.owncloud.android.R

@Composable
fun TeamMemberIcon(type: TeamMemberType?) {
    val iconId = when (type) {
        TeamMemberType.GROUP -> R.drawable.ic_group
        TeamMemberType.MAIL -> R.drawable.ic_email
        TeamMemberType.CIRCLE -> R.drawable.ic_circles
        TeamMemberType.USER, TeamMemberType.CONTACT, null -> R.drawable.ic_user
    }
    Icon(painter = painterResource(iconId), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
}
