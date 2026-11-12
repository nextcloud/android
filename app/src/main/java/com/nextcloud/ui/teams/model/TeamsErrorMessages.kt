/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.teams.model

import androidx.annotation.StringRes
import com.nextcloud.utils.extensions.connectionErrorMessageId
import com.owncloud.android.R
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import java.net.HttpURLConnection

private const val HTTP_TOO_MANY_REQUESTS = 429

@StringRes
fun RemoteOperationResult<*>.loadTeamsErrorMessageId(): Int = connectionErrorMessageId() ?: when (httpCode) {
    HttpURLConnection.HTTP_NOT_FOUND -> R.string.teams_error_not_available
    else -> R.string.teams_load_failed
}

@StringRes
fun RemoteOperationResult<*>.createTeamErrorMessageId(): Int = connectionErrorMessageId() ?: when (httpCode) {
    HttpURLConnection.HTTP_BAD_REQUEST -> R.string.teams_error_invalid_name
    HttpURLConnection.HTTP_FORBIDDEN -> R.string.teams_error_create_not_allowed
    HttpURLConnection.HTTP_NOT_FOUND -> R.string.teams_error_not_available
    HTTP_TOO_MANY_REQUESTS -> R.string.teams_error_too_many_requests
    else -> R.string.teams_create_failed
}
