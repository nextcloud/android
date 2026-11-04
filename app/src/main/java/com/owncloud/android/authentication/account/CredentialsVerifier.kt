/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013-2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.authentication.account

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentials
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CredentialsVerifier(private val context: Context) {

    suspend fun verify(url: String, credentials: OwnCloudCredentials): RemoteOperationResult<UserInfo?> =
        withContext(Dispatchers.IO) {
            val uri = url.toUri()
            val nextcloudClient = OwnCloudClientFactory.createNextcloudClient(
                uri,
                credentials.username,
                credentials.toOkHttpCredentials(),
                context,
                true
            )

            val userInfoResult: RemoteOperationResult<UserInfo?> =
                GetUserInfoRemoteOperation().execute(nextcloudClient)
            if (!userInfoResult.isSuccess) {
                return@withContext userInfoResult
            }

            checkRootFolderAccess(uri, credentials, userInfoResult.resultData)
        }

    @Suppress("DEPRECATION")
    private fun checkRootFolderAccess(
        uri: Uri,
        credentials: OwnCloudCredentials,
        userInfo: UserInfo?
    ): RemoteOperationResult<UserInfo?> {
        val client = OwnCloudClientFactory.createOwnCloudClient(uri, context, true).apply {
            userId = userInfo?.id
            this.credentials = credentials
        }
        val operation = ExistenceCheckRemoteOperation(OCFile.ROOT_PATH, SUCCESS_IF_ABSENT)

        @Suppress("UNCHECKED_CAST")
        val result = operation.execute(client) as RemoteOperationResult<UserInfo?>
        if (operation.wasRedirected()) {
            result.lastPermanentLocation = operation.redirectionPath.lastPermanentLocation
        }
        result.setResultData(userInfo)
        return result
    }

    companion object {
        private const val SUCCESS_IF_ABSENT = false
    }
}
