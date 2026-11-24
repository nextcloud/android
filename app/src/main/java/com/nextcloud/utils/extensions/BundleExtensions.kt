/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils.extensions

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.os.Parcelable
import androidx.core.os.BundleCompat
import java.io.Serializable

fun Bundle.buildAccount(account: Account, authToken: String?, accountType: String = account.type): Bundle =
    apply {
        putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
        putString(AccountManager.KEY_ACCOUNT_TYPE, accountType)
        putString(AccountManager.KEY_AUTHTOKEN, authToken)
    }

fun <T : Serializable?> Bundle?.getSerializableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return BundleCompat.getSerializable(this, key, type)
}

fun <T : Parcelable?> Bundle?.getParcelableArgument(key: String, type: Class<T>): T? {
    if (this == null) {
        return null
    }

    return BundleCompat.getParcelable(this, key, type)
}
