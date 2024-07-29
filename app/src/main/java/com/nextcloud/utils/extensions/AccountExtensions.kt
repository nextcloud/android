/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import android.accounts.Account
import android.content.Context
import com.owncloud.android.R

fun Account.isAnonymous(context: Context): Boolean = type.equals(context.getString(R.string.anonymous_account_type))
