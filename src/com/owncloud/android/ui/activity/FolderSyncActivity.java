/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderItem;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.ui.adapter.FolderSyncAdapter;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

/**
 * Activity displaying all auto-synced folders and/or instant upload media folders.
 */
public class FolderSyncActivity extends FileActivity implements FolderSyncAdapter.ClickListener, SyncedFolderPreferencesDialogFragment.OnSyncedFolderPreferenceListener {
    private static final String TAG = FolderSyncActivity.class.getSimpleName();

    private static final String SYNCED_FOLDER_PREFERENCES_DIALOG_TAG = "SYNCED_FOLDER_PREFERENCES_DIALOG";

    private RecyclerView mRecyclerView;
    private FolderSyncAdapter mAdapter;
    private LinearLayout mProgress;
    private TextView mEmpty;
    private SyncedFolderProvider mSyncedFolderProvider;
    private List<SyncedFolderItem> syncFolderItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.folder_sync_layout);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_folder_sync);
        getSupportActionBar().setTitle(getString(R.string.drawer_folder_sync));

        setupContent();
    }

    private void setupContent() {
        mRecyclerView = (RecyclerView) findViewById(android.R.id.list);

        mProgress = (LinearLayout) findViewById(android.R.id.progress);
        mEmpty = (TextView) findViewById(android.R.id.empty);

        // TODO implement "dynamic" grid count via xml-value for tablet vs. phone
        final int gridWidth = 4;
        mAdapter = new FolderSyncAdapter(this, gridWidth, this, mRecyclerView);
        mSyncedFolderProvider = new SyncedFolderProvider(getContentResolver());

        final GridLayoutManager lm = new GridLayoutManager(this, gridWidth);
        mAdapter.setLayoutManager(lm);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);

        load();
    }

    private void load() {
        if (mAdapter.getItemCount() > 0) return;
        setListShown(false);
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<MediaFolder> mediaFolders = MediaProvider.getAllShownImagesPath(getContentResolver());
                syncFolderItems = mergeFolderData(mSyncedFolderProvider.getSyncedFolders(),
                        mediaFolders);

                // TODO remove before mergeing to master, keeping it for debugging atm
                /**
                for (MediaFolder mediaFolder : mediaFolders) {
                    Log.d(TAG, mediaFolder.absolutePath);
                }
                 */

                mHandler.post(new TimerTask() {
                    @Override
                    public void run() {
                        mAdapter.setSyncFolderItems(syncFolderItems);
                        setListShown(true);
                    }
                });
            }
        }).start();
    }

    @NonNull
    private List<SyncedFolderItem> mergeFolderData(List<SyncedFolder> syncedFolders,
                                                   @NonNull List<MediaFolder> mediaFolders) {
        Map<String, SyncedFolder> syncedFoldersMap = createSyncedFoldersMap(syncedFolders);
        List<SyncedFolderItem> result = new ArrayList<>();

        for (MediaFolder mediaFolder : mediaFolders) {
            if (syncedFoldersMap.containsKey(mediaFolder.absolutePath)) {
                SyncedFolder syncedFolder = syncedFoldersMap.get(mediaFolder.absolutePath);
                result.add(createSyncedFolder(syncedFolder,mediaFolder));
            } else {
                result.add(createSyncedFolderFromMediaFolder(mediaFolder));
            }
        }

        return result;
    }

    @NonNull
    private SyncedFolderItem createSyncedFolder(@NonNull SyncedFolder syncedFolder, @NonNull MediaFolder mediaFolder) {
        return new SyncedFolderItem(
                syncedFolder.getId(),
                syncedFolder.getLocalPath(),
                syncedFolder.getRemotePath(),
                syncedFolder.getWifiOnly(),
                syncedFolder.getChargingOnly(),
                syncedFolder.getSubfolderByDate(),
                syncedFolder.getAccount(),
                syncedFolder.getUploadAction(),
                syncedFolder.isEnabled(),
                mediaFolder.filePaths,
                mediaFolder.folderName,
                mediaFolder.numberOfFiles);
    }

    @NonNull
    private SyncedFolderItem createSyncedFolderFromMediaFolder(@NonNull MediaFolder mediaFolder) {
        return new SyncedFolderItem(
                SyncedFolderItem.UNPERSISTED_ID,
                mediaFolder.absolutePath,
                getString(R.string.instant_upload_path) + "/" + mediaFolder.folderName,
                true,
                false,
                false,
                AccountUtils.getCurrentOwnCloudAccount(this).name,
                1,
                false,
                mediaFolder.filePaths,
                mediaFolder.folderName,
                mediaFolder.numberOfFiles);
    }

    @NonNull
    private Map<String,SyncedFolder> createSyncedFoldersMap(List<SyncedFolder> syncFolders) {
        Map<String, SyncedFolder> result = new HashMap<>();
        if (syncFolders != null) {
            for (SyncedFolder syncFolder : syncFolders) {
                result.put(syncFolder.getLocalPath(), syncFolder);
            }
        }
        return result;
    }

    private void setListShown(boolean shown) {
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(shown ? View.VISIBLE : View.GONE);
            mProgress.setVisibility(shown ? View.GONE : View.VISIBLE);
            mEmpty.setVisibility(shown && mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
            }

            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    public void restart() {
        Intent i = new Intent(this, FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        MainApp.showOnlyFilesOnDevice(onDeviceOnly);
        Intent fileDisplayActivity = new Intent(getApplicationContext(), FileDisplayActivity.class);
        fileDisplayActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(fileDisplayActivity);
    }

    @Override
    public void onSyncStatusToggleClick(int section, SyncedFolderItem syncedFolderItem) {
        if(syncedFolderItem.getId() > SyncedFolderItem.UNPERSISTED_ID) {
            mSyncedFolderProvider.updateFolderSyncEnabled(syncedFolderItem.getId(),!syncedFolderItem.isEnabled());
        } else {
            mSyncedFolderProvider.storeFolderSync(syncedFolderItem);
        }
    }

    @Override
    public void onSyncFolderSettingsClick(int section, SyncedFolderItem syncedFolderItem) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);

        SyncedFolderPreferencesDialogFragment.newInstance(syncedFolderItem)
                .show(ft, SYNCED_FOLDER_PREFERENCES_DIALOG_TAG);
    }

    @Override
    public void onSaveSyncedFolderPreference() {
        Toast.makeText(this, "onSaveSyncedFolderPreference clicked", Toast.LENGTH_SHORT).show();
    }
}
