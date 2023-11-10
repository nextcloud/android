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

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.utils.Throttler;
import com.owncloud.android.R;
import com.owncloud.android.databinding.UploadListLayoutBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CheckCurrentCredentialsOperation;
import com.owncloud.android.ui.adapter.UploadListAdapter;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

/**
 * Activity listing pending, active, and completed uploads. User can delete
 * completed uploads from view. Content of this list of coming from
 * {@link UploadsStorageManager}.
 */
public class UploadListActivity extends FileActivity {

    private static final String TAG = UploadListActivity.class.getSimpleName();

    private UploadMessagesReceiver uploadMessagesReceiver;

    private UploadListAdapter uploadListAdapter;

    public SwipeRefreshLayout swipeListRefreshLayout;

    @Inject
    UserAccountManager userAccountManager;

    @Inject
    UploadsStorageManager uploadsStorageManager;

    @Inject
    ConnectivityService connectivityService;

    @Inject
    PowerManagementService powerManagementService;

    @Inject
    Clock clock;

    @Inject
    BackgroundJobManager backgroundJobManager;

    @Inject
    LocalBroadcastManager localBroadcastManager;

    @Inject
    ViewThemeUtils viewThemeUtils;

    @Inject Throttler throttler;

    private UploadListLayoutBinding binding;

    public static Intent createIntent(OCFile file, User user, Integer flag, Context context) {
        Intent intent = new Intent(context, UploadListActivity.class);
        if (flag != null) {
            intent.setFlags(intent.getFlags() | flag);
        }
        intent.putExtra(ConflictsResolveActivity.EXTRA_FILE, file);
        intent.putExtra(ConflictsResolveActivity.EXTRA_USER, user);

        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        throttler.setIntervalMillis(1000);

        binding = UploadListLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        swipeListRefreshLayout = binding.swipeContainingList;

        // this activity has no file really bound, it's for multiple accounts at the same time; should no inherit
        // from FileActivity; moreover, some behaviours inherited from FileActivity should be delegated to Fragments;
        // but that's other story
        setFile(null);

        // setup toolbar
        setupToolbar();

        updateActionBarTitleAndHomeButtonByString(getString(R.string.uploads_view_title));

        // setup drawer
        setupDrawer(R.id.nav_uploads);

        setupContent();
    }

