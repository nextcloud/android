/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nextcloud.client.account.User
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.utils.Throttler
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadListLayoutBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CheckCurrentCredentialsOperation
import com.owncloud.android.ui.adapter.UploadListAdapter
import com.owncloud.android.ui.decoration.MediaGridItemDecoration
import com.owncloud.android.utils.FilesSyncHelper
import javax.inject.Inject

/**
 * Activity listing pending, active, and completed uploads. User can delete
 * completed uploads from view. Content of this list of coming from
 * [UploadsStorageManager].
 */
class UploadListActivity : FileActivity() {

    private var uploadMessagesReceiver: UploadMessagesReceiver? = null
    private var uploadListAdapter: UploadListAdapter? = null
    private var swipeListRefreshLayout: SwipeRefreshLayout? = null

    @Inject
    lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject
    lateinit var powerManagementService: PowerManagementService

    @Inject
    lateinit var clock: Clock

    @Inject
    lateinit var localBroadcastManager: LocalBroadcastManager

    @Inject
    lateinit var throttler: Throttler

    private lateinit var binding: UploadListLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        throttler.intervalMillis = 1000

        binding = UploadListLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        swipeListRefreshLayout = binding.swipeContainingList

        // this activity has no file really bound, it's for multiple accounts at the same time; should no inherit
        // from FileActivity; moreover, some behaviours inherited from FileActivity should be delegated to Fragments;
        // but that's other story
        file = null

