/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.nextcloud.ui.teams.model.TeamAccess
import com.owncloud.android.R

@Composable
fun TeamAccessFilterRow(selected: TeamAccess?, onSelect: (TeamAccess?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.standard_padding)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.standard_half_padding))
    ) {
        items(TeamAccess.entries) { access ->
            val isSelected = access == selected

            FilterChip(
                selected = isSelected,
                onClick = { onSelect(if (isSelected) null else access) },
                label = { Text(stringResource(access.titleId)) }
            )
        }
    }
}
