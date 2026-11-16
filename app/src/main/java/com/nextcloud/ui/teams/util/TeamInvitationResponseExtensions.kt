/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.util

import androidx.annotation.StringRes
import com.nextcloud.android.lib.resources.teams.put.invitation.model.TeamInvitationResponse
import com.owncloud.android.R

@StringRes
fun TeamInvitationResponse.titleId(): Int = when (this) {
    TeamInvitationResponse.ACCEPT -> R.string.teams_invitation_accept
    TeamInvitationResponse.DECLINE -> R.string.teams_invitation_decline
}

@StringRes
fun TeamInvitationResponse.failedMessageId(): Int = when (this) {
    TeamInvitationResponse.ACCEPT -> R.string.teams_invitation_accept_failed
    TeamInvitationResponse.DECLINE -> R.string.teams_invitation_decline_failed
}
