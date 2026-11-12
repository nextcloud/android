/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.model

import androidx.annotation.StringRes
import com.owncloud.android.R

enum class TeamDetailTab(@StringRes val titleId: Int) {
    RESOURCES(R.string.team_tab_resources),
    MEMBERS(R.string.team_tab_members)
}
