/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2013-2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
@file:Suppress("DEPRECATION")

package com.owncloud.android.authentication

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.AsyncTask
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.common.OwnCloudCredentials
import com.owncloud.android.lib.common.UserInfo
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation
import java.lang.ref.WeakReference

/**
 * Async Task to verify the credentials of a user.
 */
class AuthenticatorAsyncTask(activity: Activity) : AsyncTask<Any?, Void?, RemoteOperationResult<UserInfo?>?>() {
    private val mWeakContext: WeakReference<Context?>
    private val mListener: WeakReference<OnAuthenticatorTaskListener>

    init {
        mWeakContext = WeakReference(activity.applicationContext)
        mListener = WeakReference(activity as OnAuthenticatorTaskListener)
    }

    @Deprecated("Deprecated in Java")
    override fun doInBackground(vararg params: Any?): RemoteOperationResult<UserInfo?> {
        val result: RemoteOperationResult<UserInfo?>

        if (params.size == 2 && mWeakContext.get() != null) {
            val url = params[0] as String
            val credentials = params[1] as OwnCloudCredentials
            val context = mWeakContext.get()

            // Client
            val uri = Uri.parse(url)
            val nextcloudClient = OwnCloudClientFactory.createNextcloudClient(
                uri,
                credentials.username,
                credentials.toOkHttpCredentials(),
                context,
                true
            )

            // Operation - get display name
            val userInfoResult = GetUserInfoRemoteOperation().execute(nextcloudClient)

            // Operation - try credentials
            if (userInfoResult.isSuccess) {
                val client = OwnCloudClientFactory.createOwnCloudClient(uri, context, true)
                client.userId = userInfoResult.resultData?.id
                client.credentials = credentials
                val operation = ExistenceCheckRemoteOperation(OCFile.ROOT_PATH, SUCCESS_IF_ABSENT)

                @Suppress("UNCHECKED_CAST")
                result = operation.execute(client) as RemoteOperationResult<UserInfo?>
                if (operation.wasRedirected()) {
                    val redirectionPath = operation.redirectionPath
                    val permanentLocation = redirectionPath.lastPermanentLocation
                    result.lastPermanentLocation = permanentLocation
                }
                result.setResultData(userInfoResult.resultData)
            } else {
                result = userInfoResult
            }
        } else {
            result = RemoteOperationResult<UserInfo?>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
        }

        return result
    }

    @Deprecated("Deprecated in Java")
    override fun onPostExecute(result: RemoteOperationResult<UserInfo?>?) {
        result?.let {
            val listener = mListener.get()
            listener?.onAuthenticatorTaskCallback(it)
        }
    }

    /*
     * Interface to retrieve data from recognition task
     */
    interface OnAuthenticatorTaskListener {
        fun onAuthenticatorTaskCallback(result: RemoteOperationResult<UserInfo?>?)
    }

    companion object {
        private const val SUCCESS_IF_ABSENT = false
    }
}
