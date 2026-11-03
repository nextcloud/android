/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.drawer.quota

import com.owncloud.android.lib.common.Quota
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import com.owncloud.android.ui.activity.BaseActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserQuotaFetcher(private val activity: BaseActivity) {

    suspend fun fetch(): Quota? = withContext(Dispatchers.IO) {
        if (activity.userAccountManager.user.isAnonymous) return@withContext null
        val client = activity.clientRepository.getNextcloudClient() ?: return@withContext null
        val result = GetUserInfoRemoteOperation().execute(client)
        if (result.isSuccess) result.resultData?.quota else null
    }
}
