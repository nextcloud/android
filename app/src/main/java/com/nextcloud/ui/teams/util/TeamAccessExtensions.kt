/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.util

import com.nextcloud.android.lib.resources.teams.model.Team
import com.nextcloud.android.lib.resources.teams.model.TeamRole
import com.nextcloud.ui.teams.model.TeamAccess

val Team.access: TeamAccess
    get() {
        val initiator = initiator
        return when {
            initiator == null || !initiator.hasRole -> TeamAccess.PENDING
            initiator.role == TeamRole.MEMBER -> TeamAccess.MEMBER
            else -> TeamAccess.MANAGER
        }
    }
