/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.authentication

import com.nextcloud.model.HTTPStatusCodes

data class LoginUrlInfo(var server: String, var loginName: String, var appPassword: String) {
    fun isValid(status: Int): Boolean = (
        status == HTTPStatusCodes.SUCCESS.code &&
            server.isNotEmpty() && loginName.isNotEmpty() && appPassword.isNotEmpty()
        )
}
