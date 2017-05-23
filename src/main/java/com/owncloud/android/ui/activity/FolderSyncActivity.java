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

import android.accounts.Account;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.MediaProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.ui.adapter.FolderSyncAdapter;
import com.owncloud.android.ui.decoration.MediaGridItemDecoration;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;
import com.owncloud.android.ui.dialog.parcel.SyncedFolderParcelable;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;

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
    public static final int REQUEST_CODE__SELECT_REMOTE_FOLDER = 0;

    private static final String SCREEN_NAME = "Auto upload";

    private static final String TAG = FolderSyncActivity.class.getSimpleName();

    public static final String SYNCED_FOLDER_LIGHT_REMOTE_FOLDER = "SYNCED_FOLDER_LIGHT_REMOTE_FOLDER";
    public static final String SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI = "SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI";
    public static final String SYNCED_FOLDER_LIGHT_UPLOAD_ON_CHARGING = "SYNCED_FOLDER_LIGHT_UPLOAD_ON_CHARGING";
    public static final String SYNCED_FOLDER_LIGHT_USE_SUBFOLDERS = "SYNCED_FOLDER_LIGHT_USE_SUBFOLDERS";
    public static final String SYNCED_FOLDER_LIGHT_UPLOAD_BEHAVIOUR = "SYNCED_FOLDER_LIGHT_UPLOAD_BEHAVIOUR";

    private RecyclerView mRecyclerView;
    private FolderSyncAdapter mAdapter;
    private LinearLayout mProgress;
    private TextView mEmpty;
    private SyncedFolderProvider mSyncedFolderProvider;
    private List<SyncedFolderDisplayItem> syncFolderItems;
    private SyncedFolderPreferencesDialogFragment mSyncedFolderPreferencesDialogFragment;
    private ArbitraryDataProvider arbitraryDataProvider;
    private TextView mRemoteFolderSummary;
    private CheckBox mUploadOnWifiCheckbox;
    private CheckBox mUploadOnChargingCheckbox;
    private CheckBox mUploadUseSubfoldersCheckbox;
    private TextView mUploadBehaviorSummary;
    private CharSequence[] mUploadBehaviorItemStrings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        arbitraryDataProvider = new ArbitraryDataProvider(getContentResolver());


        setContentView(R.layout.folder_sync_layout);

        if (getResources().getBoolean(R.bool.syncedFolder_light)) {
            setupLightOption();
        } else {
            findViewById(R.id.folder_sync_light_linear_layout).setVisibility(View.GONE);
        }

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_folder_sync);
        getSupportActionBar().setTitle(getString(R.string.drawer_folder_sync));

        setupContent();
    }

    private void setupLightOption() {
        // Remote folder
        mRemoteFolderSummary = (TextView) findViewById(R.id.remote_folder_summary);

        String remoteFolder = arbitraryDataProvider.getValue(getAccount(), SYNCED_FOLDER_LIGHT_REMOTE_FOLDER);

        if (remoteFolder.isEmpty()) {
            remoteFolder = getString(R.string.instant_upload_path) + "/";
            saveRemoteFolder(remoteFolder);
        } else {
            mRemoteFolderSummary.setText(remoteFolder);
        }

        findViewById(R.id.remote_folder_container).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent action = new Intent(getBaseContext(), FolderPickerActivity.class);
                action.putExtra(
                        FolderPickerActivity.EXTRA_ACTION, getResources().getText(R.string.choose_remote_folder));
                startActivityForResult(action, REQUEST_CODE__SELECT_REMOTE_FOLDER);
            }
        });

        // Upload on WiFi
        mUploadOnWifiCheckbox = (CheckBox) findViewById(R.id.setting_instant_upload_on_wifi_checkbox);
        mUploadOnWifiCheckbox.setChecked(
                arbitraryDataProvider.getBooleanValue(getAccount(), SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI));

        findViewById(R.id.setting_instant_upload_on_wifi_container).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mUploadOnWifiCheckbox.toggle();
                        arbitraryDataProvider.storeOrUpdateKeyValue(getAccount(), SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI,
                                String.valueOf(mUploadOnWifiCheckbox.isChecked()));
                    }
                });

        // Upload on charging
        mUploadOnChargingCheckbox = (CheckBox) findViewById(R.id.setting_instant_upload_on_charging_checkbox);
        mUploadOnChargingCheckbox.setChecked(
                arbitraryDataProvider.getBooleanValue(getAccount(), SYNCED_FOLDER_LIGHT_UPLOAD_ON_CHARGING));

        findViewById(R.id.setting_instant_upload_on_charging_container).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mUploadOnChargingCheckbox.toggle();
                        arbitraryDataProvider.storeOrUpdateKeyValue(getAccount(), SYNCED_FOLDER_LIGHT_UPLOAD_ON_CHARGING,
                                String.valueOf(mUploadOnChargingCheckbox.isChecked()));
                    }
                });

        // use subfolders
        mUploadUseSubfoldersCheckbox = (CheckBox) findViewById(R.id.setting_instant_upload_path_use_subfolders_checkbox);
        mUploadUseSubfoldersCheckbox.setChecked(
                arbitraryDataProvider.getBooleanValue(getAccount(), SYNCED_FOLDER_LIGHT_USE_SUBFOLDERS));

        findViewById(R.id.setting_instant_upload_path_use_subfolders_container).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mUploadUseSubfoldersCheckbox.toggle();
                        arbitraryDataProvider.storeOrUpdateKeyValue(getAccount(), SYNCED_FOLDER_LIGHT_USE_SUBFOLDERS,
                                String.valueOf(mUploadUseSubfoldersCheckbox.isChecked()));
                    }
                });

        // upload behaviour
        mUploadBehaviorItemStrings = getResources().getTextArray(R.array.pref_behaviour_entries);
        mUploadBehaviorSummary = (TextView) findViewById(R.id.setting_instant_behaviour_summary);

        Integer uploadBehaviour = arbitraryDataProvider.getIntegerValue(getAccount(), SYNCED_FOLDER_LIGHT_UPLOAD_BEHAVIOUR);

        if (uploadBehaviour == -1) {
            uploadBehaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET;
            arbitraryDataProvider.storeOrUpdateKeyValue(getAccount(), SYNCED_FOLDER_LIGHT_UPLOAD_BEHAVIOUR,
                    uploadBehaviour.toString());
        }
        mUploadBehaviorSummary.setText(mUploadBehaviorItemStrings[uploadBehaviour]);

        findViewById(R.id.setting_instant_behaviour_container).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Integer behaviourId = arbitraryDataProvider.getIntegerValue(getAccount(),
                                SYNCED_FOLDER_LIGHT_UPLOAD_BEHAVIOUR);

                        Integer behaviour;
                        switch (behaviourId) {
                            case FileUploader.LOCAL_BEHAVIOUR_FORGET:
                                behaviour = 0;
                                break;
                            case FileUploader.LOCAL_BEHAVIOUR_MOVE:
                                behaviour = 1;
                                break;
                            case FileUploader.LOCAL_BEHAVIOUR_DELETE:
                                behaviour = 2;
                                break;
                            default:
                                behaviour = 0;
                                break;
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(FolderSyncActivity.this);
                        builder
                                .setTitle(R.string.prefs_instant_behaviour_dialogTitle)
                                .setSingleChoiceItems(getResources().getTextArray(R.array.pref_behaviour_entries),
                                        behaviour,
                                        new
                                                DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        saveUploadAction(getResources().getTextArray(
                                                                R.array.pref_behaviour_entryValues)[which].toString());
                                                        mUploadBehaviorSummary.setText(mUploadBehaviorItemStrings[which]);
                                                        dialog.dismiss();
                                                    }
                                                });
                        builder.create().show();
                    }
                });
    }

    private void saveRemoteFolder(String newPath) {
        arbitraryDataProvider.storeOrUpdateKeyValue(getAccount(), SYNCED_FOLDER_LIGHT_REMOTE_FOLDER, newPath);
        mRemoteFolderSummary.setText(newPath);
    }

    private void saveUploadAction(String action) {
        Integer actionId;
        switch (action) {
            case "LOCAL_BEHAVIOUR_FORGET":
                actionId = FileUploader.LOCAL_BEHAVIOUR_FORGET;
                break;
            case "LOCAL_BEHAVIOUR_MOVE":
                actionId = FileUploader.LOCAL_BEHAVIOUR_MOVE;
                break;
            case "LOCAL_BEHAVIOUR_DELETE":
                actionId = FileUploader.LOCAL_BEHAVIOUR_DELETE;
                break;
            default:
                actionId = FileUploader.LOCAL_BEHAVIOUR_FORGET;
        }

        arbitraryDataProvider.storeOrUpdateKeyValue(getAccount(), SYNCED_FOLDER_LIGHT_UPLOAD_BEHAVIOUR,
                actionId.toString());
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

        load(gridWidth * 2);
    }

    /**
     * loads all media/synced folders, adds them to the recycler view adapter and shows the list.
     *
     * @param perFolderMediaItemLimit the amount of media items to be loaded/shown per media folder
     */
    private void load(final int perFolderMediaItemLimit) {
        if (mAdapter.getItemCount() > 0) {
            return;
        }
        setListShown(false);
        final Handler mHandler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<MediaFolder> mediaFolders = MediaProvider.getMediaFolders(getContentResolver(),
                        perFolderMediaItemLimit);
                List<SyncedFolder> syncedFolderArrayList = mSyncedFolderProvider.getSyncedFolders();
                List<SyncedFolder> currentAccountSyncedFoldersList = new ArrayList<SyncedFolder>();
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
                new File(syncedFolder.getLocalPath()).getName());
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
                mediaFolder.numberOfFiles);
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
                mediaFolder.numberOfFiles);
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
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
                break;
            }

            default:
                result = super.onOptionsItemSelected(item);
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
                && resultCode == RESULT_OK) {
            OCFile chosenFolder = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);

            if (mSyncedFolderPreferencesDialogFragment != null) {
                mSyncedFolderPreferencesDialogFragment.setRemoteFolderSummary(chosenFolder.getRemotePath());
            } else {
                saveRemoteFolder(chosenFolder.getRemotePath());
            }
        } else {
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
}
