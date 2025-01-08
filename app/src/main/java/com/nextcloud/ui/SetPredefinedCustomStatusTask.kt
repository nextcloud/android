/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.ui

import android.accounts.Account
import android.content.Context
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.users.SetPredefinedCustomStatusMessageRemoteOperation

class SetPredefinedCustomStatusTask(
    val messageId: String,
    val clearAt: Long?,
    val account: Account?,
    val context: Context?
) : Function0<Boolean> {
    override fun invoke(): Boolean = try {
        val client = OwnCloudClientFactory.createNextcloudClient(account, context)

        SetPredefinedCustomStatusMessageRemoteOperation(messageId, clearAt).execute(client).isSuccess
    } catch (e: AccountUtils.AccountNotFoundException) {
        Log_OC.e(this, "Error setting predefined status", e)

        false
    }
}
