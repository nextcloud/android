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
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.jobs.MediaFoldersDetectionJob;
import com.owncloud.android.jobs.NotificationJob;
import com.owncloud.android.ui.adapter.SyncedFolderAdapter;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static com.owncloud.android.datamodel.SyncedFolderDisplayItem.UNPERSISTED_ID;

/**
 * Activity displaying all auto-synced folders and/or instant upload media folders.
 */
public class SyncedFoldersActivity extends FileActivity implements SyncedFolderAdapter.ClickListener,
        SyncedFolderPreferencesDialogFragment.OnSyncedFolderPreferenceListener, Injectable {

    private static final String[] PRIORITIZED_FOLDERS = new String[]{"Camera", "Screenshots"};
    private static final List<String> SPECIAL_MANUFACTURER = Arrays.asList("Samsung", "Huawei", "Xiaomi");
    public static final String EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR";
    private static final String SYNCED_FOLDER_PREFERENCES_DIALOG_TAG = "SYNCED_FOLDER_PREFERENCES_DIALOG";
    private static final String TAG = SyncedFoldersActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private SyncedFolderAdapter mAdapter;
    private LinearLayout mProgress;
    private TextView mEmpty;
    private SyncedFolderProvider mSyncedFolderProvider;
    private SyncedFolderPreferencesDialogFragment mSyncedFolderPreferencesDialogFragment;
    private boolean showSidebar = true;

    private String path;
    private int type;
    @Inject AppPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() != null) {
            showSidebar = getIntent().getExtras().getBoolean(EXTRA_SHOW_SIDEBAR);
        }

        setContentView(R.layout.synced_folders_layout);

        String account;
        Account currentAccount;
        if (getIntent() != null && getIntent().getExtras() != null) {
            account = getIntent().getExtras().getString(NotificationJob.KEY_NOTIFICATION_ACCOUNT);
            currentAccount = getAccount();

            if (account != null && currentAccount != null && !account.equalsIgnoreCase(currentAccount.name)) {
                accountManager.setCurrentOwnCloudAccount(account);
                setAccount(getUserAccountManager().getCurrentAccount());
            }

            path = getIntent().getStringExtra(MediaFoldersDetectionJob.KEY_MEDIA_FOLDER_PATH);
            type = getIntent().getIntExtra(MediaFoldersDetectionJob.KEY_MEDIA_FOLDER_TYPE, -1);

            // Cancel notification
            int notificationId = getIntent().getIntExtra(MediaFoldersDetectionJob.NOTIFICATION_ID, 0);
            NotificationManager notificationManager =
                (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }

        // setup toolbar
        setupToolbar();
        if (getSupportActionBar() != null){
            getSupportActionBar().setTitle(R.string.drawer_synced_folders);
        }

        // setup drawer
        setupDrawer(R.id.nav_synced_folders);

        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            mDrawerToggle.setDrawerIndicatorEnabled(false);
        }

        setupContent();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ThemeUtils.setColoredTitle(getSupportActionBar(), getString(R.string.drawer_synced_folders), this);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (ThemeUtils.themingEnabled(this)) {
            setTheme(R.style.FallbackThemingTheme);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.synced_folders_menu, menu);
        return true;
    }

    /**
     * sets up the UI elements and loads all media/synced folders.
     */
    private void setupContent() {
        mRecyclerView = findViewById(android.R.id.list);

        mProgress = findViewById(android.R.id.progress);
        mEmpty = findViewById(android.R.id.empty);

        final int gridWidth = getResources().getInteger(R.integer.media_grid_width);
        boolean lightVersion = getResources().getBoolean(R.bool.syncedFolder_light);
        mAdapter = new SyncedFolderAdapter(this, gridWidth, this, lightVersion);
        mSyncedFolderProvider = new SyncedFolderProvider(getContentResolver(), preferences);

        final GridLayoutManager lm = new GridLayoutManager(this, gridWidth);
        mAdapter.setLayoutManager(lm);
        int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing);
        mRecyclerView.addItemDecoration(new MediaGridItemDecoration(spacing));
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);

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
        final List<MediaFolder> mediaFolders = MediaProvider.getImageFolders(getContentResolver(),
                perFolderMediaItemLimit, this, false);
        mediaFolders.addAll(MediaProvider.getVideoFolders(getContentResolver(), perFolderMediaItemLimit,
                this, false));

        List<SyncedFolder> syncedFolderArrayList = mSyncedFolderProvider.getSyncedFolders();
        List<SyncedFolder> currentAccountSyncedFoldersList = new ArrayList<>();
        Account currentAccount = getUserAccountManager().getCurrentAccount();
        for (SyncedFolder syncedFolder : syncedFolderArrayList) {
            if (currentAccount != null && syncedFolder.getAccount().equals(currentAccount.name)) {

                // delete non-existing & disabled synced folders
                if (!new File(syncedFolder.getLocalPath()).exists() && !syncedFolder.isEnabled()) {
                    mSyncedFolderProvider.deleteSyncedFolder(syncedFolder.getId());
                } else {
                    currentAccountSyncedFoldersList.add(syncedFolder);
                }
            }
        }

        List<SyncedFolderDisplayItem> syncFolderItems = sortSyncedFolderItems(
                mergeFolderData(currentAccountSyncedFoldersList, mediaFolders));

        mAdapter.setSyncFolderItems(syncFolderItems);
        mAdapter.notifyDataSetChanged();
        setListShown(true);

        if (!TextUtils.isEmpty(path)) {
            int section = mAdapter.getSectionByLocalPathAndType(path, type);
            if (section >= 0) {
                onSyncFolderSettingsClick(section, mAdapter.get(section));
            }
        }
    }

    /**
     * Sorts list of {@link SyncedFolderDisplayItem}s.
     *
     * @param syncFolderItemList list of items to be sorted
     * @return sorted list of items
     */
    public static List<SyncedFolderDisplayItem> sortSyncedFolderItems(List<SyncedFolderDisplayItem>
                                                                              syncFolderItemList) {
        Collections.sort(syncFolderItemList, (f1, f2) -> {
            if (f1 == null && f2 == null) {
                return 0;
            } else if (f1 == null) {
                return -1;
            } else if (f2 == null) {
                return 1;
            } else if (f1.isEnabled() && f2.isEnabled()) {
                return f1.getFolderName().toLowerCase(Locale.getDefault()).compareTo(
                    f2.getFolderName().toLowerCase(Locale.getDefault()));
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
            }

            for (String folder : PRIORITIZED_FOLDERS) {
                if (folder.equals(f1.getFolderName()) && folder.equals(f2.getFolderName())) {
                    return 0;
                } else if (folder.equals(f1.getFolderName())) {
                    return -1;
                } else if (folder.equals(f2.getFolderName())) {
                    return 1;
                }
            }
            return f1.getFolderName().toLowerCase(Locale.getDefault()).compareTo(
                f2.getFolderName().toLowerCase(Locale.getDefault()));
        });

        return syncFolderItemList;
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
            if (syncedFoldersMap.containsKey(mediaFolder.absolutePath + "-" + mediaFolder.type)) {
                SyncedFolder syncedFolder = syncedFoldersMap.get(mediaFolder.absolutePath + "-" + mediaFolder.type);
                syncedFoldersMap.remove(mediaFolder.absolutePath + "-" + mediaFolder.type);

                if (MediaFolderType.CUSTOM == syncedFolder.getType()) {
                    result.add(createSyncedFolderWithoutMediaFolder(syncedFolder));
                } else {
                    result.add(createSyncedFolder(syncedFolder, mediaFolder));
                }
            } else {
                result.add(createSyncedFolderFromMediaFolder(mediaFolder));
            }
        }

        for (SyncedFolder syncedFolder : syncedFoldersMap.values()) {
            result.add(createSyncedFolderWithoutMediaFolder(syncedFolder));
        }

        return result;
    }

    @NonNull
    private SyncedFolderDisplayItem createSyncedFolderWithoutMediaFolder(@NonNull SyncedFolder syncedFolder) {

        File localFolder = new File(syncedFolder.getLocalPath());
        File[] files = getFileList(localFolder);
        List<String> filePaths = getDisplayFilePathList(files);

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
                filePaths,
                localFolder.getName(),
                files.length,
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
            getAccount().name,
                FileUploader.LOCAL_BEHAVIOUR_FORGET,
                false,
                mediaFolder.filePaths,
                mediaFolder.folderName,
                mediaFolder.numberOfFiles,
                mediaFolder.type);
    }

    private File[] getFileList(File localFolder) {
        File[] files = localFolder.listFiles(pathname -> !pathname.isDirectory());

        if (files != null) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
        } else {
            files = new File[]{};
        }

        return files;
    }

    private List<String> getDisplayFilePathList(File... files) {
        List<String> filePaths = null;

        if (files != null && files.length > 0) {
            filePaths = new ArrayList<>();
            for (int i = 0; i < 7 && i < files.length; i++) {
                filePaths.add(files[i].getAbsolutePath());
            }
        }

        return filePaths;
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
                result.put(syncFolder.getLocalPath() + "-" + syncFolder.getType(), syncFolder);
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
                    Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(settingsIntent);
                }
                break;
            }

            case R.id.action_create_custom_folder: {
                Log.d(TAG, "Show custom folder dialog");
                SyncedFolderDisplayItem emptyCustomFolder = new SyncedFolderDisplayItem(
                    SyncedFolder.UNPERSISTED_ID, null, null, true, false,
                    false, getAccount().name,
                    FileUploader.LOCAL_BEHAVIOUR_FORGET, false, null, MediaFolderType.CUSTOM);
                onSyncFolderSettingsClick(0, emptyCustomFolder);
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
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(MainApp.getAppContext().
                getContentResolver());

        if (syncedFolderDisplayItem.getId() > UNPERSISTED_ID) {
            mSyncedFolderProvider.updateSyncedFolderEnabled(syncedFolderDisplayItem.getId(),
                    syncedFolderDisplayItem.isEnabled());
        } else {
            long storedId = mSyncedFolderProvider.storeSyncedFolder(syncedFolderDisplayItem);
            if (storedId != -1) {
                syncedFolderDisplayItem.setId(storedId);
            }
        }

        if (syncedFolderDisplayItem.isEnabled()) {
            FilesSyncHelper.insertAllDBEntriesForSyncedFolder(syncedFolderDisplayItem);

            showBatteryOptimizationInfo();
        } else {
            String syncedFolderInitiatedKey = "syncedFolderIntitiated_" + syncedFolderDisplayItem.getId();
            arbitraryDataProvider.deleteKeyForAccount("global", syncedFolderInitiatedKey);
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
        }
        if (requestCode == SyncedFolderPreferencesDialogFragment.REQUEST_CODE__SELECT_LOCAL_FOLDER
                && resultCode == RESULT_OK && mSyncedFolderPreferencesDialogFragment != null) {
            String localPath = data.getStringExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES);
            mSyncedFolderPreferencesDialogFragment.setLocalFolderSummary(localPath);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSaveSyncedFolderPreference(SyncedFolderParcelable syncedFolder) {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(MainApp.getAppContext().
                getContentResolver());

        // custom folders newly created aren't in the list already,
        // so triggering a refresh
        if (MediaFolderType.CUSTOM == syncedFolder.getType() && syncedFolder.getId() == UNPERSISTED_ID) {
            SyncedFolderDisplayItem newCustomFolder = new SyncedFolderDisplayItem(
                    SyncedFolder.UNPERSISTED_ID, syncedFolder.getLocalPath(), syncedFolder.getRemotePath(),
                    syncedFolder.getWifiOnly(), syncedFolder.getChargingOnly(), syncedFolder.getSubfolderByDate(),
                    syncedFolder.getAccount(), syncedFolder.getUploadAction(), syncedFolder.getEnabled(),
                    new File(syncedFolder.getLocalPath()).getName(), syncedFolder.getType());
            long storedId = mSyncedFolderProvider.storeSyncedFolder(newCustomFolder);
            if (storedId != -1) {
                newCustomFolder.setId(storedId);
                if (newCustomFolder.isEnabled()) {
                    FilesSyncHelper.insertAllDBEntriesForSyncedFolder(newCustomFolder);
                } else {
                    String syncedFolderInitiatedKey = "syncedFolderIntitiated_" + newCustomFolder.getId();
                    arbitraryDataProvider.deleteKeyForAccount("global", syncedFolderInitiatedKey);
                }
            }
            mAdapter.addSyncFolderItem(newCustomFolder);
        } else {
            SyncedFolderDisplayItem item = mAdapter.get(syncedFolder.getSection());
            item = updateSyncedFolderItem(item, syncedFolder.getLocalPath(), syncedFolder.getRemotePath(), syncedFolder
                    .getWifiOnly(), syncedFolder.getChargingOnly(), syncedFolder.getSubfolderByDate(), syncedFolder
                    .getUploadAction(), syncedFolder.getEnabled());

            if (syncedFolder.getId() == UNPERSISTED_ID) {
                // newly set up folder sync config
                long storedId = mSyncedFolderProvider.storeSyncedFolder(item);
                if (storedId != -1) {
                    item.setId(storedId);
                    if (item.isEnabled()) {
                        FilesSyncHelper.insertAllDBEntriesForSyncedFolder(item);
                    } else {
                        String syncedFolderInitiatedKey = "syncedFolderIntitiated_" + item.getId();
                        arbitraryDataProvider.deleteKeyForAccount("global", syncedFolderInitiatedKey);
                    }
                }
            } else {
                // existing synced folder setup to be updated
                mSyncedFolderProvider.updateSyncFolder(item);
                if (item.isEnabled()) {
                    FilesSyncHelper.insertAllDBEntriesForSyncedFolder(item);
                } else {
                    String syncedFolderInitiatedKey = "syncedFolderIntitiated_" + item.getId();
                    arbitraryDataProvider.deleteKeyForAccount("global", syncedFolderInitiatedKey);
                }
            }

            mAdapter.setSyncFolderItem(syncedFolder.getSection(), item);
        }

        mSyncedFolderPreferencesDialogFragment = null;

        if (syncedFolder.getEnabled()) {
            showBatteryOptimizationInfo();
        }
    }

    @Override
    public void onCancelSyncedFolderPreference() {
        mSyncedFolderPreferencesDialogFragment = null;
    }

    @Override
    public void onDeleteSyncedFolderPreference(SyncedFolderParcelable syncedFolder) {
        mSyncedFolderProvider.deleteSyncedFolder(syncedFolder.getId());
        mAdapter.removeItem(syncedFolder.getSection());
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

    @Override
    protected void onResume() {
        super.onResume();

        setDrawerMenuItemChecked(R.id.nav_synced_folders);
    }

    private void showBatteryOptimizationInfo() {

        boolean isSpecialManufacturer = SPECIAL_MANUFACTURER.contains(Build.MANUFACTURER.toLowerCase(Locale.ROOT));

        if (isSpecialManufacturer && checkIfBatteryOptimizationEnabled() || checkIfBatteryOptimizationEnabled()) {
            AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.Theme_ownCloud_Dialog)
                .setTitle(getString(R.string.battery_optimization_title))
                .setMessage(getString(R.string.battery_optimization_message))
                .setPositiveButton(getString(R.string.battery_optimization_disable), (dialog, which) -> {
                    // show instant upload
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        @SuppressLint("BatteryLife")
                        Intent intent = new Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                   Uri.parse("package:" + BuildConfig.APPLICATION_ID));

                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        }
                    } else {
                        Intent powerUsageIntent = new Intent(Intent.ACTION_POWER_USAGE_SUMMARY);
                        if (getPackageManager().resolveActivity(powerUsageIntent, 0) != null) {
                            startActivity(powerUsageIntent);
                        } else {
                            dialog.dismiss();
                            DisplayUtils.showSnackMessage(this, getString(R.string.battery_optimization_no_setting));
                        }
                    }
                })
                .setNegativeButton(getString(R.string.battery_optimization_close), (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_battery_alert)
                .show();

            int color = ThemeUtils.primaryAccentColor(this);
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
        }
    }

    /**
     * Check if battery optimization is enabled. If unknown, fallback to true.
     *
     * @return true if battery optimization is enabled
     */
    private boolean checkIfBatteryOptimizationEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (powerManager == null) {
                return true;
            }

            return !powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID);
        } else {
            return true;
        }
    }
}
