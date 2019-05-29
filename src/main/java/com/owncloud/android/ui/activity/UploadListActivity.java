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
import android.widget.ImageView;
import android.widget.TextView;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.jobs.FilesSyncJob;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CheckCurrentCredentialsOperation;
import com.owncloud.android.ui.EmptyRecyclerView;
import com.owncloud.android.ui.adapter.UploadListAdapter;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.ThemeUtils;

import java.util.Set;

import javax.inject.Inject;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

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

    @BindView(R.id.empty_list_view_text)
    public TextView emptyContentMessage;

    @BindView(R.id.empty_list_view_headline)
    public TextView emptyContentHeadline;

    @BindView(R.id.empty_list_icon)
    public ImageView emptyContentIcon;

    @BindView(android.R.id.list)
    public EmptyRecyclerView recyclerView;

    @BindString(R.string.upload_list_empty_headline)
    public String noResultsHeadline;

    @BindString(R.string.upload_list_empty_text_auto_upload)
    public String noResultsMessage;

    private Unbinder unbinder;

    @Inject
    UserAccountManager userAccountManager;

    @Inject
    UploadsStorageManager uploadsStorageManager;

    @Inject
    ConnectivityService connectivityService;

    @Inject
    PowerManagementService powerManagementService;

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent i = new Intent(getApplicationContext(), FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.upload_list_layout);
        unbinder = ButterKnife.bind(this);

        swipeListRefreshLayout = findViewById(R.id.swipe_containing_list);

        // this activity has no file really bound, it's for multiple accounts at the same time; should no inherit
        // from FileActivity; moreover, some behaviours inherited from FileActivity should be delegated to Fragments;
        // but that's other story
        setFile(null);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_uploads);

        setupContent();

        if (getSupportActionBar() != null) {
            ThemeUtils.setColoredTitle(getSupportActionBar(), R.string.uploads_view_title, this);
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);

        if (getResources().getBoolean(R.bool.bottom_toolbar_enabled)) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            DisplayUtils.setupBottomBar(
                getUserAccountManager().getCurrentAccount(),
                bottomNavigationView,
                getResources(),
                accountManager,
                this,
                -1
            );
        }
    }

    private void setupContent() {
        recyclerView = findViewById(android.R.id.list);
        recyclerView.setEmptyView(findViewById(R.id.empty_list_view));
        findViewById(R.id.empty_list_progress).setVisibility(View.GONE);
        emptyContentIcon.setImageResource(R.drawable.uploads);
        emptyContentIcon.getDrawable().mutate();
        emptyContentIcon.setAlpha(0.5f);
        emptyContentIcon.setVisibility(View.VISIBLE);
        emptyContentHeadline.setText(noResultsHeadline);
        emptyContentMessage.setText(noResultsMessage);

        uploadListAdapter = new UploadListAdapter(this,
                                                  uploadsStorageManager,
                                                  userAccountManager,
                                                  connectivityService,
                                                  powerManagementService);

        final GridLayoutManager lm = new GridLayoutManager(this, 1);
        uploadListAdapter.setLayoutManager(lm);

        int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing);
        recyclerView.addItemDecoration(new MediaGridItemDecoration(spacing));
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(uploadListAdapter);


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
        // scan for missing auto uploads files
        Set<Job> jobs = JobManager.instance().getAllJobsForTag(FilesSyncJob.TAG);

        if (jobs.isEmpty()) {
            PersistableBundleCompat persistableBundleCompat = new PersistableBundleCompat();
            persistableBundleCompat.putBoolean(FilesSyncJob.OVERRIDE_POWER_SAVING, true);
            new JobRequest.Builder(FilesSyncJob.TAG)
                .setExact(1_000L)
                .setUpdateCurrent(false)
                .setExtras(persistableBundleCompat)
                .build()
                .schedule();
        }

        // retry failed uploads
        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        new Thread(() -> requester.retryFailedUploads(this,
                                                      null,
                                                      uploadsStorageManager,
                                                      connectivityService,
                                                      userAccountManager,
                                                      powerManagementService,
                                                      null)).start();

        // update UI
        uploadListAdapter.loadUploadItemsFromDb();
        swipeListRefreshLayout.setRefreshing(false);
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
        registerReceiver(uploadMessagesReceiver, uploadIntentFilter);

        Log_OC.v(TAG, "onResume() end");

    }

    @Override
    protected void onPause() {
        Log_OC.v(TAG, "onPause() start");
        if (uploadMessagesReceiver != null) {
            unregisterReceiver(uploadMessagesReceiver);
            uploadMessagesReceiver = null;
        }
        super.onPause();
        Log_OC.v(TAG, "onPause() end");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.upload_list_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                break;
            case R.id.action_clear_failed_uploads:
                uploadsStorageManager.clearFailedButNotDelayedUploads();
                uploadListAdapter.loadUploadItemsFromDb();
                break;

            default:
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
            try {
                uploadListAdapter.loadUploadItemsFromDb();
            } finally {
                if (intent != null) {
                    removeStickyBroadcast(intent);
                }
            }

        }
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);

        ThemeUtils.setColoredTitle(getSupportActionBar(), R.string.uploads_view_title, this);

        if (mAccountWasSet) {
            setAccountInDrawer(getAccount());
        }
    }

    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }
}
