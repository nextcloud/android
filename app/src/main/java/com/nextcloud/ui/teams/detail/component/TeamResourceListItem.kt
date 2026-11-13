/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

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
import com.nextcloud.ui.teams.util.iconId
import com.owncloud.android.R

@Composable
fun TeamResourceListItem(resource: TeamResource, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(text = resource.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(resource.provider.name) },
        leadingContent = {
            Icon(
                painter = painterResource(resource.provider.iconId()),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(dimensionResource(R.dimen.iconized_single_line_item_icon_size))
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
