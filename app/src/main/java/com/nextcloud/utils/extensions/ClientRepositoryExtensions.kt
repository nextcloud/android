/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import com.nextcloud.repository.ClientRepository
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun <T> ClientRepository.execute(operation: RemoteOperation<T>): RemoteOperationResult<T> {
    val client = getNextcloudClient() ?: return RemoteOperationResult(ResultCode.ACCOUNT_NOT_FOUND)
    return withContext(Dispatchers.IO) { operation.execute(client) }
}
