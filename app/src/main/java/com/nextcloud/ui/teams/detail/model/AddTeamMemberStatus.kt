/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.model

import androidx.annotation.StringRes

sealed interface AddTeamMemberStatus {
    data object Editing : AddTeamMemberStatus

    data object Adding : AddTeamMemberStatus

    data class Failed(@StringRes val messageId: Int) : AddTeamMemberStatus
}
