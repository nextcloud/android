/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2023 Tobias Kaminsky
 * Copyright (C) 2023 Nextcloud GmbH
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
package com.owncloud.android.ui.asynctasks

import android.os.AsyncTask
import com.nextcloud.android.lib.resources.groupfolders.GetGroupfoldersRemoteOperation
import com.nextcloud.android.lib.resources.groupfolders.Groupfolder
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.ui.fragment.GroupfolderListFragment
import java.lang.ref.WeakReference

class GroupfoldersSearchTask(
    fragment: GroupfolderListFragment,
    private val user: User,
    storageManager: FileDataStorageManager
) : AsyncTask<Void, Void, Map<String, Groupfolder>>() {
    private val fragmentWeakReference: WeakReference<GroupfolderListFragment?>
    private val storageManager: FileDataStorageManager

    init {
        fragmentWeakReference = WeakReference(fragment)
        this.storageManager = storageManager
    }

    override fun doInBackground(vararg voids: Void): Map<String, Groupfolder> {
        if (fragmentWeakReference.get() == null) {
            return HashMap()
        }
        val fragment = fragmentWeakReference.get()
        return if (isCancelled) {
            HashMap()
        } else {
            val searchRemoteOperation = GetGroupfoldersRemoteOperation()
            if (fragment?.context != null) {
                val result = searchRemoteOperation.executeNextcloudClient(
                    user,
                    fragment.requireContext()
                )
                if (result.isSuccess) {
                    result.resultData
                } else {
                    HashMap()
                }
            } else {
                HashMap()
            }
        }
    }

    override fun onPostExecute(result: Map<String, Groupfolder>) {
        fragmentWeakReference.get()?.setData(result)
    }
}
