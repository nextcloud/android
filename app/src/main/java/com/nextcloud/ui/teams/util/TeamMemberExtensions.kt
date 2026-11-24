/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.util

import com.nextcloud.android.lib.resources.teams.model.TeamMember
import com.nextcloud.android.lib.resources.teams.model.TeamMemberType

private const val LEVEL_NONE = 0

val TeamMember.hasRole: Boolean
    get() = level > LEVEL_NONE

val TeamMember.isUser: Boolean
    get() = type == TeamMemberType.USER
