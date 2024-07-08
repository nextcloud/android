/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nextcloud.android.lib.resources.groupfolders.Groupfolder
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.logger.Logger
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.ui.EmptyRecyclerView
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.GroupfolderListAdapter
import com.owncloud.android.ui.asynctasks.GroupfoldersSearchTask
import com.owncloud.android.ui.interfaces.GroupfolderListInterface
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchFragment = true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        currentSearchType = SearchType.GROUPFOLDER
        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT
        requireActivity().invalidateOptionsMenu()

        search()
    }

    public override fun setAdapter(args: Bundle?) {
        adapter = GroupfolderListAdapter(requireContext(), viewThemeUtils, this)
        setRecyclerViewAdapter(adapter)

        val layoutManager = LinearLayoutManager(context)
        recyclerView?.layoutManager = layoutManager
        (recyclerView as EmptyRecyclerView).setHasFooter(false)
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

    override fun onRefresh() {
        super.onRefresh()

        search()
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
                if (fetchResult.isException && fetchResult.exception != null) {
                    logger.e(SHARED_TAG, "exception: ", fetchResult.exception!!)
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

    companion object {
        private val SHARED_TAG = GroupfolderListFragment::class.java.simpleName
    }

    override fun onFolderClick(path: String) {
        MainApp.showOnlyFilesOnDevice(false)
        Intent(
            context,
            FileDisplayActivity::class.java
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = ACTION_VIEW
            putExtra(FileDisplayActivity.KEY_FILE_PATH, path)
            startActivity(this)
        }
    }
}
