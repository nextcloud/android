/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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

import com.nextcloud.client.account.User;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.jobs.BackgroundJobManager;
import com.nextcloud.client.jobs.MediaFoldersDetectionWork;
import com.nextcloud.client.jobs.NotificationWork;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.SyncedFoldersLayoutBinding;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.ui.adapter.SyncedFolderAdapter;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.SyncedFolderUtils;
import com.owncloud.android.utils.theme.ThemeButtonUtils;
import com.owncloud.android.utils.theme.ThemeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.GridLayoutManager;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static com.owncloud.android.datamodel.SyncedFolderDisplayItem.UNPERSISTED_ID;

/**
 * Activity displaying all auto-synced folders and/or instant upload media folders.
 */
public class SyncedFoldersActivity extends FileActivity implements SyncedFolderAdapter.ClickListener,
        SyncedFolderPreferencesDialogFragment.OnSyncedFolderPreferenceListener, Injectable {

    private static final String[] PRIORITIZED_FOLDERS = new String[]{"Camera", "Screenshots"};
    private static final String SYNCED_FOLDER_PREFERENCES_DIALOG_TAG = "SYNCED_FOLDER_PREFERENCES_DIALOG";
    private static final String TAG = SyncedFoldersActivity.class.getSimpleName();

    private SyncedFoldersLayoutBinding binding;
    private SyncedFolderAdapter adapter;
    private SyncedFolderProvider syncedFolderProvider;
    private SyncedFolderPreferencesDialogFragment syncedFolderPreferencesDialogFragment;

    private String path;
    private int type;
    @Inject AppPreferences preferences;
    @Inject PowerManagementService powerManagementService;
    @Inject Clock clock;
    @Inject BackgroundJobManager backgroundJobManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = SyncedFoldersLayoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getIntent() != null && getIntent().getExtras() != null) {
            final String accountName = getIntent().getExtras().getString(NotificationWork.KEY_NOTIFICATION_ACCOUNT);
            Optional<User> optionalUser = getUser();
            if (optionalUser.isPresent() && accountName != null) {
                User user = optionalUser.get();
                if (!accountName.equalsIgnoreCase(user.getAccountName())) {
                    accountManager.setCurrentOwnCloudAccount(accountName);
                    setUser(getUserAccountManager().getUser());
                }
            }

            path = getIntent().getStringExtra(MediaFoldersDetectionWork.KEY_MEDIA_FOLDER_PATH);
            type = getIntent().getIntExtra(MediaFoldersDetectionWork.KEY_MEDIA_FOLDER_TYPE, -1);

            // Cancel notification
            int notificationId = getIntent().getIntExtra(MediaFoldersDetectionWork.NOTIFICATION_ID, 0);
            NotificationManager notificationManager =
                (NotificationManager) getSystemService(Activity.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }

        // setup toolbar
        setupToolbar();
        updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_synced_folders));

        setupDrawer();
        setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        if (mDrawerToggle != null) {
            mDrawerToggle.setDrawerIndicatorEnabled(false);
        }

        // TODO: The content loading should be done asynchronously
        setupContent();

        if (ThemeUtils.themingEnabled(this)) {
            setTheme(R.style.FallbackThemingTheme);
        }

        binding.emptyList.emptyListViewAction.setOnClickListener(v -> showHiddenItems());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_synced_folders, menu);

        if (powerManagementService.isPowerSavingExclusionAvailable()) {
            MenuItem item = menu.findItem(R.id.action_disable_power_save_check);
            item.setVisible(true);

            item.setChecked(preferences.isPowerCheckDisabled());

            item.setOnMenuItemClickListener(this::onDisablePowerSaveCheckClicked);
        }

        return true;
    }

    private boolean onDisablePowerSaveCheckClicked(MenuItem powerCheck) {
        if (!powerCheck.isChecked()) {
            showPowerCheckDialog();
        }

        preferences.setPowerCheckDisabled(!powerCheck.isChecked());
        powerCheck.setChecked(!powerCheck.isChecked());

        return true;
    }

    private void showPowerCheckDialog() {
        AlertDialog alertDialog = new AlertDialog.Builder(this)
            .setView(findViewById(R.id.root_layout))
            .setPositiveButton(R.string.common_ok, (dialog, which) -> dialog.dismiss())
            .setTitle(R.string.autoupload_disable_power_save_check)
            .setMessage(getString(R.string.power_save_check_dialog_message))
            .show();

        ThemeButtonUtils.themeBorderlessButton(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE));
    }

    /**
     * sets up the UI elements and loads all media/synced folders.
     */
    private void setupContent() {
        final int gridWidth = getResources().getInteger(R.integer.media_grid_width);
        boolean lightVersion = getResources().getBoolean(R.bool.syncedFolder_light);
        adapter = new SyncedFolderAdapter(this, clock, gridWidth, this, lightVersion);
        syncedFolderProvider = new SyncedFolderProvider(getContentResolver(), preferences, clock);
        binding.emptyList.emptyListIcon.setImageResource(R.drawable.nav_synced_folders);
        ThemeButtonUtils.colorPrimaryButton(binding.emptyList.emptyListViewAction, this);

        final GridLayoutManager lm = new GridLayoutManager(this, gridWidth);
        adapter.setLayoutManager(lm);
        int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing);
        binding.list.addItemDecoration(new MediaGridItemDecoration(spacing));
        binding.list.setLayoutManager(lm);
        binding.list.setAdapter(adapter);

        load(gridWidth * 2, false);
    }

    public void showHiddenItems() {
        if (adapter.getSectionCount() == 0 && adapter.getUnfilteredSectionCount() > adapter.getSectionCount()) {
            adapter.toggleHiddenItemsVisibility();
            binding.emptyList.emptyListView.setVisibility(View.GONE);
            binding.list.setVisibility(View.VISIBLE);
        }
    }

    /**
     * loads all media/synced folders, adds them to the recycler view adapter and shows the list.
     *
     * @param perFolderMediaItemLimit the amount of media items to be loaded/shown per media folder
     */
    private void load(final int perFolderMediaItemLimit, boolean force) {
        if (adapter.getItemCount() > 0 && !force) {
            return;
        }
        showLoadingContent();
        final List<MediaFolder> mediaFolders = MediaProvider.getImageFolders(getContentResolver(),
                perFolderMediaItemLimit, this, false);
        mediaFolders.addAll(MediaProvider.getVideoFolders(getContentResolver(), perFolderMediaItemLimit,
                this, false));

        List<SyncedFolder> syncedFolderArrayList = syncedFolderProvider.getSyncedFolders();
        List<SyncedFolder> currentAccountSyncedFoldersList = new ArrayList<>();
        User user = getUserAccountManager().getUser();
        for (SyncedFolder syncedFolder : syncedFolderArrayList) {
            if (syncedFolder.getAccount().equals(user.getAccountName())) {
                // delete non-existing & disabled synced folders
                if (!new File(syncedFolder.getLocalPath()).exists() && !syncedFolder.isEnabled()) {
                    syncedFolderProvider.deleteSyncedFolder(syncedFolder.getId());
                } else {
                    currentAccountSyncedFoldersList.add(syncedFolder);
                }
            }
        }

        List<SyncedFolderDisplayItem> syncFolderItems = sortSyncedFolderItems(
                mergeFolderData(currentAccountSyncedFoldersList, mediaFolders));

        adapter.setSyncFolderItems(syncFolderItems);
        adapter.notifyDataSetChanged();
        showList();

        if (!TextUtils.isEmpty(path)) {
            int section = adapter.getSectionByLocalPathAndType(path, type);
            if (section >= 0) {
                onSyncFolderSettingsClick(section, adapter.get(section));
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
                if (f1.getFolderName() == null) {
                    return -1;
                }
                if (f2.getFolderName() == null) {
                    return 1;
                }

                return f1.getFolderName().toLowerCase(Locale.getDefault()).compareTo(
                    f2.getFolderName().toLowerCase(Locale.getDefault()));
            } else if (f1.getFolderName() == null && f2.getFolderName() == null) {
                return 0;
            } else if (f1.isEnabled()) {
                return -1;
            } else if (f2.isEnabled()) {
                return 1;
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

                if (syncedFolder != null && SyncedFolderUtils.isQualifyingMediaFolder(syncedFolder)) {
                    if (MediaFolderType.CUSTOM == syncedFolder.getType()) {
                        result.add(createSyncedFolderWithoutMediaFolder(syncedFolder));
                    } else {
                        result.add(createSyncedFolder(syncedFolder, mediaFolder));
                    }
                }
            } else {
                if (SyncedFolderUtils.isQualifyingMediaFolder(mediaFolder)) {
                    result.add(createSyncedFolderFromMediaFolder(mediaFolder));
                }
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
        File[] files = SyncedFolderUtils.getFileList(localFolder);
        List<String> filePaths = getDisplayFilePathList(files);

        return new SyncedFolderDisplayItem(
            syncedFolder.getId(),
            syncedFolder.getLocalPath(),
            syncedFolder.getRemotePath(),
            syncedFolder.isWifiOnly(),
            syncedFolder.isChargingOnly(),
            syncedFolder.isExisting(),
            syncedFolder.isSubfolderByDate(),
            syncedFolder.getAccount(),
            syncedFolder.getUploadAction(),
            syncedFolder.getNameCollisionPolicyInt(),
            syncedFolder.isEnabled(),
            clock.getCurrentTime(),
            filePaths,
            localFolder.getName(),
            files.length,
            syncedFolder.getType(),
            syncedFolder.isHidden());
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
            syncedFolder.isWifiOnly(),
            syncedFolder.isChargingOnly(),
            syncedFolder.isExisting(),
            syncedFolder.isSubfolderByDate(),
            syncedFolder.getAccount(),
            syncedFolder.getUploadAction(),
            syncedFolder.getNameCollisionPolicyInt(),
            syncedFolder.isEnabled(),
            clock.getCurrentTime(),
            mediaFolder.filePaths,
            mediaFolder.folderName,
            mediaFolder.numberOfFiles,
            mediaFolder.type,
            syncedFolder.isHidden());
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
                true,
                false,
                getAccount().name,
                FileUploader.LOCAL_BEHAVIOUR_FORGET,
                NameCollisionPolicy.ASK_USER.serialize(),
                false,
                clock.getCurrentTime(),
                mediaFolder.filePaths,
                mediaFolder.folderName,
                mediaFolder.numberOfFiles,
                mediaFolder.type,
                false);
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
     * show recycler view list or the empty message info (in case list is empty).
     */
    private void showList() {
        binding.list.setVisibility(View.VISIBLE);
        binding.loadingContent.setVisibility(View.GONE);
        checkAndShowEmptyListContent();
    }

    private void checkAndShowEmptyListContent() {
        if (adapter.getSectionCount() == 0 && adapter.getUnfilteredSectionCount() > adapter.getSectionCount()) {
            binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
            int hiddenFoldersCount = adapter.getHiddenFolderCount();

            showEmptyContent(getString(R.string.drawer_synced_folders),
                             getResources().getQuantityString(R.plurals.synced_folders_show_hidden_folders,
                                                              hiddenFoldersCount,
                                                              hiddenFoldersCount),
                             getResources().getQuantityString(R.plurals.synced_folders_show_hidden_folders,
                                                              hiddenFoldersCount,
                                                              hiddenFoldersCount));
        } else if (adapter.getSectionCount() == 0 && adapter.getUnfilteredSectionCount() == 0) {
            binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
            showEmptyContent(getString(R.string.drawer_synced_folders),
                             getString(R.string.synced_folders_no_results));
        } else {
            binding.emptyList.emptyListView.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = true;
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            finish();
        } else if (itemId == R.id.action_create_custom_folder) {
            Log.d(TAG, "Show custom folder dialog");
            SyncedFolderDisplayItem emptyCustomFolder = new SyncedFolderDisplayItem(
                    UNPERSISTED_ID,
                    null,
                    null,
                    true,
                    false,
                    true,
                    false,
                    getAccount().name,
                    FileUploader.LOCAL_BEHAVIOUR_FORGET,
                    NameCollisionPolicy.ASK_USER.serialize(),
                    false,
                    clock.getCurrentTime(),
                    null,
                    MediaFolderType.CUSTOM,
                    false);
            onSyncFolderSettingsClick(0, emptyCustomFolder);

            result = super.onOptionsItemSelected(item);
        } else {
            result = super.onOptionsItemSelected(item);
        }

        return result;
    }

    @Override
    public void onSyncStatusToggleClick(int section, SyncedFolderDisplayItem syncedFolderDisplayItem) {
        if (syncedFolderDisplayItem.getId() > UNPERSISTED_ID) {
            syncedFolderProvider.updateSyncedFolderEnabled(syncedFolderDisplayItem.getId(),
                                                           syncedFolderDisplayItem.isEnabled());
        } else {
            long storedId = syncedFolderProvider.storeSyncedFolder(syncedFolderDisplayItem);
            if (storedId != -1) {
                syncedFolderDisplayItem.setId(storedId);
            }
        }

        if (syncedFolderDisplayItem.isEnabled()) {
            backgroundJobManager.startImmediateFilesSyncJob(false, false);
            showBatteryOptimizationInfo();
        }
    }

    @Override
    public void onSyncFolderSettingsClick(int section, SyncedFolderDisplayItem syncedFolderDisplayItem) {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);

        syncedFolderPreferencesDialogFragment = SyncedFolderPreferencesDialogFragment.newInstance(
                syncedFolderDisplayItem, section);
        syncedFolderPreferencesDialogFragment.show(ft, SYNCED_FOLDER_PREFERENCES_DIALOG_TAG);
    }

    @Override
    public void onVisibilityToggleClick(int section, SyncedFolderDisplayItem syncedFolder) {
        syncedFolder.setHidden(!syncedFolder.isHidden());

        saveOrUpdateSyncedFolder(syncedFolder);
        adapter.setSyncFolderItem(section, syncedFolder);

        checkAndShowEmptyListContent();
    }

    private void showEmptyContent(String headline, String message, String action) {
        showEmptyContent(headline, message);
        binding.emptyList.emptyListViewAction.setText(action);
        binding.emptyList.emptyListViewAction.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewText.setVisibility(View.GONE);
    }

    private void showLoadingContent() {
        binding.loadingContent.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListViewAction.setVisibility(View.GONE);
    }

    private void showEmptyContent(String headline, String message) {
        binding.emptyList.emptyListViewAction.setVisibility(View.GONE);
        binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
        binding.list.setVisibility(View.GONE);
        binding.loadingContent.setVisibility(View.GONE);

        binding.emptyList.emptyListViewHeadline.setText(headline);
        binding.emptyList.emptyListViewText.setText(message);
        binding.emptyList.emptyListViewText.setVisibility(View.VISIBLE);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SyncedFolderPreferencesDialogFragment.REQUEST_CODE__SELECT_REMOTE_FOLDER
                && resultCode == RESULT_OK && syncedFolderPreferencesDialogFragment != null) {
            OCFile chosenFolder = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
            syncedFolderPreferencesDialogFragment.setRemoteFolderSummary(chosenFolder.getRemotePath());
        }
        if (requestCode == SyncedFolderPreferencesDialogFragment.REQUEST_CODE__SELECT_LOCAL_FOLDER
                && resultCode == RESULT_OK && syncedFolderPreferencesDialogFragment != null) {
            String localPath = data.getStringExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES);
            syncedFolderPreferencesDialogFragment.setLocalFolderSummary(localPath);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSaveSyncedFolderPreference(SyncedFolderParcelable syncedFolder) {
        // custom folders newly created aren't in the list already,
        // so triggering a refresh
        if (MediaFolderType.CUSTOM == syncedFolder.getType() && syncedFolder.getId() == UNPERSISTED_ID) {
            SyncedFolderDisplayItem newCustomFolder = new SyncedFolderDisplayItem(
                SyncedFolder.UNPERSISTED_ID,
                syncedFolder.getLocalPath(),
                syncedFolder.getRemotePath(),
                syncedFolder.isWifiOnly(),
                syncedFolder.isChargingOnly(),
                syncedFolder.isExisting(),
                syncedFolder.isSubfolderByDate(),
                syncedFolder.getAccount(),
                syncedFolder.getUploadAction(),
                syncedFolder.getNameCollisionPolicy().serialize(),
                syncedFolder.isEnabled(),
                clock.getCurrentTime(),
                new File(syncedFolder.getLocalPath()).getName(),
                syncedFolder.getType(),
                syncedFolder.isHidden());

            saveOrUpdateSyncedFolder(newCustomFolder);
            adapter.addSyncFolderItem(newCustomFolder);
        } else {
            SyncedFolderDisplayItem item = adapter.get(syncedFolder.getSection());
            updateSyncedFolderItem(item,
                                   syncedFolder.getId(),
                                   syncedFolder.getLocalPath(),
                                   syncedFolder.getRemotePath(),
                                   syncedFolder.isWifiOnly(),
                                   syncedFolder.isChargingOnly(),
                                   syncedFolder.isExisting(),
                                   syncedFolder.isSubfolderByDate(),
                                   syncedFolder.getUploadAction(),
                                   syncedFolder.getNameCollisionPolicy().serialize(),
                                   syncedFolder.isEnabled());

            saveOrUpdateSyncedFolder(item);

            // TODO test if notifyItemChanged is sufficient (should improve performance)
            adapter.notifyDataSetChanged();
        }

        syncedFolderPreferencesDialogFragment = null;

        if (syncedFolder.isEnabled()) {
            showBatteryOptimizationInfo();
        }
    }

    private void saveOrUpdateSyncedFolder(SyncedFolderDisplayItem item) {
        if (item.getId() == UNPERSISTED_ID) {
            // newly set up folder sync config
            storeSyncedFolder(item);
        } else {
            // existing synced folder setup to be updated
            syncedFolderProvider.updateSyncFolder(item);
            if (item.isEnabled()) {
                backgroundJobManager.startImmediateFilesSyncJob(false, false);
            } else {
                String syncedFolderInitiatedKey = "syncedFolderIntitiated_" + item.getId();

                ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(MainApp.getAppContext().
                    getContentResolver());
                arbitraryDataProvider.deleteKeyForAccount("global", syncedFolderInitiatedKey);
            }
        }
    }

    private void storeSyncedFolder(SyncedFolderDisplayItem item) {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(MainApp.getAppContext().
            getContentResolver());
        long storedId = syncedFolderProvider.storeSyncedFolder(item);
        if (storedId != -1) {
            item.setId(storedId);
            if (item.isEnabled()) {
                backgroundJobManager.startImmediateFilesSyncJob(false, false);
            } else {
                String syncedFolderInitiatedKey = "syncedFolderIntitiated_" + item.getId();
                arbitraryDataProvider.deleteKeyForAccount("global", syncedFolderInitiatedKey);
            }
        }
    }

    @Override
    public void onCancelSyncedFolderPreference() {
        syncedFolderPreferencesDialogFragment = null;
    }

    @Override
    public void onDeleteSyncedFolderPreference(SyncedFolderParcelable syncedFolder) {
        syncedFolderProvider.deleteSyncedFolder(syncedFolder.getId());
        adapter.removeItem(syncedFolder.getSection());
    }

    /**
     * update given synced folder with the given values.
     *
     * @param item            the synced folder to be updated
     * @param localPath       the local path
     * @param remotePath      the remote path
     * @param wifiOnly        upload on wifi only
     * @param chargingOnly    upload on charging only
     * @param existing        also upload existing
     * @param subfolderByDate created sub folders
     * @param uploadAction    upload action
     * @param nameCollisionPolicy what to do on name collision
     * @param enabled         is sync enabled
     */
    private void updateSyncedFolderItem(SyncedFolderDisplayItem item,
                                                           long id,
                                                           String localPath,
                                                           String remotePath,
                                                           boolean wifiOnly,
                                                           boolean chargingOnly,
                                                           boolean existing,
                                                           boolean subfolderByDate,
                                                           Integer uploadAction,
                                                           Integer nameCollisionPolicy,
                                                           boolean enabled) {
        item.setId(id);
        item.setLocalPath(localPath);
        item.setRemotePath(remotePath);
        item.setWifiOnly(wifiOnly);
        item.setChargingOnly(chargingOnly);
        item.setExisting(existing);
        item.setSubfolderByDate(subfolderByDate);
        item.setUploadAction(uploadAction);
        item.setNameCollisionPolicy(nameCollisionPolicy);
        item.setEnabled(enabled, clock.getCurrentTime());
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
    }

    private void showBatteryOptimizationInfo() {
        if (powerManagementService.isPowerSavingExclusionAvailable() || checkIfBatteryOptimizationEnabled()) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this, R.style.Theme_ownCloud_Dialog)
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
                .setNeutralButton(getString(R.string.battery_optimization_close), (dialog, which) -> dialog.dismiss())
                .setIcon(R.drawable.ic_battery_alert);

            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                AlertDialog alertDialog = alertDialogBuilder.show();
                ThemeButtonUtils.themeBorderlessButton(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                                                       alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL));
            }
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
