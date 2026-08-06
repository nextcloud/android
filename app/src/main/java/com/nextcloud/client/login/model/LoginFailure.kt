/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.login.model

import androidx.annotation.StringRes
import com.owncloud.android.R

enum class LoginFailure(@field:StringRes @param:StringRes val messageId: Int) {
    EMPTY_SERVER_URL(R.string.authenticator_activity_empty_base_url),
    EMPTY_RESPONSE(R.string.authenticator_activity_empty_response_message),
    MALFORMED_RESPONSE(R.string.authenticator_activity_login_error),
    TIMED_OUT(R.string.authenticator_activity_login_timeout)
}
