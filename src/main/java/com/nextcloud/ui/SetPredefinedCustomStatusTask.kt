/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.ui

import android.accounts.Account
import android.content.Context
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.users.SetPredefinedCustomStatusMessageRemoteOperation

public class SetPredefinedCustomStatusTask(
    val messageId: String,
    val clearAt: Long?,
    val account: Account?,
    val context: Context?
) : Function0<Boolean> {
    override fun invoke(): Boolean {
        return try {
            val client = OwnCloudClientFactory.createNextcloudClient(account, context)

            SetPredefinedCustomStatusMessageRemoteOperation(messageId, clearAt).execute(client).isSuccess
        } catch (e: AccountUtils.AccountNotFoundException) {
            Log_OC.e(this, "Error setting predefined status", e)

            false
        }
    }
}
