/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
                if (result.isSuccess && result.resultData != null) {
                    result.resultData!!
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
