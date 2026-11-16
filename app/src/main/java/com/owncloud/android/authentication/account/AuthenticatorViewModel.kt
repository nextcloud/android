/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.authentication.account

import androidx.lifecycle.ViewModel
import com.owncloud.android.authentication.loginurl.LoginUrlInfo

class AuthenticatorViewModel : ViewModel() {
    var credentials: LoginUrlInfo? = null
}
