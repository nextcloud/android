/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.files

import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.activities.data.files.FilesServiceApi.FilesServiceCallback
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Implementation of the Files service API that communicates with the NextCloud remote server.
 */
class FilesServiceApiImpl(private val accountManager: UserAccountManager, private val clientFactory: ClientFactory) :
    FilesServiceApi {

    override fun readRemoteFile(fileUrl: String, activity: BaseActivity, callback: FilesServiceCallback<OCFile>) {
        CoroutineScope(Dispatchers.Main).launch {
            val result = runCatching {
                withContext(Dispatchers.IO) { fetchRemoteFile(fileUrl, activity) }
            }

            result.fold(
                onSuccess = { ocFile ->
                    if (ocFile != null) {
                        callback.onLoaded(ocFile)
                    } else {
                        callback.onError(activity.getString(R.string.file_not_found))
                    }
                },
                onFailure = { error ->
                    val message = when (error) {
                        is CreationException -> activity.getString(R.string.account_not_found)
                        else -> error.message
                    }
                    Log_OC.e(TAG, "Failed to read remote file", error)
                    callback.onError(message ?: "")
                }
            )
        }
    }

    private fun fetchRemoteFile(fileUrl: String?, activity: BaseActivity): OCFile? {
        val context = MainApp.getAppContext()
        val client = clientFactory.create(accountManager.user)
        val result = ReadFileRemoteOperation(fileUrl).execute(client)

        if (!result.isSuccess) return null

        val remoteFile = result.getData()[0] as RemoteFile
        val ocFile = activity.storageManager.saveFileWithParent(
            FileStorageUtils.fillOCFile(remoteFile),
            context
        )

        if (ocFile.isFolder) {
            RefreshFolderOperation(
                ocFile,
                System.currentTimeMillis(),
                false,
                true,
                activity.storageManager,
                activity.user.orElseThrow { RuntimeException() },
                context
            ).execute(client)
        }

        return ocFile
    }

    companion object {
        private val TAG: String = FilesServiceApiImpl::class.java.simpleName
    }
}
