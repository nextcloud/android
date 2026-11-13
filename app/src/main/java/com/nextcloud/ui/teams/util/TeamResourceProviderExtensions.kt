/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.util

import androidx.annotation.DrawableRes
import com.nextcloud.android.lib.resources.teams.get.resources.model.TeamResourceProvider
import com.nextcloud.ui.teams.detail.model.TeamResourceProviderId
import com.owncloud.android.R

@DrawableRes
fun TeamResourceProvider.iconId(): Int = when (id) {
    TeamResourceProviderId.FILES -> R.drawable.file
    TeamResourceProviderId.TEAM_FOLDER -> R.drawable.folder
    TeamResourceProviderId.TALK -> R.drawable.ic_talk
    TeamResourceProviderId.DECK -> R.drawable.ic_deck
    TeamResourceProviderId.COLLECTIVES -> R.drawable.ic_notes
    else -> R.drawable.ic_link
}
