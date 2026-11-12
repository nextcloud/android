/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.nextcloud.android.lib.resources.teams.get.resources.model.TeamResource
import com.nextcloud.ui.teams.detail.model.TeamResourceProviderId
import com.owncloud.android.R

@Composable
fun TeamResourceListItem(resource: TeamResource, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text = resource.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(resource.provider.name) },
        leadingContent = {
            Icon(
                painter = painterResource(iconId(resource.provider.id)),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimensionResource(R.dimen.iconized_single_line_item_icon_size))
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@DrawableRes
private fun iconId(providerId: String): Int = when (providerId) {
    TeamResourceProviderId.TEAM_FOLDER -> R.drawable.folder
    TeamResourceProviderId.TALK -> R.drawable.ic_talk
    TeamResourceProviderId.DECK -> R.drawable.ic_deck
    else -> R.drawable.ic_link
}