        setupToolbar()
        updateActionBarTitleAndHomeButtonByString(getString(R.string.uploads_view_title))
        setupDrawer(R.id.nav_uploads)
        setupEmptyList()
        setupUploadListAdapter()
        setupSwipeListRefreshLayout()
        loadItems()
    }

    private fun setupEmptyList() {
        binding.list.setEmptyView(binding.emptyList.root)
        binding.emptyList.root.visibility = View.GONE
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.uploads)
        binding.emptyList.emptyListIcon.drawable.mutate()
        binding.emptyList.emptyListIcon.alpha = 0.5f
        binding.emptyList.emptyListIcon.visibility = View.VISIBLE
        binding.emptyList.emptyListViewHeadline.text = getString(R.string.upload_list_empty_headline)
        binding.emptyList.emptyListViewText.text =
            getString(R.string.upload_list_empty_text_auto_upload)
        binding.emptyList.emptyListViewText.visibility = View.VISIBLE
    }

    private fun setupUploadListAdapter() {
        uploadListAdapter = UploadListAdapter(
            this,
            uploadsStorageManager,
            storageManager,
            userAccountManager,
            connectivityService,
            powerManagementService,
            clock,
            viewThemeUtils
        )

        val lm = GridLayoutManager(this, 1)
        uploadListAdapter?.setLayoutManager(lm)
        val spacing = resources.getDimensionPixelSize(R.dimen.media_grid_spacing)
        binding.list.addItemDecoration(MediaGridItemDecoration(spacing))
        binding.list.layoutManager = lm
        binding.list.adapter = uploadListAdapter
    }

    private fun setupSwipeListRefreshLayout() {
        swipeListRefreshLayout?.let {
            viewThemeUtils.androidx.themeSwipeRefreshLayout(it)
        }
        swipeListRefreshLayout?.setOnRefreshListener { refresh() }
    }

    private fun loadItems() {
        uploadListAdapter?.loadUploadItemsFromDb()
        if ((uploadListAdapter?.itemCount ?: 0) > 0) {
            return
        }
        swipeListRefreshLayout?.visibility = View.VISIBLE
        swipeListRefreshLayout?.isRefreshing = false
    }

    private fun refresh() {
        backgroundJobManager.startImmediateFilesSyncJob(skipCustomFolders = false, overridePowerSaving = true)

        // retry failed uploads
        Thread {
            FileUploader.retryFailedUploads(
                this,
                uploadsStorageManager,
                connectivityService,
                userAccountManager,
                powerManagementService
            )
        }.start()

        // update UI
        uploadListAdapter?.loadUploadItemsFromDb()
        swipeListRefreshLayout?.isRefreshing = false
    }

    override fun onResume() {
        Log_OC.v(TAG, "onResume() start")

        super.onResume()
        setDrawerMenuItemChecked(R.id.nav_uploads)

        // Listen for upload messages
        uploadMessagesReceiver = UploadMessagesReceiver()
        val uploadIntentFilter = IntentFilter()
        uploadIntentFilter.addAction(FileUploader.getUploadsAddedMessage())
        uploadIntentFilter.addAction(FileUploader.getUploadStartMessage())
        uploadIntentFilter.addAction(FileUploader.getUploadFinishMessage())
        localBroadcastManager.registerReceiver(uploadMessagesReceiver!!, uploadIntentFilter)
        Log_OC.v(TAG, "onResume() end")
    }

    override fun onPause() {
        Log_OC.v(TAG, "onPause() start")
        if (uploadMessagesReceiver != null) {
            localBroadcastManager.unregisterReceiver(uploadMessagesReceiver!!)
            uploadMessagesReceiver = null
        }
        super.onPause()
        Log_OC.v(TAG, "onPause() end")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.activity_upload_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            if (isDrawerOpen) {
                closeDrawer()
            } else {
                openDrawer()
            }
        } else if (itemId == R.id.action_clear_failed_uploads) {
            uploadsStorageManager.clearFailedButNotDelayedUploads()
            uploadListAdapter?.loadUploadItemsFromDb()
        } else {
            retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

    @Suppress("DEPRECATION")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE__UPDATE_CREDENTIALS && resultCode == RESULT_OK) {
            FilesSyncHelper.restartJobsIfNeeded(
                uploadsStorageManager,
                userAccountManager,
                connectivityService,
                powerManagementService
            )
        }
    }

    /**
     * @param operation Operation performed.
     * @param result    Result of the removal.
     */
    @Suppress("DEPRECATION")
    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        if (operation is CheckCurrentCredentialsOperation) {
            // Do not call super in this case; more refactoring needed around onRemoteOperationFinish :'(
            fileOperationsHelper.opIdWaitingFor = Long.MAX_VALUE
            dismissLoadingDialog()
            val account = result.data[0] as Account
            if (!result.isSuccess) {
                requestCredentialsUpdate(this, account)
            } else {
                // already updated -> just retry!
                FilesSyncHelper.restartJobsIfNeeded(
                    uploadsStorageManager,
                    userAccountManager,
                    connectivityService,
                    powerManagementService
                )
            }
        } else {
            super.onRemoteOperationFinish(operation, result)
        }
    }

    override fun newTransferenceServiceConnection(): ServiceConnection {
        return UploadListServiceConnection()
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private inner class UploadListServiceConnection : ServiceConnection {
        override fun onServiceConnected(component: ComponentName, service: IBinder) {
            if (service is FileUploaderBinder) {
                if (mUploaderBinder == null) {
                    mUploaderBinder = service
                    Log_OC.d(
                        TAG,
                        "UploadListActivity connected to Upload service. component: " +
                            component + " service: " + service
                    )
                } else {
                    Log_OC.d(
                        TAG,
                        "mUploaderBinder already set. mUploaderBinder: " +
                            mUploaderBinder + " service:" + service
                    )
                }
            } else {
                Log_OC.d(
                    TAG,
                    "UploadListActivity not connected to Upload service. component: " +
                        component + " service: " + service
                )
            }
        }

        override fun onServiceDisconnected(component: ComponentName) {
            if (component == ComponentName(this@UploadListActivity, FileUploader::class.java)) {
                Log_OC.d(TAG, "UploadListActivity suddenly disconnected from Upload service")
                mUploaderBinder = null
            }
        }
    }

    /**
     * Once the file upload has changed its status -> update uploads list view
     */
    private inner class UploadMessagesReceiver : BroadcastReceiver() {
        /**
         * [BroadcastReceiver] to enable syncing feedback in UI
         */
        override fun onReceive(context: Context, intent: Intent) {
            throttler.run("update_upload_list") { uploadListAdapter?.loadUploadItemsFromDb() }
        }
    }

    companion object {
        private val TAG = UploadListActivity::class.java.simpleName

        @JvmStatic
        fun createIntent(file: OCFile?, user: User?, flag: Int?, context: Context?): Intent {
            val intent = Intent(context, UploadListActivity::class.java)
            if (flag != null) {
                intent.flags = intent.flags or flag
            }
            intent.putExtra(EXTRA_FILE, file)
            intent.putExtra(EXTRA_USER, user)
            return intent
        }
    }
}
