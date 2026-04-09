/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.User
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadEventBroadcaster
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.utils.Throttler
import com.owncloud.android.R
import com.owncloud.android.databinding.UploadListLayoutBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.CheckCurrentCredentialsOperation
import com.owncloud.android.ui.adapter.uploadList.UploadListAdapter
import com.owncloud.android.ui.adapter.uploadList.helper.ConflictHandlingResult
import com.owncloud.android.ui.adapter.uploadList.helper.UploadListAdapterAction
import com.owncloud.android.ui.adapter.uploadList.helper.UploadListAdapterActionHandler
import com.owncloud.android.ui.adapter.uploadList.helper.UploadListAdapterHelper
import com.owncloud.android.ui.adapter.uploadList.helper.UploadListItemOnClick
import com.owncloud.android.ui.decoration.MediaGridItemDecoration
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.FilesSyncHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("MagicNumber")
class UploadListActivity :
    FileActivity(),
    UploadListItemOnClick {
    @Inject lateinit var uploadsStorageManager: UploadsStorageManager

    @Inject lateinit var powerManagementService: PowerManagementService

    @Inject lateinit var clock: Clock

    @Inject lateinit var syncedFolderProvider: SyncedFolderProvider

    @Inject lateinit var localBroadcastManager: LocalBroadcastManager

    @Inject lateinit var throttler: Throttler

    private var swipeListRefreshLayout: SwipeRefreshLayout? = null
    private var binding: UploadListLayoutBinding? = null

    private var uploadFinishReceiver: UploadFinishReceiver? = null
    private lateinit var uploadListAdapter: UploadListAdapter
    private lateinit var adapterActionHandler: UploadListAdapterAction
    private lateinit var adapterHelper: UploadListAdapterHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        throttler.intervalMillis = 1000
        binding = UploadListLayoutBinding.inflate(layoutInflater)
        val binding = binding!!
        setContentView(binding.getRoot())
        swipeListRefreshLayout = binding.swipeContainingList

        // this activity has no file really bound, it's for multiple accounts at the same time; should no inherit
        // from FileActivity; moreover, some behaviours inherited from FileActivity should be delegated to Fragments;
        // but that's other story
        file = null

        setupToolbar()
        updateActionBarTitleAndHomeButtonByString(getString(R.string.uploads_view_title))
        setupDrawer(menuItemId)
        setupContent()
    }

    override fun getMenuItemId() = R.id.nav_uploads

    private fun setupContent() {
        setupEmptyList()
        adapterActionHandler = UploadListAdapterActionHandler()
        adapterHelper = UploadListAdapterHelper(this)
        uploadListAdapter = UploadListAdapter(
            this,
            uploadsStorageManager,
            userAccountManager,
            connectivityService,
            powerManagementService,
            viewThemeUtils,
            this,
            adapterHelper
        )

        val lm = GridLayoutManager(this, 1)
        uploadListAdapter.setLayoutManager(lm)

        val spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing)
        binding?.list?.run {
            addItemDecoration(MediaGridItemDecoration(spacing))
            setLayoutManager(lm)
            setAdapter(uploadListAdapter)
        }

        swipeListRefreshLayout?.let { viewThemeUtils.androidx.themeSwipeRefreshLayout(it) }
        swipeListRefreshLayout?.setOnRefreshListener { this.refresh() }
        loadItems()
    }

    private fun setupEmptyList() {
        binding?.run {
            list.setEmptyView(emptyList.getRoot())
            emptyList.run {
                root.visibility = View.GONE

                emptyListIcon.run {
                    setImageResource(R.drawable.uploads)
                    getDrawable().mutate()
                    setAlpha(0.5f)
                    setVisibility(View.VISIBLE)
                }

                emptyListViewHeadline.text = getString(R.string.upload_list_empty_headline)

                emptyListViewText.run {
                    text = getString(R.string.upload_list_empty_text_auto_upload)
                    visibility = View.VISIBLE
                }
            }
        }
    }

    private fun loadItems() {
        swipeListRefreshLayout?.isRefreshing = true
        uploadListAdapter.loadUploadItemsFromDb { swipeListRefreshLayout?.isRefreshing = false }
    }

    private fun refresh() {
        val isUploadStarted = FileUploadHelper.instance().retryFailedUploads(
            uploadsStorageManager,
            connectivityService,
            accountManager,
            powerManagementService
        )

        if (!isUploadStarted) {
            uploadListAdapter.loadUploadItemsFromDb { swipeListRefreshLayout?.isRefreshing = false }
        }
    }

    override fun onStart() {
        Log_OC.v(TAG, "onStart() start")
        super.onStart()

        highlightNavigationViewItem(menuItemId)

        uploadFinishReceiver = UploadFinishReceiver()
        val intentFilter = IntentFilter().apply {
            addAction(FileUploadEventBroadcaster.ACTION_UPLOAD_ENQUEUED)
            addAction(FileUploadEventBroadcaster.ACTION_UPLOAD_STARTED)
            addAction(FileUploadEventBroadcaster.ACTION_UPLOAD_COMPLETED)
        }
        uploadFinishReceiver?.let { localBroadcastManager.registerReceiver(it, intentFilter) }

        Log_OC.v(TAG, "onStart() end")
    }

    override fun onStop() {
        Log_OC.v(TAG, "onStop() start")
        if (uploadFinishReceiver != null) {
            uploadFinishReceiver?.let { localBroadcastManager.unregisterReceiver(it) }
            uploadFinishReceiver = null
        }
        super.onStop()
        Log_OC.v(TAG, "onStop() end")
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_upload_list, menu)
        menu.findItem(R.id.action_toggle_global_pause)?.let { updateGlobalPauseIcon(it) }
        return true
    }

    private fun updateGlobalPauseIcon(item: MenuItem) {
        val paused = preferences.isGlobalUploadPaused()
        item.setIcon(if (paused) R.drawable.ic_global_resume else R.drawable.ic_global_pause)
        item.title = getString(
            if (paused) {
                R.string.upload_action_global_upload_resume
            } else {
                R.string.upload_action_global_upload_pause
            }
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun toggleGlobalPause(item: MenuItem) {
        preferences.setGlobalUploadPaused(!preferences.isGlobalUploadPaused())
        updateGlobalPauseIcon(item)
        val uploadHelper = FileUploadHelper.instance()
        accountManager.getAllUsers().filterNotNull().forEach { user ->
            val ids = uploadsStorageManager.getCurrentUploadIds(user.accountName)
            uploadHelper.cancelAndRestartUploadJob(user, ids)
        }
        uploadListAdapter.notifyDataSetChanged()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            if (isDrawerOpen) closeDrawer() else openDrawer()
            true
        }

        R.id.action_toggle_global_pause -> {
            toggleGlobalPause(item)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE__UPDATE_CREDENTIALS && resultCode == RESULT_OK) {
            FilesSyncHelper.restartUploadsIfNeeded(
                uploadsStorageManager,
                userAccountManager,
                connectivityService,
                powerManagementService
            )
        }
    }

    override fun onRemoteOperationFinish(operation: RemoteOperation<*>?, result: RemoteOperationResult<*>) {
        if (operation !is CheckCurrentCredentialsOperation) {
            super.onRemoteOperationFinish(operation, result)
            return
        }

        fileOperationsHelper.opIdWaitingFor = Long.MAX_VALUE
        dismissLoadingDialog()
        val account = result.data[0] as? Account
        if (!result.isSuccess) {
            requestCredentialsUpdate(account)
        } else {
            FilesSyncHelper.restartUploadsIfNeeded(
                uploadsStorageManager,
                userAccountManager,
                connectivityService,
                powerManagementService
            )
        }
    }

    override fun onLastUploadResultConflictClick(upload: OCUpload) {
        DisplayUtils.showSnackMessage(this, R.string.upload_sync_conflict_checking)

        lifecycleScope.launch {
            val client = clientRepository.getOwncloudClient() ?: return@launch
            val result = adapterActionHandler.handleConflict(upload, client, uploadsStorageManager)

            withContext(Dispatchers.Main) {
                when (result) {
                    is ConflictHandlingResult.ConflictNotExists -> {
                        uploadListAdapter.notifyUploadChanged(upload)
                        onConflictNotExists(upload)
                    }

                    is ConflictHandlingResult.CannotCheckConflict -> {
                        DisplayUtils.showSnackMessage(
                            this@UploadListActivity,
                            R.string.upload_sync_conflict_check_error
                        )
                    }

                    is ConflictHandlingResult.ShowConflictResolveDialog -> {
                        adapterHelper.openConflictActivity(result.file, result.upload)
                    }
                }
            }
        }
    }

    private fun onConflictNotExists(upload: OCUpload) {
        val rootView = binding?.root ?: return
        val snackbar = Snackbar.make(
            rootView,
            R.string.upload_sync_conflict_not_exists,
            Snackbar.LENGTH_LONG
        )

        snackbar.setAction(R.string.retry) {
            val optionalUser = userAccountManager.getUser(upload.accountName)
            if (optionalUser.isPresent) {
                FileUploadHelper.instance().retryUpload(upload, optionalUser.get())
            }
        }

        snackbar.show()
    }

    private inner class UploadFinishReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            throttler.run("update_upload_list") { uploadListAdapter.loadUploadItemsFromDb() }
        }
    }

    companion object {
        private val TAG: String = UploadListActivity::class.java.getSimpleName()

        fun createIntent(file: OCFile?, user: User?, flag: Int?, context: Context?): Intent =
            Intent(context, UploadListActivity::class.java).apply {
                if (flag != null) {
                    setFlags(flags or flag)
                }
                putExtra(EXTRA_FILE, file)
                putExtra(EXTRA_USER, user)
            }
    }
}
