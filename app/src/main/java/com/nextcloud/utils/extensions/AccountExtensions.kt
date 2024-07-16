/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.accounts.Account
import android.content.Context
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.NextcloudVersion

fun Account.isAnonymous(context: Context): Boolean = type.equals(context.getString(R.string.anonymous_account_type))

fun UserAccountManager.isServerVersionNewerOrEqual(version: NextcloudVersion): Boolean {
    return user.server.version.isNewerOrEqual(version)
}
