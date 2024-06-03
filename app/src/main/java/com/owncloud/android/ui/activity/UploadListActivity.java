/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.utils.Throttler;
import com.nextcloud.model.WorkerState;
import com.nextcloud.model.WorkerStateLiveData;
import com.owncloud.android.R;
import com.owncloud.android.databinding.UploadListLayoutBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CheckCurrentCredentialsOperation;
import com.owncloud.android.ui.adapter.UploadListAdapter;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Activity listing pending, active, and completed uploads. User can delete completed uploads from view. Content of this
 * list of coming from {@link UploadsStorageManager}.
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
    SyncedFolderProvider syncedFolderProvider;

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
        observeWorkerState();
    }

    private void observeWorkerState() {
        WorkerStateLiveData.Companion.instance().observe(this, state -> {
            if (state instanceof WorkerState.Upload) {
                Log_OC.d(TAG, "Upload worker started");
                handleUploadWorkerState();
            }
        });
    }

    private void handleUploadWorkerState() {
        uploadListAdapter.loadUploadItemsFromDb();
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
        uploadListAdapter.loadUploadItemsFromDb();
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
        FilesSyncHelper.startFilesSyncForAllFolders(syncedFolderProvider,
                                                    backgroundJobManager,
                                                    true,
                                                    new String[]{});

        if (uploadsStorageManager.getFailedUploads().length > 0) {
            new Thread(() -> {
                FileUploadHelper.Companion.instance().retryFailedUploads(
                    uploadsStorageManager,
                    connectivityService,
                    accountManager,
                    powerManagementService);
                this.runOnUiThread(() -> {
                    uploadListAdapter.loadUploadItemsFromDb();
                });
            }).start();
            DisplayUtils.showSnackMessage(this, R.string.uploader_local_files_uploaded);
        }


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
        uploadIntentFilter.addAction(FileUploadWorker.Companion.getUploadsAddedMessage());
        uploadIntentFilter.addAction(FileUploadWorker.Companion.getUploadStartMessage());
        uploadIntentFilter.addAction(FileUploadWorker.Companion.getUploadFinishMessage());
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
        updateGlobalPauseIcon(menu.getItem(0));
        return true;
    }

    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    private void updateGlobalPauseIcon(MenuItem pauseMenuItem) {
        if (pauseMenuItem.getItemId() != R.id.action_toggle_global_pause) {
            return;
        }

        int iconId;
        String title;
        if (preferences.isGlobalUploadPaused()) {
            iconId = R.drawable.ic_global_resume;
            title = getString(R.string.upload_action_global_upload_resume);
        } else {
            iconId = R.drawable.ic_global_pause;
            title = getString(R.string.upload_action_global_upload_pause);
        }

        pauseMenuItem.setIcon(iconId);
        pauseMenuItem.setTitle(title);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void toggleGlobalPause(MenuItem pauseMenuItem) {
        preferences.setGlobalUploadPaused(!preferences.isGlobalUploadPaused());
        updateGlobalPauseIcon(pauseMenuItem);

        for (User user : accountManager.getAllUsers()) {
            if (user != null) {
                FileUploadHelper.Companion.instance().cancelAndRestartUploadJob(user);
            }
        }

        uploadListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            if (isDrawerOpen()) {
                closeDrawer();
            } else {
                openDrawer();
            }
        } else if (itemId == R.id.action_toggle_global_pause) {
            toggleGlobalPause(item);
        } else {
            return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileActivity.REQUEST_CODE__UPDATE_CREDENTIALS && resultCode == RESULT_OK) {
            FilesSyncHelper.restartUploadsIfNeeded(uploadsStorageManager,
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
                FilesSyncHelper.restartUploadsIfNeeded(uploadsStorageManager,
                                                       userAccountManager,
                                                       connectivityService,
                                                       powerManagementService);
            }

        } else {
            super.onRemoteOperationFinish(operation, result);
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
