/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2023 TSI-mc
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("DEPRECATION")

package com.owncloud.android.ui.trashbin

import android.os.AsyncTask
import com.nextcloud.client.account.User
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.client.network.ClientFactory.CreationException
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.trashbin.EmptyTrashbinRemoteOperation
import com.owncloud.android.lib.resources.trashbin.ReadTrashbinFolderRemoteOperation
import com.owncloud.android.lib.resources.trashbin.RemoveTrashbinFileRemoteOperation
import com.owncloud.android.lib.resources.trashbin.RestoreTrashbinFileRemoteOperation
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile
import com.owncloud.android.ui.trashbin.TrashbinRepository.LoadFolderCallback
import com.owncloud.android.ui.trashbin.TrashbinRepository.OperationCallback

class RemoteTrashbinRepository internal constructor(private val user: User, private val clientFactory: ClientFactory) :
    TrashbinRepository {

    override fun removeTrashbinFile(file: TrashbinFile?, callback: OperationCallback?) {
        RemoveTrashbinFileTask(user, clientFactory, file, callback).execute()
    }

    private class RemoveTrashbinFileTask(
        private val user: User,
        private val clientFactory: ClientFactory,
        private val file: TrashbinFile?,
        private val callback: OperationCallback?
    ) : AsyncTask<Void?, Void?, Boolean>() {

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void?): Boolean {
            return try {
                val client = clientFactory.create(user)
                val result = RemoveTrashbinFileRemoteOperation(file!!.fullRemotePath)
                    .execute(client)
                result.isSuccess
            } catch (e: CreationException) {
                Log_OC.e(this, "Cannot create client", e)
                false
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(success: Boolean) {
            super.onPostExecute(success)
            callback?.onResult(success)
        }
    }

    override fun emptyTrashbin(callback: OperationCallback?) {
        EmptyTrashbinTask(user, clientFactory, callback).execute()
    }

    private class EmptyTrashbinTask(
        private val user: User,
        private val clientFactory: ClientFactory,
        private val callback: OperationCallback?
    ) : AsyncTask<Void?, Void?, Boolean>() {

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void?): Boolean {
            return try {
                val client = clientFactory.createNextcloudClient(user)
                val emptyTrashbinFileOperation = EmptyTrashbinRemoteOperation()
                val result = emptyTrashbinFileOperation.execute(client)
                result.isSuccess
            } catch (e: CreationException) {
                Log_OC.e(this, "Cannot create client", e)
                false
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(success: Boolean) {
            super.onPostExecute(success)
            callback?.onResult(success)
        }
    }

    override fun restoreFile(file: TrashbinFile?, callback: OperationCallback?) {
        RestoreTrashbinFileTask(file, user, clientFactory, callback).execute()
    }

    private class RestoreTrashbinFileTask(
        private val file: TrashbinFile?,
        private val user: User,
        private val clientFactory: ClientFactory,
        private val callback: OperationCallback?
    ) : AsyncTask<Void?, Void?, Boolean>() {

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void?): Boolean {
            return try {
                val client = clientFactory.create(user)
                val result = RestoreTrashbinFileRemoteOperation(
                    file!!.fullRemotePath,
                    file.fileName
                ).execute(client)
                result.isSuccess
            } catch (e: CreationException) {
                Log_OC.e(this, "Cannot create client", e)
                false
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(success: Boolean) {
            super.onPostExecute(success)
            callback?.onResult(success)
        }
    }

    override fun getFolder(remotePath: String?, callback: LoadFolderCallback?) {
        callback?.let {
            ReadRemoteTrashbinFolderTask(remotePath, user, clientFactory, it).execute()
        }
    }

    private class ReadRemoteTrashbinFolderTask(
        private val remotePath: String?,
        private val user: User,
        private val clientFactory: ClientFactory,
        private val callback: LoadFolderCallback
    ) : AsyncTask<Void?, Void?, Boolean>() {
        private var trashbinFiles: List<TrashbinFile?>? = null

        @Deprecated("Deprecated in Java")
        override fun doInBackground(vararg voids: Void?): Boolean {
            return try {
                val client = clientFactory.create(user)
                val result = ReadTrashbinFolderRemoteOperation(remotePath).execute(client)
                if (result.isSuccess) {
                    trashbinFiles = result.resultData
                    true
                } else {
                    false
                }
            } catch (e: CreationException) {
                false
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onPostExecute(success: Boolean) {
            super.onPostExecute(success)

            if (success) {
                callback.onSuccess(trashbinFiles)
            } else {
                callback.onError(R.string.trashbin_loading_failed)
            }
        }
    }
}
