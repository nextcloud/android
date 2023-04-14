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
package com.owncloud.android.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.nextcloud.android.lib.resources.groupfolders.Groupfolder
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.logger.Logger
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.GroupfolderListAdapter
import com.owncloud.android.ui.asynctasks.GroupfoldersSearchTask
import com.owncloud.android.ui.interfaces.GroupfolderListInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * A Fragment that lists groupfolders
 */
class GroupfolderListFragment : OCFileListFragment(), Injectable, GroupfolderListInterface {

    lateinit var adapter: GroupfolderListAdapter

    @Inject
    lateinit var logger: Logger

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchFragment = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        currentSearchType = SearchType.GROUPFOLDER
        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT
        requireActivity().invalidateOptionsMenu()

        search()
    }

    override fun setAdapter(args: Bundle?) {
        adapter = GroupfolderListAdapter(requireContext(), viewThemeUtils, this)
        setRecyclerViewAdapter(adapter)

        val layoutManager = GridLayoutManager(context, 1)
        recyclerView.layoutManager = layoutManager
    }

    private fun search() {
        GroupfoldersSearchTask(
            this,
            accountManager.user,
            mContainerActivity.storageManager
        ).execute()
    }

    override fun onResume() {
        super.onResume()
        Handler().post {
            if (activity is FileDisplayActivity) {
                val fileDisplayActivity = activity as FileDisplayActivity
                fileDisplayActivity.updateActionBarTitleAndHomeButtonByString(
                    getString(R.string.drawer_item_groupfolders)
                )
                fileDisplayActivity.setMainFabVisible(false)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setData(result: Map<String, Groupfolder>) {
        adapter.setData(result)
        adapter.notifyDataSetChanged()
    }

    private suspend fun fetchFileData(partialFile: OCFile): OCFile? {
        return withContext(Dispatchers.IO) {
            val user = accountManager.user
            val fetchResult = ReadFileRemoteOperation(partialFile.remotePath).execute(user, context)
            if (!fetchResult.isSuccess) {
                logger.e(SHARED_TAG, "Error fetching file")
                if (fetchResult.isException) {
                    logger.e(SHARED_TAG, "exception: ", fetchResult.exception)
                }
                null
            } else {
                val remoteFile = fetchResult.data[0] as RemoteFile
                val file = FileStorageUtils.fillOCFile(remoteFile)
                FileStorageUtils.searchForLocalFileInDefaultPath(file, user.accountName)
                val savedFile = mContainerActivity.storageManager.saveFileWithParent(file, context)
                savedFile.apply {
                    isSharedViaLink = partialFile.isSharedViaLink
                    isSharedWithSharee = partialFile.isSharedWithSharee
                    sharees = partialFile.sharees
                }
            }
        }
    }

    private fun fetchFileAndRun(partialFile: OCFile, block: (file: OCFile) -> Unit) {
        lifecycleScope.launch {
            isLoading = true
            val file = fetchFileData(partialFile)
            isLoading = false
            if (file != null) {
                block(file)
            } else {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.error_retrieving_file)
            }
        }
    }

    override fun onShareIconClick(file: OCFile) {
        fetchFileAndRun(file) { fetched ->
            super.onShareIconClick(fetched)
        }
    }

    override fun showShareDetailView(file: OCFile) {
        fetchFileAndRun(file) { fetched ->
            super.showShareDetailView(fetched)
        }
    }

    override fun showActivityDetailView(file: OCFile) {
        fetchFileAndRun(file) { fetched ->
            super.showActivityDetailView(fetched)
        }
    }

    override fun onOverflowIconClicked(file: OCFile, view: View?) {
        fetchFileAndRun(file) { fetched ->
            super.onOverflowIconClicked(fetched, view)
        }
    }

    override fun onItemClicked(file: OCFile) {
        fetchFileAndRun(file) { fetched ->
            super.onItemClicked(fetched)
        }
    }

    override fun onLongItemClicked(file: OCFile): Boolean {
        fetchFileAndRun(file) { fetched ->
            super.onLongItemClicked(fetched)
        }
        return true
    }

    companion object {
        private val SHARED_TAG = GroupfolderListFragment::class.java.simpleName
    }

    override fun onFolderClick(path: String) {
        Log_OC.d("groupfolder", path)
    }
}
