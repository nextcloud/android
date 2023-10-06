/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2019 Tobias Kaminsky
 * Copyright (C) 2019 Nextcloud GmbH
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

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.logger.Logger
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.SearchRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.GetSharesRemoteOperation
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.events.SearchEvent
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FileStorageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * A Fragment that lists folders shared by the user
 */
@Suppress("TooManyFunctions")
class SharedListFragment : OCFileListFragment(), Injectable {

    @Inject
    lateinit var logger: Logger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        searchFragment = true
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        adapter.setShowMetadata(false)
        currentSearchType = SearchType.SHARED_FILTER
        searchEvent = SearchEvent("", SearchRemoteOperation.SearchType.SHARED_FILTER)
        menuItemAddRemoveValue = MenuItemAddRemove.REMOVE_GRID_AND_SORT
        requireActivity().invalidateOptionsMenu()
    }

    override fun onResume() {
        super.onResume()
        Handler().post {
            if (activity is FileDisplayActivity) {
                val fileDisplayActivity = activity as FileDisplayActivity
                fileDisplayActivity.updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_shared))
                fileDisplayActivity.setMainFabVisible(false)
            }
        }
    }

    override fun getSearchRemoteOperation(currentUser: User?, event: SearchEvent?): RemoteOperation<*> {
        return GetSharesRemoteOperation()
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
                savedFile
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

    private fun fetchAllAndRun(partialFiles: MutableSet<OCFile>?, callback: (MutableSet<OCFile>?) -> Unit) {
        lifecycleScope.launch {
            isLoading = true
            if (partialFiles != null) {
                val files = partialFiles.toMutableSet().mapNotNull { partialFile ->
                    fetchFileData(partialFile).also { fetched ->
                        if (fetched == null) {
                            DisplayUtils.showSnackMessage(requireActivity(), R.string.error_retrieving_file)
                        }
                    }
                }
                isLoading = false
                callback(files.toHashSet())
            } else {
                isLoading = false
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
        // if in multi select keep mock file
        if (adapter.isMultiSelect()) {
            super.onItemClicked(file)
        } else {
            fetchFileAndRun(file) { fetched ->
                super.onItemClicked(fetched)
            }
        }
    }

    override fun onFileActionChosen(itemId: Int, checkedFiles: MutableSet<OCFile>?): Boolean {
        // fetch all files and run selected action
        if (itemId != R.id.action_select_all_action_menu && itemId != R.id.action_deselect_all_action_menu) {
            fetchAllAndRun(checkedFiles) { files ->
                exitSelectionMode()
                super.onFileActionChosen(itemId, files)
            }
            return true
        } else {
            return super.onFileActionChosen(itemId, checkedFiles)
        }
    }

    override fun onRefresh() {
        exitSelectionMode()
        super.onRefresh()
    }

    companion object {
        private val SHARED_TAG = SharedListFragment::class.java.simpleName
    }
}