    private void setupContent() {
        binding.list.setEmptyView(binding.emptyList.getRoot());
        binding.emptyList.getRoot().setVisibility(View.GONE);
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.uploads);
        binding.emptyList.emptyListIcon.getDrawable().mutate();
        binding.emptyList.emptyListIcon.setAlpha(0.5f);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewHeadline.setText(getString(R.string.upload_list_empty_headline));
        binding.emptyList.emptyListViewText.setText(getString(R.string.upload_list_empty_text_auto_upload));
        binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);

        uploadListAdapter = new UploadListAdapter(this,
                                                  uploadsStorageManager,
                                                  getStorageManager(),
                                                  userAccountManager,
                                                  connectivityService,
                                                  powerManagementService,
                                                  clock,
                                                  viewThemeUtils);

        final GridLayoutManager lm = new GridLayoutManager(this, 1);
        uploadListAdapter.setLayoutManager(lm);

        int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing);
        binding.list.addItemDecoration(new MediaGridItemDecoration(spacing));
        binding.list.setLayoutManager(lm);
        binding.list.setAdapter(uploadListAdapter);

        viewThemeUtils.androidx.themeSwipeRefreshLayout(swipeListRefreshLayout);
        swipeListRefreshLayout.setOnRefreshListener(this::refresh);

        loadItems();
    }

    private void loadItems() {
        uploadListAdapter.loadUploadItemsFromDb();

        if (uploadListAdapter.getItemCount() > 0) {
            return;
        }

        swipeListRefreshLayout.setVisibility(View.VISIBLE);
        swipeListRefreshLayout.setRefreshing(false);
    }

    private void refresh() {
        backgroundJobManager.startImmediateFilesSyncJob(false, true);

        // retry failed uploads
        new Thread(() -> FileUploader.retryFailedUploads(
            this,
            uploadsStorageManager,
            connectivityService,
            userAccountManager,
            powerManagementService
                                                        )).start();

        // update UI
        uploadListAdapter.loadUploadItemsFromDb();
        swipeListRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        Log_OC.v(TAG, "onResume() start");
        super.onResume();

        setDrawerMenuItemChecked(R.id.nav_uploads);

        // Listen for upload messages
        uploadMessagesReceiver = new UploadMessagesReceiver();
        IntentFilter uploadIntentFilter = new IntentFilter();
        uploadIntentFilter.addAction(FileUploader.getUploadsAddedMessage());
        uploadIntentFilter.addAction(FileUploader.getUploadStartMessage());
        uploadIntentFilter.addAction(FileUploader.getUploadFinishMessage());
        localBroadcastManager.registerReceiver(uploadMessagesReceiver, uploadIntentFilter);

        Log_OC.v(TAG, "onResume() end");

    }

    @Override
    protected void onPause() {
        Log_OC.v(TAG, "onPause() start");
        if (uploadMessagesReceiver != null) {
            localBroadcastManager.unregisterReceiver(uploadMessagesReceiver);
            uploadMessagesReceiver = null;
        }
        super.onPause();
        Log_OC.v(TAG, "onPause() end");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_upload_list, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            if (isDrawerOpen()) {
                closeDrawer();
            } else {
                openDrawer();
            }
        } else if (itemId == R.id.action_clear_failed_uploads) {
            uploadsStorageManager.clearFailedButNotDelayedUploads();
            uploadListAdapter.loadUploadItemsFromDb();
        } else {
            retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileActivity.REQUEST_CODE__UPDATE_CREDENTIALS && resultCode == RESULT_OK) {
            FilesSyncHelper.restartJobsIfNeeded(uploadsStorageManager,
                                                userAccountManager,
                                                connectivityService,
                                                powerManagementService);
        }
    }

    /**
     * @param operation Operation performed.
     * @param result    Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof CheckCurrentCredentialsOperation) {
            // Do not call super in this case; more refactoring needed around onRemoteOperationFinish :'(
            getFileOperationsHelper().setOpIdWaitingFor(Long.MAX_VALUE);
            dismissLoadingDialog();
            Account account = (Account) result.getData().get(0);
            if (!result.isSuccess()) {
                requestCredentialsUpdate(this, account);

            } else {
                // already updated -> just retry!
                FilesSyncHelper.restartJobsIfNeeded(uploadsStorageManager,
                                                    userAccountManager,
                                                    connectivityService,
                                                    powerManagementService);
            }

        } else {
            super.onRemoteOperationFinish(operation, result);
        }
    }


    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new UploadListServiceConnection();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private class UploadListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (service instanceof FileUploaderBinder) {
                if (mUploaderBinder == null) {
                    mUploaderBinder = (FileUploaderBinder) service;
                    Log_OC.d(TAG, "UploadListActivity connected to Upload service. component: " +
                            component + " service: " + service);
                } else {
                    Log_OC.d(TAG, "mUploaderBinder already set. mUploaderBinder: " +
                            mUploaderBinder + " service:" + service);
                }
            } else {
                Log_OC.d(TAG, "UploadListActivity not connected to Upload service. component: " +
                        component + " service: " + service);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(UploadListActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "UploadListActivity suddenly disconnected from Upload service");
                mUploaderBinder = null;
            }
        }
    }

    /**
     * Once the file upload has changed its status -> update uploads list view
     */
    private class UploadMessagesReceiver extends BroadcastReceiver {
        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            throttler.run("update_upload_list", () -> {
                uploadListAdapter.loadUploadItemsFromDb();
            });
        }
    }
}
