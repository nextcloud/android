/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.util

import androidx.annotation.DrawableRes
import com.nextcloud.android.lib.resources.teams.model.TeamMemberType
import com.owncloud.android.R

@DrawableRes
fun TeamMemberType?.iconId(): Int = when (this) {
    TeamMemberType.GROUP -> R.drawable.ic_group
    TeamMemberType.MAIL -> R.drawable.ic_email
    TeamMemberType.CIRCLE -> R.drawable.ic_circles
    TeamMemberType.USER, TeamMemberType.CONTACT, null -> R.drawable.ic_user
}
