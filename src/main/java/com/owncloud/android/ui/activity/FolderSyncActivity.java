/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.adapter.FolderSyncAdapter;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable;
import com.owncloud.android.ui.events.CustomFolderEvent;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.ThemeUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import static com.owncloud.android.datamodel.SyncedFolderDisplayItem.UNPERSISTED_ID;

/**
 * Activity displaying all auto-synced folders and/or instant upload media folders.
 */
public class FolderSyncActivity extends FileActivity implements FolderSyncAdapter.ClickListener,
        SyncedFolderPreferencesDialogFragment.OnSyncedFolderPreferenceListener {

    private static final String SYNCED_FOLDER_PREFERENCES_DIALOG_TAG = "SYNCED_FOLDER_PREFERENCES_DIALOG";
    public static final String PRIORITIZED_FOLDER = "Camera";
    public static final String EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR";

    private static final String SCREEN_NAME = "Auto upload";

    private static final String TAG = FolderSyncActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private FolderSyncAdapter mAdapter;
    private LinearLayout mProgress;
    private TextView mEmpty;
    private SyncedFolderProvider mSyncedFolderProvider;
    private List<SyncedFolderDisplayItem> syncFolderItems;
    private SyncedFolderPreferencesDialogFragment mSyncedFolderPreferencesDialogFragment;
    private boolean showSidebar = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() != null) {
            showSidebar = getIntent().getExtras().getBoolean(EXTRA_SHOW_SIDEBAR);
        }

        setContentView(R.layout.folder_sync_layout);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_folder_sync);

        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            mDrawerToggle.setDrawerIndicatorEnabled(false);
        }

        setupContent();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ThemeUtils.setColoredTitle(getSupportActionBar(), getString(R.string.drawer_folder_sync));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (ThemeUtils.themingEnabled()) {
            setTheme(R.style.FallbackThemingTheme);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnalyticsUtils.setCurrentScreenName(this, SCREEN_NAME, TAG);
    }

    /**
     * sets up the UI elements and loads all media/synced folders.
     */
    private void setupContent() {
        mRecyclerView = (RecyclerView) findViewById(android.R.id.list);

        mProgress = (LinearLayout) findViewById(android.R.id.progress);
        mEmpty = (TextView) findViewById(android.R.id.empty);

        final int gridWidth = getResources().getInteger(R.integer.media_grid_width);
        boolean lightVersion = getResources().getBoolean(R.bool.syncedFolder_light);
        mAdapter = new FolderSyncAdapter(this, gridWidth, this, lightVersion);
        mSyncedFolderProvider = new SyncedFolderProvider(getContentResolver());

        final GridLayoutManager lm = new GridLayoutManager(this, gridWidth);
        mAdapter.setLayoutManager(lm);
        int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing);
        mRecyclerView.addItemDecoration(new MediaGridItemDecoration(spacing));
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);

        if (getResources().getBoolean(R.bool.bottom_toolbar_enabled)) {
            bottomNavigationView.setVisibility(View.VISIBLE);
            DisplayUtils.setupBottomBar(bottomNavigationView, getResources(), this, -1);
        }

        load(gridWidth * 2, false);
    }

    /**
     * loads all media/synced folders, adds them to the recycler view adapter and shows the list.
     *
     * @param perFolderMediaItemLimit the amount of media items to be loaded/shown per media folder
     */
    private void load(final int perFolderMediaItemLimit, boolean force) {
        if (mAdapter.getItemCount() > 0 && !force) {
            return;
        }
        setListShown(false);
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<MediaFolder> mediaFolders = MediaProvider.getImageFolders(getContentResolver(),
                        perFolderMediaItemLimit, FolderSyncActivity.this);
                Log_OC.w(TAG, "Picture Folders: " + mediaFolders.size());
                mediaFolders.addAll(MediaProvider.getVideoFolders(getContentResolver(), perFolderMediaItemLimit));
                Log_OC.w(TAG, "Picture+Video Folders: " + mediaFolders.size());

                //TODO properly merge image and video lists to remove duplicates

                List<SyncedFolder> syncedFolderArrayList = mSyncedFolderProvider.getSyncedFolders();
                List<SyncedFolder> currentAccountSyncedFoldersList = new ArrayList<>();
                Account currentAccount = AccountUtils.getCurrentOwnCloudAccount(FolderSyncActivity.this);
                for (SyncedFolder syncedFolder : syncedFolderArrayList) {
                    if (syncedFolder.getAccount().equals(currentAccount.name)) {
                        currentAccountSyncedFoldersList.add(syncedFolder);
                    }
                }

                syncFolderItems = sortSyncedFolderItems(mergeFolderData(currentAccountSyncedFoldersList,
                        mediaFolders));

                mHandler.post(new TimerTask() {
                    @Override
                    public void run() {
                        // strange hack to make everything work as expected
                        if (syncFolderItems.size() > 0) {
                            syncFolderItems.add(0, syncFolderItems.get(0));
                        }
                        mAdapter.setSyncFolderItems(syncFolderItems);
                        setListShown(true);
                    }
                });
            }
        }).start();
    }

    /**
     * merges two lists of {@link SyncedFolder} and {@link MediaFolder} items into one of SyncedFolderItems.
     *
     * @param syncedFolders the synced folders
     * @param mediaFolders  the media folders
     * @return the merged list of SyncedFolderItems
     */
    @NonNull
    private List<SyncedFolderDisplayItem> mergeFolderData(List<SyncedFolder> syncedFolders,
                                                          @NonNull List<MediaFolder> mediaFolders) {
        Map<String, SyncedFolder> syncedFoldersMap = createSyncedFoldersMap(syncedFolders);
        List<SyncedFolderDisplayItem> result = new ArrayList<>();


        for (MediaFolder mediaFolder : mediaFolders) {
            if (syncedFoldersMap.containsKey(mediaFolder.absolutePath)) {
                SyncedFolder syncedFolder = syncedFoldersMap.get(mediaFolder.absolutePath);
                syncedFoldersMap.remove(mediaFolder.absolutePath);
                result.add(createSyncedFolder(syncedFolder, mediaFolder));
            } else {
                result.add(createSyncedFolderFromMediaFolder(mediaFolder));
            }
        }

        for (SyncedFolder syncedFolder : syncedFoldersMap.values()) {
            SyncedFolderDisplayItem syncedFolderDisplayItem = createSyncedFolderWithoutMediaFolder(syncedFolder);
            result.add(syncedFolderDisplayItem);
        }

        return result;
    }

    /**
     * Sorts list of {@link SyncedFolderDisplayItem}s.
     *
     * @param syncFolderItemList list of items to be sorted
     * @return sorted list of items
     */
    public static List<SyncedFolderDisplayItem> sortSyncedFolderItems(List<SyncedFolderDisplayItem>
                                                                              syncFolderItemList) {
        Collections.sort(syncFolderItemList, new Comparator<SyncedFolderDisplayItem>() {
            public int compare(SyncedFolderDisplayItem f1, SyncedFolderDisplayItem f2) {
                if (f1 == null && f2 == null) {
                    return 0;
                } else if (f1 == null) {
                    return -1;
                } else if (f2 == null) {
                    return 1;
                } else if (f1.isEnabled() && f2.isEnabled()) {
                    return f1.getFolderName().toLowerCase().compareTo(f2.getFolderName().toLowerCase());
                } else if (f1.isEnabled()) {
                    return -1;
                } else if (f2.isEnabled()) {
                    return 1;
                } else if (f1.getFolderName() == null && f2.getFolderName() == null) {
                    return 0;
                } else if (f1.getFolderName() == null) {
                    return -1;
                } else if (f2.getFolderName() == null) {
                    return 1;
                } else if (PRIORITIZED_FOLDER.equals(f1.getFolderName())) {
                    return -1;
                } else if (PRIORITIZED_FOLDER.equals(f2.getFolderName())) {
                    return 1;
                } else {
                    return f1.getFolderName().toLowerCase().compareTo(f2.getFolderName().toLowerCase());
                }
            }
        });

        return syncFolderItemList;
    }

    @NonNull
    private SyncedFolderDisplayItem createSyncedFolderWithoutMediaFolder(@NonNull SyncedFolder syncedFolder) {
        return new SyncedFolderDisplayItem(
                syncedFolder.getId(),
                syncedFolder.getLocalPath(),
                syncedFolder.getRemotePath(),
                syncedFolder.getWifiOnly(),
                syncedFolder.getChargingOnly(),
                syncedFolder.getSubfolderByDate(),
                syncedFolder.getAccount(),
                syncedFolder.getUploadAction(),
                syncedFolder.isEnabled(),
                new File(syncedFolder.getLocalPath()).getName(),
                syncedFolder.getType());
    }

    /**
     * creates a SyncedFolderDisplayItem merging a {@link SyncedFolder} and a {@link MediaFolder} object instance.
     *
     * @param syncedFolder the synced folder object
     * @param mediaFolder  the media folder object
     * @return the created SyncedFolderDisplayItem
     */
    @NonNull
    private SyncedFolderDisplayItem createSyncedFolder(@NonNull SyncedFolder syncedFolder, @NonNull MediaFolder mediaFolder) {
        return new SyncedFolderDisplayItem(
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
                mediaFolder.numberOfFiles,
                mediaFolder.type);
    }

    /**
     * creates a {@link SyncedFolderDisplayItem} based on a {@link MediaFolder} object instance.
     *
     * @param mediaFolder the media folder object
     * @return the created SyncedFolderDisplayItem
     */
    @NonNull
    private SyncedFolderDisplayItem createSyncedFolderFromMediaFolder(@NonNull MediaFolder mediaFolder) {
        return new SyncedFolderDisplayItem(
                UNPERSISTED_ID,
                mediaFolder.absolutePath,
                getString(R.string.instant_upload_path) + "/" + mediaFolder.folderName,
                true,
                false,
                false,
                AccountUtils.getCurrentOwnCloudAccount(this).name,
                FileUploader.LOCAL_BEHAVIOUR_FORGET,
                false,
                mediaFolder.filePaths,
                mediaFolder.folderName,
                mediaFolder.numberOfFiles,
                mediaFolder.type);
    }

    /**
     * creates a lookup map for a list of given {@link SyncedFolder}s with their local path as the key.
     *
     * @param syncFolders list of {@link SyncedFolder}s
     * @return the lookup map for {@link SyncedFolder}s
     */
    @NonNull
    private Map<String, SyncedFolder> createSyncedFoldersMap(List<SyncedFolder> syncFolders) {
        Map<String, SyncedFolder> result = new HashMap<>();
        if (syncFolders != null) {
            for (SyncedFolder syncFolder : syncFolders) {
                result.put(syncFolder.getLocalPath(), syncFolder);
            }
        }
        return result;
    }

    /**
     * show/hide recycler view list or the empty message / progress info.
     *
     * @param shown flag if list should be shown
     */
    private void setListShown(boolean shown) {
        if (mRecyclerView != null) {
            mRecyclerView.setVisibility(shown ? View.VISIBLE : View.GONE);
            mProgress.setVisibility(shown ? View.GONE : View.VISIBLE);
            mEmpty.setVisibility(shown && mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = true;
        switch (item.getItemId()) {
            case android.R.id.home: {
                if (showSidebar) {
                    if (isDrawerOpen()) {
                        closeDrawer();
                    } else {
                        openDrawer();
                    }
                } else {
                    Intent settingsIntent = new Intent(getApplicationContext(), Preferences.class);
                    startActivity(settingsIntent);
                }
                break;
            }

            default:
                result = super.onOptionsItemSelected(item);
                break;
        }
        return result;
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
    public void onSyncStatusToggleClick(int section, SyncedFolderDisplayItem syncedFolderDisplayItem) {
        if (syncedFolderDisplayItem.getId() > UNPERSISTED_ID) {
            mSyncedFolderProvider.updateFolderSyncEnabled(syncedFolderDisplayItem.getId(),
                    syncedFolderDisplayItem.isEnabled());
        } else {
            long storedId = mSyncedFolderProvider.storeFolderSync(syncedFolderDisplayItem);
            if (storedId != -1) {
                syncedFolderDisplayItem.setId(storedId);
            }
        }
    }

    @Override
    public void onSyncFolderSettingsClick(int section, SyncedFolderDisplayItem syncedFolderDisplayItem) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);

        mSyncedFolderPreferencesDialogFragment = SyncedFolderPreferencesDialogFragment.newInstance(
                syncedFolderDisplayItem, section);
        mSyncedFolderPreferencesDialogFragment.show(ft, SYNCED_FOLDER_PREFERENCES_DIALOG_TAG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SyncedFolderPreferencesDialogFragment.REQUEST_CODE__SELECT_REMOTE_FOLDER
                && resultCode == RESULT_OK && mSyncedFolderPreferencesDialogFragment != null) {
            OCFile chosenFolder = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
            mSyncedFolderPreferencesDialogFragment.setRemoteFolderSummary(chosenFolder.getRemotePath());
        } if (requestCode == SyncedFolderPreferencesDialogFragment.REQUEST_CODE__SELECT_LOCAL_FOLDER
                && resultCode == RESULT_OK && mSyncedFolderPreferencesDialogFragment != null) {
            String localPath = data.getStringExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES);
            mSyncedFolderPreferencesDialogFragment.setLocalFolderSummary(localPath);
        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSaveSyncedFolderPreference(SyncedFolderParcelable syncedFolder) {
        SyncedFolderDisplayItem item = syncFolderItems.get(syncedFolder.getSection());
        boolean dirty = item.isEnabled() != syncedFolder.getEnabled();
        item = updateSyncedFolderItem(item, syncedFolder.getLocalPath(), syncedFolder.getRemotePath(), syncedFolder
                .getWifiOnly(), syncedFolder.getChargingOnly(), syncedFolder.getSubfolderByDate(), syncedFolder
                .getUploadAction(), syncedFolder.getEnabled());

        if (syncedFolder.getId() == UNPERSISTED_ID) {
            // newly set up folder sync config
            long storedId = mSyncedFolderProvider.storeFolderSync(item);
            if (storedId != -1) {
                item.setId(storedId);
            }

        } else {
            // existing synced folder setup to be updated
            mSyncedFolderProvider.updateSyncFolder(item);
        }
        mSyncedFolderPreferencesDialogFragment = null;

        if (dirty) {
            mAdapter.setSyncFolderItem(syncedFolder.getSection(), item);
        }
    }

    @Override
    public void onCancelSyncedFolderPreference() {
        mSyncedFolderPreferencesDialogFragment = null;
    }

    /**
     * update given synced folder with the given values.
     *
     * @param item            the synced folder to be updated
     * @param localPath       the local path
     * @param remotePath      the remote path
     * @param wifiOnly        upload on wifi only
     * @param chargingOnly    upload on charging only
     * @param subfolderByDate created sub folders
     * @param uploadAction    upload action
     * @param enabled         is sync enabled
     * @return the updated item
     */
    private SyncedFolderDisplayItem updateSyncedFolderItem(SyncedFolderDisplayItem item,
                                                           String localPath,
                                                           String remotePath,
                                                           Boolean wifiOnly,
                                                           Boolean chargingOnly,
                                                           Boolean subfolderByDate,
                                                           Integer uploadAction,
                                                           Boolean enabled) {
        item.setLocalPath(localPath);
        item.setRemotePath(remotePath);
        item.setWifiOnly(wifiOnly);
        item.setChargingOnly(chargingOnly);
        item.setSubfolderByDate(subfolderByDate);
        item.setUploadAction(uploadAction);
        item.setEnabled(enabled);
        return item;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    int gridWidth = getResources().getInteger(R.integer.media_grid_width);
                    load(gridWidth * 2, true);
                } else {
                    // permission denied --> do nothing
                    return;
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(CustomFolderEvent event) {
        Log.d(TAG, "Show custom folder magic here");
        SyncedFolderDisplayItem emptyCustomFolder = new SyncedFolderDisplayItem(
                SyncedFolder.UNPERSISTED_ID, null, null, true, false,
                false, AccountUtils.getCurrentOwnCloudAccount(this).name,
                FileUploader.LOCAL_BEHAVIOUR_FORGET, false, null, MediaFolder.CUSTOM);
        onSyncFolderSettingsClick(0, emptyCustomFolder);
    };

}
