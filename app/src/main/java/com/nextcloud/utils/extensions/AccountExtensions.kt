/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.accounts.Account
import android.accounts.AccountManager
import com.owncloud.android.MainApp
import com.owncloud.android.lib.common.accounts.AccountUtils

fun Account.userId(): String = AccountManager
    .get(MainApp.getAppContext())
    .getUserData(this, AccountUtils.Constants.KEY_USER_ID)
