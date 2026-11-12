/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.detail.model

import androidx.annotation.StringRes
import com.nextcloud.utils.extensions.connectionErrorMessageId
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import java.net.HttpURLConnection

@StringRes
fun RemoteOperationResult<*>.loadTeamDetailErrorMessageId(): Int = connectionErrorMessageId() ?: when (httpCode) {
    HttpURLConnection.HTTP_NOT_FOUND -> R.string.team_error_not_found
    else -> R.string.team_load_failed
}

@StringRes
fun RemoteOperationResult<*>.addTeamMemberErrorMessageId(): Int = connectionErrorMessageId() ?: when (httpCode) {
    HttpURLConnection.HTTP_FORBIDDEN -> R.string.team_error_add_member_not_allowed
    HttpURLConnection.HTTP_BAD_REQUEST -> R.string.team_error_add_member_invalid
    else -> R.string.team_add_member_failed
}
