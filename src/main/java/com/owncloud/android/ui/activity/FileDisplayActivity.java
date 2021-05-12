/*
 * ownCloud Android client application
 *
 * @author Bartek Przybylski
 * @author David A. Velasco
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 * Copyright (C) 2011  Bartek Przybylski
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.nextcloud.client.account.User;
import com.nextcloud.client.appinfo.AppInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.files.DeepLinkHandler;
import com.nextcloud.client.media.PlayerServiceConnection;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FilesBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.VirtualFolderType;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.RestoreFileVersionRemoteOperation;
import com.owncloud.android.lib.resources.files.SearchRemoteOperation;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.asynctasks.CheckAvailableSpaceTask;
import com.owncloud.android.ui.asynctasks.FetchRemoteFileTask;
import com.owncloud.android.ui.dialog.SendShareDialog;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.events.SearchEvent;
import com.owncloud.android.ui.events.SyncEventFinished;
import com.owncloud.android.ui.events.TokenPushEvent;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.fragment.TaskRetainerFragment;
import com.owncloud.android.ui.helpers.FileOperationsHelper;
import com.owncloud.android.ui.helpers.UriUploader;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewTextFileFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.ui.preview.PreviewTextStringFragment;
import com.owncloud.android.ui.preview.PreviewVideoActivity;
import com.owncloud.android.utils.DataHolderUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.parceler.Parcels;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;

/**
 * Displays, what files the user has available in his ownCloud. This is the main view.
 */
public class FileDisplayActivity extends FileActivity
        implements FileFragment.ContainerActivity,
        OnEnforceableRefreshListener, SortingOrderDialogFragment.OnSortingOrderListener,
        SendShareDialog.SendShareDialogDownloader, Injectable {

    public static final String RESTART = "RESTART";
    public static final String ALL_FILES = "ALL_FILES";
    public static final String PHOTO_SEARCH = "PHOTO_SEARCH";
    public static final int SINGLE_USER_SIZE = 1;

    private FilesBinding binding;

    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private UploadFinishReceiver mUploadFinishReceiver;
    private DownloadFinishReceiver mDownloadFinishReceiver;
    private RemoteOperationResult mLastSslUntrustedServerResult;
    @Inject LocalBroadcastManager localBroadcastManager;

    private boolean mDualPane;

    public static final String TAG_PUBLIC_LINK = "PUBLIC_LINK";
    public static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG";
    public static final String KEY_FILE_ID = "KEY_FILE_ID";
    public static final String KEY_ACCOUNT = "KEY_ACCOUNT";


    private static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    private static final String KEY_SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS";
    private static final String KEY_WAITING_TO_SEND = "WAITING_TO_SEND";

    public static final String ACTION_DETAILS = "com.owncloud.android.ui.activity.action.DETAILS";

    public static final String DRAWER_MENU_ID = "DRAWER_MENU_ID";

    public static final int REQUEST_CODE__SELECT_CONTENT_FROM_APPS = REQUEST_CODE__LAST_SHARED + 1;
    public static final int REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM = REQUEST_CODE__LAST_SHARED + 2;
    public static final int REQUEST_CODE__MOVE_FILES = REQUEST_CODE__LAST_SHARED + 3;
    public static final int REQUEST_CODE__COPY_FILES = REQUEST_CODE__LAST_SHARED + 4;
    public static final int REQUEST_CODE__UPLOAD_FROM_CAMERA = REQUEST_CODE__LAST_SHARED + 5;

    protected static final long DELAY_TO_REQUEST_REFRESH_OPERATION_LATER = DELAY_TO_REQUEST_OPERATIONS_LATER + 350;

    private static final String TAG = FileDisplayActivity.class.getSimpleName();

    public static final String TAG_LIST_OF_FILES = "LIST_OF_FILES";
    public static final String TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT";

    public static final String TEXT_PREVIEW = "TEXT_PREVIEW";

    private OCFile mWaitingToPreview;

    private boolean mSyncInProgress;

    private OCFile mWaitingToSend;

    private Collection<MenuItem> mDrawerMenuItemstoShowHideList;

    public static final String KEY_IS_SEARCH_OPEN = "IS_SEARCH_OPEN";
    public static final String KEY_SEARCH_QUERY = "SEARCH_QUERY";

    private String searchQuery = "";
    private boolean searchOpen;

    private SearchView searchView;
    private PlayerServiceConnection mPlayerConnection;
    private Account mLastDisplayedAccount;
    private int menuItemId = -1;

    @Inject
    AppPreferences preferences;

    @Inject
    AppInfo appInfo;

    @Inject
    ConnectivityService connectivityService;

    public static Intent openFileIntent(Context context, User user, OCFile file) {
        final Intent intent = new Intent(context, PreviewImageActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, file);
        intent.putExtra(FileActivity.EXTRA_ACCOUNT, user.toPlatformAccount());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        // Set the default theme to replace the launch screen theme.
        setTheme(R.style.Theme_ownCloud_Toolbar_Drawer);

        super.onCreate(savedInstanceState);
        /// Load of saved instance state
        if (savedInstanceState != null) {
            mWaitingToPreview = savedInstanceState.getParcelable(FileDisplayActivity.KEY_WAITING_TO_PREVIEW);
            mSyncInProgress = savedInstanceState.getBoolean(KEY_SYNC_IN_PROGRESS);
            mWaitingToSend = savedInstanceState.getParcelable(FileDisplayActivity.KEY_WAITING_TO_SEND);
            searchQuery = savedInstanceState.getString(KEY_SEARCH_QUERY);
            searchOpen = savedInstanceState.getBoolean(FileDisplayActivity.KEY_IS_SEARCH_OPEN, false);
        } else {
            mWaitingToPreview = null;
            mSyncInProgress = false;
            mWaitingToSend = null;
        }

        /// USER INTERFACE

        // Inflate and set the layout view
        binding = FilesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // setup toolbar
        setupHomeSearchToolbarWithSortAndListButtons();

        mMenuButton.setOnClickListener(v -> openDrawer());

        mSwitchAccountButton.setOnClickListener(v -> showManageAccountsDialog());

        mDualPane = getResources().getBoolean(R.bool.large_land_layout);

        // Init Fragment without UI to retain AsyncTask across configuration changes
        FragmentManager fm = getSupportFragmentManager();
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);
        if (taskRetainerFragment == null) {
            taskRetainerFragment = new TaskRetainerFragment();
            fm.beginTransaction()
                .add(taskRetainerFragment, TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT).commit();
        }   // else, Fragment already created and retained across configuration change

        if (Intent.ACTION_VIEW.equals(getIntent().getAction())) {
            handleOpenFileViaIntent(getIntent());
        }

        mPlayerConnection = new PlayerServiceConnection(this);

        checkStoragePath();
    }

    private void checkStoragePath() {
        String newStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
        String storagePath = preferences.getStoragePath(newStorage);
        if (!preferences.isStoragePathValid() && !new File(storagePath).exists()) {
            // falling back to default
            preferences.setStoragePath(newStorage);
            preferences.setStoragePathValid();
            MainApp.setStoragePath(newStorage);

            try {
                AlertDialog alertDialog = new AlertDialog.Builder(this, R.style.Theme_ownCloud_Dialog)
                    .setTitle(R.string.wrong_storage_path)
                    .setMessage(R.string.wrong_storage_path_desc)
                    .setNegativeButton(R.string.dialog_close, (dialog, which) -> dialog.dismiss())
                    .setIcon(R.drawable.ic_settings)
                    .create();

                alertDialog.show();
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ThemeUtils.primaryAccentColor(this));
            } catch (WindowManager.BadTokenException e) {
                Log_OC.e(TAG, "Error showing wrong storage info, so skipping it: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);


        if (!PermissionUtil.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Check if we should show an explanation
            if (PermissionUtil.shouldShowRequestPermissionRationale(this,
                                                                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show explanation to the user and then request permission
                Snackbar snackbar = Snackbar.make(binding.rootLayout,
                                                  R.string.permission_storage_access,
                                                  Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.common_ok, v -> PermissionUtil.requestWriteExternalStoreagePermission(this));
                ThemeUtils.colorSnackbar(this, snackbar);
                snackbar.show();
            } else {
                // No explanation needed, request the permission.
                PermissionUtil.requestWriteExternalStoreagePermission(this);
            }
        }

        if (getIntent().getParcelableExtra(OCFileListFragment.SEARCH_EVENT) != null) {
            switchToSearchFragment(savedInstanceState);

            int menuId = getIntent().getIntExtra(DRAWER_MENU_ID, -1);
            if (menuId != -1) {
                setupDrawer(menuId);
            }
        } else {
            createMinFragments(savedInstanceState);
            syncAndUpdateFolder(true);
        }

        upgradeNotificationForInstantUpload();
        checkOutdatedServer();
    }

    private Activity getActivity() {
        return this;
    }

    /**
     * For Android 7+.
     * Opens a pop up info for the new instant upload and disabled the old instant upload.
     */
    private void upgradeNotificationForInstantUpload() {
        // check for Android 6+ if legacy instant upload is activated --> disable + show info
        if (preferences.instantPictureUploadEnabled() || preferences.instantVideoUploadEnabled()) {
            preferences.removeLegacyPreferences();
            // show info pop-up
            new AlertDialog.Builder(this, R.style.Theme_ownCloud_Dialog)
                    .setTitle(R.string.drawer_synced_folders)
                    .setMessage(R.string.synced_folders_new_info)
                    .setPositiveButton(R.string.drawer_open, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // show instant upload
                            Intent syncedFoldersIntent = new Intent(getApplicationContext(), SyncedFoldersActivity.class);
                            dialog.dismiss();
                            startActivity(syncedFoldersIntent);
                        }
                    })
                    .setNegativeButton(R.string.drawer_close, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setIcon(R.drawable.nav_synced_folders)
                    .show();
        }
    }

    private void checkOutdatedServer() {
        Account account = getAccount();

        if (getResources().getBoolean(R.bool.show_outdated_server_warning) && account != null) {
            OwnCloudVersion serverVersion = AccountUtils.getServerVersionForAccount(account, this);

            if (serverVersion == null) {
                serverVersion = getCapabilities().getVersion();
            }

            // show outdated warning
            if (getResources().getBoolean(R.bool.show_outdated_server_warning) &&
                MainApp.OUTDATED_SERVER_VERSION.isSameMajorVersion(serverVersion) &&
                getCapabilities().getExtendedSupport().isFalse()) {
                DisplayUtils.showServerOutdatedSnackbar(this, Snackbar.LENGTH_LONG);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    EventBus.getDefault().post(new TokenPushEvent());
                    syncAndUpdateFolder(true);
                    // toggle on is save since this is the only scenario this code gets accessed
                } else {
                    // permission denied --> do nothing
                    return;
                }
                return;
            }
            case PermissionUtil.PERMISSIONS_CAMERA: {
                // If request is cancelled, result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    getFileOperationsHelper()
                        .uploadFromCamera(this, FileDisplayActivity.REQUEST_CODE__UPLOAD_FROM_CAMERA);
                } else {
                    // permission denied
                    return;
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void switchToSearchFragment(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            OCFileListFragment listOfFiles = new OCFileListFragment();
            Bundle args = new Bundle();

            args.putParcelable(OCFileListFragment.SEARCH_EVENT,
                    getIntent().getParcelableExtra(OCFileListFragment.SEARCH_EVENT));
            args.putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true);

            listOfFiles.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES);
            transaction.commit();
        } else {
            getSupportFragmentManager().findFragmentByTag(TAG_LIST_OF_FILES);
        }
    }

    private void createMinFragments(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            OCFileListFragment listOfFiles = new OCFileListFragment();
            Bundle args = new Bundle();
            args.putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true);
            listOfFiles.setArguments(args);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES);
            transaction.commit();
        } else {
            getSupportFragmentManager().findFragmentByTag(TAG_LIST_OF_FILES);
        }
    }

    private void initFragmentsWithFile(User user, OCFile file) {
        /// First fragment
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null && TextUtils.isEmpty(searchQuery)) {
            listOfFiles.listDirectory(getCurrentDir(), getFile(), MainApp.isOnlyOnDevice(), false);
        } else {
            Log_OC.e(TAG, "Still have a chance to lose the initialization of list fragment >(");
        }

        /// Second fragment
        if (mDualPane) {
            Fragment secondFragment = getSecondFragment();
            if (secondFragment == null) {
                secondFragment = chooseInitialSecondFragment(file, user);
            }

            if (secondFragment != null) {
                setSecondFragment(secondFragment);
                updateFragmentsVisibility(true);
                updateActionBarTitleAndHomeButton(file);
            } else {
                cleanSecondFragment();
                if (file.isDown() && MimeTypeUtil.isVCard(file.getMimeType())) {
                    startContactListFragment(file);
                } else if (file.isDown() && PreviewTextFileFragment.canBePreviewed(file)) {
                    startTextPreview(file, false);
                }
            }
        } else {
            cleanSecondFragment();
        }
    }

    // Is called with the flag FLAG_ACTIVITY_SINGLE_TOP and set the new file and intent
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        if (ACTION_DETAILS.equalsIgnoreCase(intent.getAction())) {
            OCFile file = intent.getParcelableExtra(EXTRA_FILE);
            setFile(file);
            setIntent(intent);
            setFile(intent.getParcelableExtra(EXTRA_FILE));
            showDetails(file);
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            handleOpenFileViaIntent(intent);
        } else if (RESTART.equals(intent.getAction())) {
            finish();
            startActivity(intent);
        } else // Verify the action and get the query
            if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
                setIntent(intent);

                SearchEvent searchEvent = Parcels.unwrap(intent.getParcelableExtra(OCFileListFragment.SEARCH_EVENT));
                if (searchEvent != null) {
                    if (SearchRemoteOperation.SearchType.PHOTO_SEARCH.equals(searchEvent.searchType)) {
                        Log_OC.d(this, "Switch to photo search fragment");

                        GalleryFragment photoFragment = new GalleryFragment(true);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent));
                        photoFragment.setArguments(bundle);
                        setLeftFragment(photoFragment);
                    } else {
                        Log_OC.d(this, "Switch to oc file search fragment");

                        OCFileListFragment photoFragment = new OCFileListFragment();
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent));
                        photoFragment.setArguments(bundle);
                        setLeftFragment(photoFragment);
                    }
                }
            } else if (ALL_FILES.equals(intent.getAction())) {
                Log_OC.d(this, "Switch to oc file fragment");

                setLeftFragment(new OCFileListFragment());
            }
    }

    @Deprecated
    private Fragment chooseInitialSecondFragment(OCFile file, User user) {
        Fragment secondFragment = null;
        if (file != null && !file.isFolder()) {
            if (file.isDown() && PreviewMediaFragment.canBePreviewed(file)) {
                int startPlaybackPosition = getIntent().getIntExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0);
                boolean autoplay = getIntent().getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, true);
                secondFragment = PreviewMediaFragment.newInstance(file, user, startPlaybackPosition, autoplay);
            } else if (file.isDown() && PreviewTextFileFragment.canBePreviewed(file)) {
                secondFragment = null;
            } else {
                secondFragment = FileDetailFragment.newInstance(file, user);
            }
        }
        return secondFragment;
    }

    /**
     * Replaces the first fragment managed by the activity with the received as a parameter.
     *
     * @param fragment New Fragment to set.
     */
    private void setLeftFragment(Fragment fragment) {
        if (searchView != null) {
            searchView.post(() -> searchView.setQuery(searchQuery, true));
        }
        setDrawerIndicatorEnabled(false);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.addToBackStack(null);
        transaction.replace(R.id.left_fragment_container, fragment, TAG_LIST_OF_FILES);
        transaction.commit();
    }


    /**
     * Replaces the second fragment managed by the activity with the received as a parameter.
     * <p>
     * Assumes never will be more than two fragments managed at the same time.
     *
     * @param fragment New second Fragment to set.
     */
    @Deprecated // in future no dual pane
    private void setSecondFragment(Fragment fragment) {
        if (searchView != null) {
            searchView.post(new Runnable() {
                @Override
                public void run() {
                    searchView.setQuery(searchQuery, true);
                }
            });
        }
        setDrawerIndicatorEnabled(false);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.right_fragment_container, fragment, TAG_SECOND_FRAGMENT);
        transaction.commit();
    }


    @Deprecated
    private void updateFragmentsVisibility(boolean existsSecondFragment) {
        if (mDualPane) {
            if (binding.leftFragmentContainer.getVisibility() != View.VISIBLE) {
                binding.leftFragmentContainer.setVisibility(View.VISIBLE);
            }
            if (binding.rightFragmentContainer.getVisibility() != View.VISIBLE) {
                binding.rightFragmentContainer.setVisibility(View.VISIBLE);
            }

        } else if (existsSecondFragment) {
            if (binding.leftFragmentContainer.getVisibility() != View.GONE) {
                binding.leftFragmentContainer.setVisibility(View.GONE);
            }
            if (binding.rightFragmentContainer.getVisibility() != View.VISIBLE) {
                binding.rightFragmentContainer.setVisibility(View.VISIBLE);
            }

        } else {
            if (binding.leftFragmentContainer.getVisibility() != View.VISIBLE) {
                binding.leftFragmentContainer.setVisibility(View.VISIBLE);
            }
            if (binding.rightFragmentContainer.getVisibility() != View.GONE) {
                binding.rightFragmentContainer.setVisibility(View.GONE);
            }
        }
    }

    public @androidx.annotation.Nullable
    Fragment getLeftFragment() {
        return getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES);
    }

    public @androidx.annotation.Nullable
    @Deprecated
    OCFileListFragment getListOfFilesFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(
            FileDisplayActivity.TAG_LIST_OF_FILES);
        if (listOfFiles instanceof OCFileListFragment) {
            return (OCFileListFragment) listOfFiles;
        }
        Log_OC.e(TAG, "Access to unexisting list of files fragment!!");
        return null;
    }

    public @Nullable
    @Deprecated
    FileFragment getSecondFragment() {
        Fragment second = getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_SECOND_FRAGMENT);
        if (second != null) {
            return (FileFragment) second;
        }
        return null;
    }

    @Deprecated
    protected void cleanSecondFragment() {
        Fragment second = getSecondFragment();
        if (second != null) {
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            tr.remove(second);
            tr.commit();
        }
        updateFragmentsVisibility(false);
        updateActionBarTitleAndHomeButton(null);

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) findViewById(R.id.root_layout).getLayoutParams();
        params.setBehavior(new AppBarLayout.ScrollingViewBehavior());
    }

    public void updateListOfFilesFragment(boolean fromSearch) {
        OCFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) {
            fileListFragment.listDirectory(MainApp.isOnlyOnDevice(), fromSearch);
        }
    }

    public void resetSearchView() {
        OCFileListFragment fileListFragment = getListOfFilesFragment();

        if (fileListFragment != null) {
            fileListFragment.setSearchFragment(false);
        }
    }

    @Deprecated
    protected void refreshSecondFragment(String downloadEvent, String downloadedRemotePath,
                                         boolean success) {
        FileFragment secondFragment = getSecondFragment();
        boolean waitedPreview = mWaitingToPreview != null
            && mWaitingToPreview.getRemotePath().equals(downloadedRemotePath);
        if (secondFragment instanceof FileDetailFragment) {
            FileDetailFragment detailsFragment = (FileDetailFragment) secondFragment;
            OCFile fileInFragment = detailsFragment.getFile();
            if (fileInFragment != null &&
                !downloadedRemotePath.equals(fileInFragment.getRemotePath())) {
                // the user browsed to other file ; forget the automatic preview
                mWaitingToPreview = null;

            } else if (downloadEvent.equals(FileDownloader.getDownloadAddedMessage())) {
                // grant that the right panel updates the progress bar
                detailsFragment.listenForTransferProgress();
                detailsFragment.updateFileDetails(true, false);

            } else if (downloadEvent.equals(FileDownloader.getDownloadFinishMessage())) {
                //  update the right panel
                boolean detailsFragmentChanged = false;
                if (waitedPreview) {
                    if (success) {
                        // update the file from database, for the local storage path
                        mWaitingToPreview = getStorageManager().getFileById(mWaitingToPreview.getFileId());

                        if (PreviewMediaFragment.canBePreviewed(mWaitingToPreview)) {
                            startMediaPreview(mWaitingToPreview, 0, true, true, true);
                            detailsFragmentChanged = true;
                        } else if (MimeTypeUtil.isVCard(mWaitingToPreview.getMimeType())) {
                            startContactListFragment(mWaitingToPreview);
                            detailsFragmentChanged = true;
                        } else if (PreviewTextFileFragment.canBePreviewed(mWaitingToPreview)) {
                            startTextPreview(mWaitingToPreview, true);
                            detailsFragmentChanged = true;
                        } else {
                            getFileOperationsHelper().openFile(mWaitingToPreview);
                        }
                    }
                    mWaitingToPreview = null;
                }
                if (!detailsFragmentChanged) {
                    detailsFragment.updateFileDetails(false, success);
                }
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = isDrawerOpen();

        for (MenuItem menuItem : mDrawerMenuItemstoShowHideList) {
            menuItem.setVisible(!drawerOpen);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_file_display, menu);

        menu.findItem(R.id.action_select_all).setVisible(false);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);
        searchMenuItem.setVisible(false);
        mSearchText.setOnClickListener(v -> {
            showSearchView();
            searchView.setIconified(false);
        });

        ThemeUtils.themeSearchView(searchView, this);

        // populate list of menu items to show/hide when drawer is opened/closed
        mDrawerMenuItemstoShowHideList = new ArrayList<>(1);
        mDrawerMenuItemstoShowHideList.add(searchMenuItem);

        //focus the SearchView
        if (!TextUtils.isEmpty(searchQuery)) {
            searchView.post(() -> {
                searchView.setIconified(false);
                searchView.setQuery(searchQuery, true);
                searchView.clearFocus();
            });
        }

        final View mSearchEditFrame = searchView
            .findViewById(androidx.appcompat.R.id.search_edit_frame);

        searchView.setOnCloseListener(() -> {
            if (TextUtils.isEmpty(searchView.getQuery().toString())) {
                searchView.onActionViewCollapsed();
                setDrawerIndicatorEnabled(isDrawerIndicatorAvailable()); // order matters
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                mDrawerToggle.syncState();

                OCFileListFragment ocFileListFragment = getListOfFilesFragment();
                if (ocFileListFragment != null) {
                    ocFileListFragment.setSearchFragment(false);
                    ocFileListFragment.refreshDirectory();
                }
            } else {
                searchView.post(() -> searchView.setQuery("", true));
            }
            return true;
        });

        ViewTreeObserver vto = mSearchEditFrame.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            int oldVisibility = -1;

            @Override
            public void onGlobalLayout() {

                int currentVisibility = mSearchEditFrame.getVisibility();

                if (currentVisibility != oldVisibility) {
                    if (currentVisibility == View.VISIBLE) {
                        setDrawerIndicatorEnabled(false);
                    }

                    oldVisibility = currentVisibility;
                }

            }
        });

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home: {
                FileFragment second = getSecondFragment();
                OCFile currentDir = getCurrentDir();
                if (isDrawerOpen()) {
                    closeDrawer();
                } else if (
                    currentDir != null && currentDir.getParentId() != 0 ||
                        second != null && second.getFile() != null ||
                        isSearchOpen()) {
                    onBackPressed();
                } else if (getLeftFragment() instanceof FileDetailFragment) {
                    onBackPressed();
                } else {
                    openDrawer();
                }
                break;
            }
            case R.id.action_select_all: {
                OCFileListFragment fragment = getListOfFilesFragment();

                if (fragment != null) {
                    fragment.selectAllFiles(true);
                }
                break;
            }
            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    /**
     * Called, when the user selected something for uploading
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE__SELECT_CONTENT_FROM_APPS &&
                (resultCode == RESULT_OK ||
                        resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)) {

            requestUploadOfContentFromApps(data, resultCode);

        } else if (requestCode == REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM &&
                (resultCode == RESULT_OK ||
                        resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE ||
                        resultCode == UploadFilesActivity.RESULT_OK_AND_DO_NOTHING ||
                        resultCode == UploadFilesActivity.RESULT_OK_AND_DELETE)) {

            requestUploadOfFilesFromFileSystem(data, resultCode);

        } else if (requestCode == REQUEST_CODE__UPLOAD_FROM_CAMERA &&
                (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_DELETE)) {

            new CheckAvailableSpaceTask(new CheckAvailableSpaceTask.CheckAvailableSpaceListener() {
                @Override
                public void onCheckAvailableSpaceStart() {
                    Log_OC.d(this, "onCheckAvailableSpaceStart");
                }

                @Override
                public void onCheckAvailableSpaceFinish(boolean hasEnoughSpaceAvailable, String... filesToUpload) {
                    Log_OC.d(this, "onCheckAvailableSpaceFinish");

                    if (hasEnoughSpaceAvailable) {
                        File file = new File(filesToUpload[0]);
                        File renamedFile = new File(file.getParent() + PATH_SEPARATOR + FileOperationsHelper.getCapturedImageName());

                        if (!file.renameTo(renamedFile)) {
                            DisplayUtils.showSnackMessage(getActivity(), "Fail to upload taken image!");
                            return;
                        }

                        requestUploadOfFilesFromFileSystem(new String[]{renamedFile.getAbsolutePath()},
                                                           FileUploader.LOCAL_BEHAVIOUR_DELETE);
                    }
                }
            }, new String[]{FileOperationsHelper.createImageFile(getActivity()).getAbsolutePath()}).execute();
        } else if (requestCode == REQUEST_CODE__MOVE_FILES && resultCode == RESULT_OK) {
            exitSelectionMode();
            final Intent fData = data;
            getHandler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            requestMoveOperation(fData);
                        }
                    },
                    DELAY_TO_REQUEST_OPERATIONS_LATER
            );

        } else if (requestCode == REQUEST_CODE__COPY_FILES && resultCode == RESULT_OK) {
            exitSelectionMode();
            final Intent fData = data;
            getHandler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            requestCopyOperation(fData);
                        }
                    },
                    DELAY_TO_REQUEST_OPERATIONS_LATER
            );
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void exitSelectionMode() {
        OCFileListFragment ocFileListFragment = getListOfFilesFragment();
        if (ocFileListFragment != null) {
            ocFileListFragment.exitSelectionMode();
        }
    }

    private void requestUploadOfFilesFromFileSystem(Intent data, int resultCode) {
        String[] filePaths = data.getStringArrayExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES);
        requestUploadOfFilesFromFileSystem(filePaths, resultCode);
    }

    private void requestUploadOfFilesFromFileSystem(String[] filePaths, int resultCode) {
        if (filePaths != null) {
            String[] remotePaths = new String[filePaths.length];
            String remotePathBase = getCurrentDir().getRemotePath();
            for (int j = 0; j < remotePaths.length; j++) {
                remotePaths[j] = remotePathBase + (new File(filePaths[j])).getName();
            }

            int behaviour;
            switch (resultCode) {
                case UploadFilesActivity.RESULT_OK_AND_MOVE:
                    behaviour = FileUploader.LOCAL_BEHAVIOUR_MOVE;
                    break;

                case UploadFilesActivity.RESULT_OK_AND_DELETE:
                    behaviour = FileUploader.LOCAL_BEHAVIOUR_DELETE;
                    break;

                case UploadFilesActivity.RESULT_OK_AND_DO_NOTHING:
                    behaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET;
                    break;

                default:
                    behaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET;
                    break;
            }

            FileUploader.uploadNewFile(
                this,
                getAccount(),
                filePaths,
                remotePaths,
                null,           // MIME type will be detected from file name
                behaviour,
                false,          // do not create parent folder if not existent
                UploadFileOperation.CREATED_BY_USER,
                false,
                false,
                FileUploader.NameCollisionPolicy.ASK_USER
            );

        } else {
            Log_OC.d(TAG, "User clicked on 'Update' with no selection");
            DisplayUtils.showSnackMessage(this, R.string.filedisplay_no_file_selected);
        }
    }

    private void requestUploadOfContentFromApps(Intent contentIntent, int resultCode) {

        ArrayList<Parcelable> streamsToUpload = new ArrayList<>();

        if (contentIntent.getClipData() != null && contentIntent.getClipData().getItemCount() > 0) {

            for (int i = 0; i < contentIntent.getClipData().getItemCount(); i++) {
                streamsToUpload.add(contentIntent.getClipData().getItemAt(i).getUri());
            }

        } else {
            streamsToUpload.add(contentIntent.getData());
        }

        int behaviour = (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE) ? FileUploader.LOCAL_BEHAVIOUR_MOVE :
                FileUploader.LOCAL_BEHAVIOUR_COPY;

        OCFile currentDir = getCurrentDir();
        String remotePath = (currentDir != null) ? currentDir.getRemotePath() : OCFile.ROOT_PATH;

        UriUploader uploader = new UriUploader(
                this,
                streamsToUpload,
                remotePath,
                getAccount(),
                behaviour,
                false, // Not show waiting dialog while file is being copied from private storage
                null  // Not needed copy temp task listener
        );

        uploader.uploadUris();

    }

    /**
     * Request the operation for moving the file/folder from one path to another
     *
     * @param data Intent received
     */
    private void requestMoveOperation(Intent data) {
        OCFile folderToMoveAt = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
        ArrayList<OCFile> files = data.getParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES);
        getFileOperationsHelper().moveFiles(files, folderToMoveAt);
    }

    /**
     * Request the operation for copying the file/folder from one path to another
     *
     * @param data Intent received
     */
    private void requestCopyOperation(Intent data) {
        OCFile folderToMoveAt = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
        ArrayList<OCFile> files = data.getParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES);
        getFileOperationsHelper().copyFiles(files, folderToMoveAt);
    }

    private boolean isSearchOpen() {
        if (searchView == null) {
            return false;
        } else {
            View mSearchEditFrame = searchView.findViewById(androidx.appcompat.R.id.search_edit_frame);
            return mSearchEditFrame != null && mSearchEditFrame.getVisibility() == View.VISIBLE;
        }
    }

    /*
     * BackPressed priority/hierarchy:
     *    1. close search view if opened
     *    2. close drawer if opened
     *    3. close FAB if open (only if drawer isn't open)
     *    4. navigate up (only if drawer and FAB aren't open)
     */
    @SuppressFBWarnings("ITC_INHERITANCE_TYPE_CHECKING")
    @Override
    public void onBackPressed() {
        boolean isDrawerOpen = isDrawerOpen();
        boolean isSearchOpen = isSearchOpen();

        Fragment leftFragment = getLeftFragment();

        if (leftFragment instanceof OCFileListFragment) {
            OCFileListFragment listOfFiles = (OCFileListFragment) leftFragment;

            if (isSearchOpen && searchView != null) {
                searchView.setQuery("", true);
                searchView.onActionViewCollapsed();
                searchView.clearFocus();

                // Remove the list to the original state
                listOfFiles.performSearch("", true);

                hideSearchView(getCurrentDir());

                setDrawerIndicatorEnabled(isDrawerIndicatorAvailable());
            } else if (isDrawerOpen) {
                // close drawer first
                super.onBackPressed();
            } else {
                // all closed
                if (mDualPane || getSecondFragment() == null) {
                    OCFile currentDir = getCurrentDir();
                    if (currentDir == null || currentDir.getParentId() == FileDataStorageManager.ROOT_PARENT_ID) {
                        finish();
                        return;
                    }
                    listOfFiles.onBrowseUp();
                }
                setFile(listOfFiles.getCurrentFile());
                listOfFiles.setFabVisible(true);
                listOfFiles.registerFabListener();
                showSortListGroup(true);
                cleanSecondFragment();
            }
        } else if (leftFragment instanceof PreviewTextStringFragment) {
            createMinFragments(null);
        } else {
            // pop back
            hideSearchView(getCurrentDir());
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        Log_OC.v(TAG, "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDisplayActivity.KEY_WAITING_TO_PREVIEW, mWaitingToPreview);
        outState.putBoolean(FileDisplayActivity.KEY_SYNC_IN_PROGRESS, mSyncInProgress);
        //outState.putBoolean(FileDisplayActivity.KEY_REFRESH_SHARES_IN_PROGRESS,
        // mRefreshSharesInProgress);
        outState.putParcelable(FileDisplayActivity.KEY_WAITING_TO_SEND, mWaitingToSend);
        if (searchView != null) {
            outState.putBoolean(KEY_IS_SEARCH_OPEN, !searchView.isIconified());
        }
        outState.putString(KEY_SEARCH_QUERY, searchQuery);

        Log_OC.v(TAG, "onSaveInstanceState() end");
    }

    @Override
    protected void onResume() {
        Log_OC.v(TAG, "onResume() start");
        super.onResume();
        // Instead of onPostCreate, starting the loading in onResume for children fragments
        Fragment leftFragment = getLeftFragment();

        // Listen for sync messages
        if (!(leftFragment instanceof OCFileListFragment) || !((OCFileListFragment) leftFragment).isSearchFragment()) {
            IntentFilter syncIntentFilter = new IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START);
            syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_END);
            syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED);
            syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED);
            syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED);
            mSyncBroadcastReceiver = new SyncBroadcastReceiver();
            localBroadcastManager.registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);
        }

        if (!(leftFragment instanceof OCFileListFragment)) {
            return;
        }

        OCFileListFragment ocFileListFragment = (OCFileListFragment) leftFragment;

        ocFileListFragment.setLoading(mSyncInProgress);
        syncAndUpdateFolder(false);

        OCFile startFile = null;
        if (getIntent() != null && getIntent().getParcelableExtra(EXTRA_FILE) != null) {
            startFile = getIntent().getParcelableExtra(EXTRA_FILE);
            setFile(startFile);
        }

        // refresh list of files
        if (searchView != null && !TextUtils.isEmpty(searchQuery)) {
            searchView.setQuery(searchQuery, false);
        } else if (!ocFileListFragment.isSearchFragment() && startFile == null) {
            updateListOfFilesFragment(false);
            ocFileListFragment.registerFabListener();
        } else {
            ocFileListFragment.listDirectory(startFile, false, false);
            updateActionBarTitleAndHomeButton(startFile);
        }

        // Listen for upload messages
        IntentFilter uploadIntentFilter = new IntentFilter(FileUploader.getUploadFinishMessage());
        mUploadFinishReceiver = new UploadFinishReceiver();
        localBroadcastManager.registerReceiver(mUploadFinishReceiver, uploadIntentFilter);

        // Listen for download messages
        IntentFilter downloadIntentFilter = new IntentFilter(FileDownloader.getDownloadAddedMessage());
        downloadIntentFilter.addAction(FileDownloader.getDownloadFinishMessage());
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        localBroadcastManager.registerReceiver(mDownloadFinishReceiver, downloadIntentFilter);

        // setup drawer
        menuItemId = getIntent().getIntExtra(FileDisplayActivity.DRAWER_MENU_ID, menuItemId);

        if (menuItemId == -1) {
            if (MainApp.isOnlyOnDevice()) {
                setDrawerMenuItemChecked(R.id.nav_on_device);
                setupToolbar();
            } else {
                setDrawerMenuItemChecked(R.id.nav_all_files);
                setupHomeSearchToolbarWithSortAndListButtons();
            }
        } else {
            if (menuItemId == R.id.nav_all_files) {
                setupHomeSearchToolbarWithSortAndListButtons();
            } else {
                setupToolbar();
            }
            setDrawerMenuItemChecked(menuItemId);
        }

        if (ocFileListFragment instanceof GalleryFragment) {
            updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_gallery));
        }

        Log_OC.v(TAG, "onResume() end");
    }


    @Override
    protected void onPause() {
        Log_OC.v(TAG, "onPause() start");
        if (mSyncBroadcastReceiver != null) {
            localBroadcastManager.unregisterReceiver(mSyncBroadcastReceiver);
            mSyncBroadcastReceiver = null;
        }
        if (mUploadFinishReceiver != null) {
            localBroadcastManager.unregisterReceiver(mUploadFinishReceiver);
            mUploadFinishReceiver = null;
        }
        if (mDownloadFinishReceiver != null) {
            localBroadcastManager.unregisterReceiver(mDownloadFinishReceiver);
            mDownloadFinishReceiver = null;
        }

        super.onPause();
        Log_OC.v(TAG, "onPause() end");
    }

    @Override
    public void onSortingOrderChosen(FileSortOrder selection) {
        OCFileListFragment ocFileListFragment = getListOfFilesFragment();
        if (ocFileListFragment != null) {
            ocFileListFragment.sortFiles(selection);
        }
    }

    @Override
    public void downloadFile(OCFile file, String packageName, String activityName) {
        startDownloadForSending(file, OCFileListFragment.DOWNLOAD_SEND, packageName, activityName);
    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {

        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String event = intent.getAction();
                Log_OC.d(TAG, "Received broadcast " + event);
                String accountName = intent.getStringExtra(FileSyncAdapter.EXTRA_ACCOUNT_NAME);

                String synchFolderRemotePath =
                        intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH);
                RemoteOperationResult synchResult = (RemoteOperationResult)
                        DataHolderUtil.getInstance().retrieve(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT));
                boolean sameAccount = getAccount() != null &&
                        accountName.equals(getAccount().name) && getStorageManager() != null;

                if (sameAccount) {

                    if (FileSyncAdapter.EVENT_FULL_SYNC_START.equals(event)) {
                        mSyncInProgress = true;

                    } else {
                        OCFile currentFile = (getFile() == null) ? null :
                            getStorageManager().getFileByPath(getFile().getRemotePath());
                        OCFile currentDir = (getCurrentDir() == null) ? null :
                            getStorageManager().getFileByPath(getCurrentDir().getRemotePath());

                        if (currentDir == null) {
                            // current folder was removed from the server
                            DisplayUtils.showSnackMessage(
                                    getActivity(),
                                    R.string.sync_current_folder_was_removed,
                                    synchFolderRemotePath
                            );

                            browseToRoot();

                        } else {
                            if (currentFile == null && !getFile().isFolder()) {
                                // currently selected file was removed in the server, and now we
                                // know it
                                cleanSecondFragment();
                                currentFile = currentDir;
                            }

                            if (currentDir.getRemotePath().equals(synchFolderRemotePath)) {
                                OCFileListFragment fileListFragment = getListOfFilesFragment();
                                if (fileListFragment != null) {
                                    fileListFragment.listDirectory(currentDir, MainApp.isOnlyOnDevice(), false);
                                }
                            }
                            setFile(currentFile);
                        }

                        mSyncInProgress = !FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) &&
                                !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED.equals(event);

                        if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.equals(event) &&
                                synchResult != null) {

                            if (synchResult.isSuccess()) {
                                hideInfoBox();
                            } else {
                                // TODO refactor and make common
                                if (checkForRemoteOperationError(synchResult)) {
                                    requestCredentialsUpdate(context);
                                } else {
                                    switch (synchResult.getCode()) {
                                        case SSL_RECOVERABLE_PEER_UNVERIFIED:
                                            showUntrustedCertDialog(synchResult);
                                            break;

                                        case MAINTENANCE_MODE:
                                            showInfoBox(R.string.maintenance_mode);
                                            break;

                                        case NO_NETWORK_CONNECTION:
                                            showInfoBox(R.string.offline_mode);
                                            break;

                                        case HOST_NOT_AVAILABLE:
                                            showInfoBox(R.string.host_not_available);
                                            break;

                                        default:
                                            // nothing to do
                                            break;
                                    }
                                }
                            }
                        }
                        DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT));

                        Log_OC.d(TAG, "Setting progress visibility to " + mSyncInProgress);
                        OCFileListFragment ocFileListFragment = getListOfFilesFragment();
                        if (ocFileListFragment != null) {
                            ocFileListFragment.setLoading(mSyncInProgress);
                        }
                        setBackgroundText();
                    }
                }

                if (synchResult != null && synchResult.getCode().equals(
                        RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED)) {
                    mLastSslUntrustedServerResult = synchResult;
                }
            } catch (RuntimeException e) {
                // avoid app crashes after changing the serial id of RemoteOperationResult
                // in owncloud library with broadcast notifications pending to process

                try {
                    DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT));
                } catch (RuntimeException re) {
                    // we did not send this intent, so ignoring
                    Log_OC.i(TAG, "Ignoring error deleting data");
                }
            }
        }
    }

    private boolean checkForRemoteOperationError(RemoteOperationResult syncResult) {
        return ResultCode.UNAUTHORIZED.equals(syncResult.getCode()) ||
                (syncResult.isException() && syncResult.getException()
                        instanceof AuthenticatorException);
    }

    /**
     * Show a text message on screen view for notifying user if content is
     * loading or folder is empty
     */
    private void setBackgroundText() {
        final OCFileListFragment ocFileListFragment = getListOfFilesFragment();
        if (ocFileListFragment != null) {
            if (mSyncInProgress ||
                getFile().getFileLength() > 0 && getStorageManager().getFolderContent(getFile(), false).isEmpty()) {
                ocFileListFragment.setEmptyListLoadingMessage();
            } else {
                if (MainApp.isOnlyOnDevice()) {
                    ocFileListFragment.setMessageForEmptyList(R.string.file_list_empty_headline,
                                                              R.string.file_list_empty_on_device,
                                                              R.drawable.ic_list_empty_folder,
                                                              true);
                } else {
                    ocFileListFragment.setEmptyListMessage(ExtendedListFragment.SearchType.NO_SEARCH);
                }
            }
        } else {
            Log_OC.e(TAG, "OCFileListFragment is null");
        }
    }

    /**
     * Once the file upload has finished -> update view
     */
    private class UploadFinishReceiver extends BroadcastReceiver {
        /**
         * Once the file upload has finished -> update view
         *
         * {@link BroadcastReceiver} to enable upload feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String uploadedRemotePath = intent.getStringExtra(FileUploader.EXTRA_REMOTE_PATH);
            String accountName = intent.getStringExtra(FileUploader.ACCOUNT_NAME);
            boolean sameAccount = getAccount() != null && accountName.equals(getAccount().name);
            OCFile currentDir = getCurrentDir();
            boolean isDescendant = currentDir != null && uploadedRemotePath != null &&
                uploadedRemotePath.startsWith(currentDir.getRemotePath());

            if (sameAccount && isDescendant) {
                String linkedToRemotePath =
                    intent.getStringExtra(FileUploader.EXTRA_LINKED_TO_PATH);
                if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                    updateListOfFilesFragment(false);
                }
            }

            boolean uploadWasFine = intent.getBooleanExtra(
                FileUploader.EXTRA_UPLOAD_RESULT,
                false);
            boolean renamedInUpload = getFile().getRemotePath().
                equals(intent.getStringExtra(FileUploader.EXTRA_OLD_REMOTE_PATH));

            boolean sameFile = getFile().getRemotePath().equals(uploadedRemotePath) ||
                renamedInUpload;
            FileFragment details = getSecondFragment();

            if (sameAccount && sameFile && details instanceof FileDetailFragment) {
                if (uploadWasFine) {
                    setFile(getStorageManager().getFileByPath(uploadedRemotePath));
                } else {
                    //TODO remove upload progress bar after upload failed.
                    Log_OC.d(TAG, "Remove upload progress bar after upload failed");
                }
                if (renamedInUpload) {
                    String newName = new File(uploadedRemotePath).getName();
                    DisplayUtils.showSnackMessage(
                        getActivity(),
                        R.string.filedetails_renamed_in_upload_msg,
                        newName
                    );
                }
                if (uploadWasFine || getFile().fileExists()) {
                    ((FileDetailFragment) details).updateFileDetails(false, true);
                } else {
                    cleanSecondFragment();
                }

                // Force the preview if the file is an image or text file
                if (uploadWasFine) {
                    OCFile ocFile = getFile();
                    if (PreviewImageFragment.canBePreviewed(ocFile)) {
                        startImagePreview(getFile(), true);
                    } else if (PreviewTextFileFragment.canBePreviewed(ocFile)) {
                        startTextPreview(ocFile, true);
                    }
                    // TODO what about other kind of previews?
                }
            }
            OCFileListFragment ocFileListFragment = getListOfFilesFragment();
            if (ocFileListFragment != null) {
                ocFileListFragment.setLoading(false);
            }
        }

        // TODO refactor this receiver, and maybe DownloadFinishReceiver; this method is duplicated :S
        private boolean isAscendant(String linkedToRemotePath) {
            OCFile currentDir = getCurrentDir();
            return currentDir != null && currentDir.getRemotePath().startsWith(linkedToRemotePath);
        }
    }


    /**
     * Class waiting for broadcast events from the {@link FileDownloader} service.
     *
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * current folder.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            boolean sameAccount = isSameAccount(intent);
            String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
            String downloadBehaviour = intent.getStringExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR);
            boolean isDescendant = isDescendant(downloadedRemotePath);

                if (sameAccount && isDescendant) {
                    String linkedToRemotePath = intent.getStringExtra(FileDownloader.EXTRA_LINKED_TO_PATH);
                    if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                        updateListOfFilesFragment(false);
                    }
                    refreshSecondFragment(
                            intent.getAction(),
                            downloadedRemotePath,
                            intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false));
                }

            if (mWaitingToSend != null) {
                // update file after downloading
                mWaitingToSend = getStorageManager().getFileByRemoteId(mWaitingToSend.getRemoteId());
                if (mWaitingToSend != null && mWaitingToSend.isDown() && downloadBehaviour != null) {
                    switch (downloadBehaviour) {
                        case OCFileListFragment.DOWNLOAD_SEND:
                            String packageName = intent.getStringExtra(SendShareDialog.PACKAGE_NAME);
                            String activityName = intent.getStringExtra(SendShareDialog.ACTIVITY_NAME);

                            sendDownloadedFile(packageName, activityName);
                            break;
                        default:
                            // do nothing
                            break;
                    }
                }
            }
        }

        private boolean isDescendant(String downloadedRemotePath) {
            OCFile currentDir = getCurrentDir();
            return currentDir != null &&
                    downloadedRemotePath != null &&
                downloadedRemotePath.startsWith(currentDir.getRemotePath());
        }

        private boolean isAscendant(String linkedToRemotePath) {
            OCFile currentDir = getCurrentDir();
            return currentDir != null &&
                currentDir.getRemotePath().startsWith(linkedToRemotePath);
        }

        private boolean isSameAccount(Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);
            return accountName != null && getAccount() != null && accountName.equals(getAccount().name);
        }
    }


    public void browseToRoot() {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            listOfFiles.listDirectory(root, MainApp.isOnlyOnDevice(), false);
            setFile(listOfFiles.getCurrentFile());
            startSyncFolderOperation(root, false);
        }
        cleanSecondFragment();
    }


    /**
     * {@inheritDoc}
     * Updates action bar and second fragment, if in dual pane mode.
     */
    @Override
    public void onBrowsedDownTo(OCFile directory) {
        setFile(directory);
        cleanSecondFragment();
        // Sync Folder
        startSyncFolderOperation(directory, false);
    }

    /**
     * Shows the information of the {@link OCFile} received as a
     * parameter in the second fragment.
     *
     * @param file {@link OCFile} whose details will be shown
     */
    @Override
    public void showDetails(OCFile file) {
        showDetails(file, 0);
    }

    /**
     * Shows the information of the {@link OCFile} received as a
     * parameter in the second fragment.
     *
     * @param file {@link OCFile} whose details will be shown
     * @param activeTab the active tab in the details view
     */
    public void showDetails(OCFile file, int activeTab) {
        User currentUser = getUser().orElseThrow(RuntimeException::new);
        Fragment detailFragment = FileDetailFragment.newInstance(file, currentUser, activeTab);
        setLeftFragment(detailFragment);

        updateActionBarTitleAndHomeButton(file);
        mDrawerToggle.setDrawerIndicatorEnabled(false);
    }

    private void resetHeaderScrollingState() {
        AppBarLayout appBarLayout = findViewById(R.id.appbar);

        if (appBarLayout != null) {
            appBarLayout.setExpanded(true);
        }
    }

    @Override
    @Deprecated // in future no dual pane
    public void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
        if (chosenFile == null) {
            chosenFile = getFile();     // if no file is passed, current file decides
        }
        if (mDualPane) {
            // in dual pane mode, keep the focus of title an action bar in the current folder
            super.updateActionBarTitleAndHomeButton(getCurrentDir());

        } else {
            super.updateActionBarTitleAndHomeButton(chosenFile);
        }
    }

    @Override
    public boolean isDrawerIndicatorAvailable() {
        return isRoot(getCurrentDir());
    }

    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new ListServiceConnection();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private class ListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(new ComponentName(FileDisplayActivity.this, FileDownloader.class))) {
                Log_OC.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mWaitingToPreview != null && getStorageManager() != null) {
                    // update the file
                    mWaitingToPreview = getStorageManager().getFileById(mWaitingToPreview.getFileId());
                    if (mWaitingToPreview != null && !mWaitingToPreview.isDown()) {
                        requestForDownload();
                    }
                }
            } else if (component.equals(new ComponentName(FileDisplayActivity.this,
                    FileUploader.class))) {
                Log_OC.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            // a new chance to get the mDownloadBinder through
            // getFileDownloadBinder() - THIS IS A MESS
            OCFileListFragment listOfFiles = getListOfFilesFragment();
            if (listOfFiles != null && (getIntent() == null ||
                    (getIntent() != null && getIntent().getParcelableExtra(EXTRA_FILE) == null))) {
                listOfFiles.listDirectory(MainApp.isOnlyOnDevice(), false);
            }
            FileFragment secondFragment = getSecondFragment();
            if (secondFragment instanceof FileDetailFragment) {
                FileDetailFragment detailFragment = (FileDetailFragment) secondFragment;
                detailFragment.listenForTransferProgress();
                detailFragment.updateFileDetails(false, false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(FileDisplayActivity.this, FileDownloader.class))) {
                Log_OC.d(TAG, "Download service disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(FileDisplayActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service disconnected");
                mUploaderBinder = null;
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files
     * in the current account.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (operation instanceof RemoveFileOperation) {
            onRemoveFileOperationFinish((RemoveFileOperation) operation, result);
        } else if (operation instanceof RenameFileOperation) {
            onRenameFileOperationFinish((RenameFileOperation) operation, result);
        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation) operation, result);
        } else if (operation instanceof CreateFolderOperation) {
            onCreateFolderOperationFinish((CreateFolderOperation) operation, result);
        } else if (operation instanceof MoveFileOperation) {
            onMoveFileOperationFinish((MoveFileOperation) operation, result);
        } else if (operation instanceof CopyFileOperation) {
            onCopyFileOperationFinish((CopyFileOperation) operation, result);
        } else if (operation instanceof RestoreFileVersionRemoteOperation) {
            onRestoreFileVersionOperationFinish(result);
        }
    }

    private void refreshShowDetails() {
        FileFragment details = getSecondFragment();
        if (details != null) {
            OCFile file = details.getFile();
            if (file != null) {
                file = getStorageManager().getFileByPath(file.getRemotePath());
                if (details instanceof PreviewMediaFragment) {
                    // Refresh  OCFile of the fragment
                    ((PreviewMediaFragment) details).updateFile(file);
                } else if (details instanceof PreviewTextFragment) {
                    // Refresh  OCFile of the fragment
                    ((PreviewTextFileFragment) details).updateFile(file);
                } else {
                    showDetails(file);
                }
            }
            supportInvalidateOptionsMenu();
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to
     * remove a file.
     *
     * @param operation Removal operation performed.
     * @param result    Result of the removal.
     */
    private void onRemoveFileOperationFinish(RemoveFileOperation operation,
                                             RemoteOperationResult result) {

        if (!operation.isInBackground()) {
            DisplayUtils.showSnackMessage(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation,
                    getResources()));
        }

        if (result.isSuccess()) {
            OCFile removedFile = operation.getFile();
            tryStopPlaying(removedFile);
            FileFragment second = getSecondFragment();

            // check if file is still available, if so do nothing
            boolean fileAvailable = getStorageManager().fileExists(removedFile.getFileId());

            if (second != null && !fileAvailable && removedFile.equals(second.getFile())) {
                if (second instanceof PreviewMediaFragment) {
                    ((PreviewMediaFragment) second).stopPreview(true);
                }
                setFile(getStorageManager().getFileById(removedFile.getParentId()));
                cleanSecondFragment();
            }
            OCFile parentFile = getStorageManager().getFileById(removedFile.getParentId());
            if (parentFile != null && parentFile.equals(getCurrentDir())) {
                updateListOfFilesFragment(false);
            }
            supportInvalidateOptionsMenu();
        } else {
            if (result.isSslRecoverableException()) {
                mLastSslUntrustedServerResult = result;
                showUntrustedCertDialog(mLastSslUntrustedServerResult);
            }
        }
    }

    private void onRestoreFileVersionOperationFinish(RemoteOperationResult result) {
        if (result.isSuccess()) {
            OCFile file = getFile();

            // delete old local copy
            if (file.isDown()) {
                List<OCFile> list = new ArrayList<>();
                list.add(file);
                getFileOperationsHelper().removeFiles(list, true, true);

                // download new version, only if file was previously download
                getFileOperationsHelper().syncFile(file);
            }

            OCFile parent = getStorageManager().getFileById(file.getParentId());
            startSyncFolderOperation(parent, true, true);

            if (getSecondFragment() instanceof FileDetailFragment) {
                FileDetailFragment fileDetailFragment = (FileDetailFragment) getSecondFragment();
                fileDetailFragment.getFileDetailActivitiesFragment().reload();
            }

            DisplayUtils.showSnackMessage(this, R.string.file_version_restored_successfully);
        } else {
            DisplayUtils.showSnackMessage(this, R.string.file_version_restored_error);
        }
    }

    private void tryStopPlaying(OCFile file) {
        // placeholder for stop-on-delete future code
        if (mPlayerConnection != null && MimeTypeUtil.isAudio(file) && mPlayerConnection.isPlaying()) {
            mPlayerConnection.stop(file);
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to move a
     * file.
     *
     * @param operation Move operation performed.
     * @param result    Result of the move operation.
     */
    private void onMoveFileOperationFinish(MoveFileOperation operation,
                                           RemoteOperationResult result) {
        if (result.isSuccess()) {
            syncAndUpdateFolder(true);
        } else {
            try {
                DisplayUtils.showSnackMessage(
                        this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
                );

            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e);
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to copy a
     * file.
     *
     * @param operation Copy operation performed.
     * @param result    Result of the copy operation.
     */
    private void onCopyFileOperationFinish(CopyFileOperation operation, RemoteOperationResult result) {
        if (result.isSuccess()) {
            updateListOfFilesFragment(false);
        } else {
            try {
                DisplayUtils.showSnackMessage(
                        this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
                );

            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e);
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying to rename
     * a file.
     *
     * @param operation Renaming operation performed.
     * @param result    Result of the renaming.
     */
    private void onRenameFileOperationFinish(RenameFileOperation operation,
                                             RemoteOperationResult result) {
        Optional<User> optionalUser = getUser();
        OCFile renamedFile = operation.getFile();
        if (result.isSuccess() && optionalUser.isPresent()) {
            final User currentUser = optionalUser.get();
            FileFragment details = getSecondFragment();
            if (details != null) {
                if (details instanceof FileDetailFragment &&
                        renamedFile.equals(details.getFile())) {
                    ((FileDetailFragment) details).updateFileDetails(renamedFile, currentUser);
                    showDetails(renamedFile);

                } else if (details instanceof PreviewMediaFragment &&
                        renamedFile.equals(details.getFile())) {
                    ((PreviewMediaFragment) details).updateFile(renamedFile);
                    if (PreviewMediaFragment.canBePreviewed(renamedFile)) {
                        int position = ((PreviewMediaFragment) details).getPosition();
                        startMediaPreview(renamedFile, position, true, true, true);
                    } else {
                        getFileOperationsHelper().openFile(renamedFile);
                    }
                } else if (details instanceof PreviewTextFragment &&
                    renamedFile.equals(details.getFile())) {
                    ((PreviewTextFileFragment) details).updateFile(renamedFile);
                    if (PreviewTextFileFragment.canBePreviewed(renamedFile)) {
                        startTextPreview(renamedFile, true);
                    } else {
                        getFileOperationsHelper().openFile(renamedFile);
                    }
                }
            }

            OCFile file = getStorageManager().getFileById(renamedFile.getParentId());
            if (file != null && file.equals(getCurrentDir())) {
                updateListOfFilesFragment(false);
            }

        } else {
            DisplayUtils.showSnackMessage(
                    this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources())
            );

            if (result.isSslRecoverableException()) {
                mLastSslUntrustedServerResult = result;
                showUntrustedCertDialog(mLastSslUntrustedServerResult);
            }
        }
    }


    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation,
                                                  RemoteOperationResult result) {
        if (result.isSuccess() && operation.transferWasRequested()) {
            OCFile syncedFile = operation.getLocalFile();
            onTransferStateChanged(syncedFile, true, true);
            supportInvalidateOptionsMenu();
            refreshShowDetails();
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying create a
     * new folder
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private void onCreateFolderOperationFinish(CreateFolderOperation operation,
                                               RemoteOperationResult result) {
        if (result.isSuccess()) {
            OCFileListFragment fileListFragment = getListOfFilesFragment();
            if (fileListFragment != null) {
                fileListFragment.onItemClicked(getStorageManager().getFileByPath(operation.getRemotePath()));
            }
        } else {
            try {
                if (ResultCode.FOLDER_ALREADY_EXISTS == result.getCode()) {
                    DisplayUtils.showSnackMessage(this, R.string.folder_already_exists);
                } else {
                    DisplayUtils.showSnackMessage(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation,
                            getResources()));
                }
            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message ", e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading) {
        updateListOfFilesFragment(false);
        FileFragment details = getSecondFragment();
        Optional<User> optionalUser = getUser();
        if (details instanceof FileDetailFragment && file.equals(details.getFile()) && optionalUser.isPresent()) {
            final User currentUser = optionalUser.get();
            if (downloading || uploading) {
                ((FileDetailFragment) details).updateFileDetails(file, currentUser);
            } else {
                if (!file.fileExists()) {
                    cleanSecondFragment();
                } else {
                    ((FileDetailFragment) details).updateFileDetails(false, true);
                }
            }
        }

    }


    private void requestForDownload() {
        User user = getUser().orElseThrow(RuntimeException::new);
        //if (!mWaitingToPreview.isDownloading()) {
        if (!mDownloaderBinder.isDownloading(user, mWaitingToPreview)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_USER, user);
            i.putExtra(FileDownloader.EXTRA_FILE, mWaitingToPreview);
            startService(i);
        }
    }

    @Override
    public void onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir(), false);
    }

    /**
     * Starts an operation to refresh the requested folder.
     *
     * The operation is run in a new background thread created on the fly.
     *
     * The refresh updates is a "light sync": properties of regular files in folder are updated (including
     * associated shares), but not their contents. Only the contents of files marked to be kept-in-sync are
     * synchronized too.
     *
     * @param folder     Folder to refresh.
     * @param ignoreETag If 'true', the data from the server will be fetched and sync'ed even if the eTag
     *                   didn't change.
     */
    public void startSyncFolderOperation(OCFile folder, boolean ignoreETag) {
        startSyncFolderOperation(folder, ignoreETag, false);
    }

    /**
     * Starts an operation to refresh the requested folder.
     *
     * The operation is run in a new background thread created on the fly.
     *
     * The refresh updates is a "light sync": properties of regular files in folder are updated (including
     * associated shares), but not their contents. Only the contents of files marked to be kept-in-sync are
     * synchronized too.
     *
     * @param folder      Folder to refresh.
     * @param ignoreETag  If 'true', the data from the server will be fetched and sync'ed even if the eTag
     *                    didn't change.
     * @param ignoreFocus reloads file list even without focus, e.g. on tablet mode, focus can still be in detail view
     */
    public void startSyncFolderOperation(final OCFile folder, final boolean ignoreETag, boolean ignoreFocus) {

        // the execution is slightly delayed to allow the activity get the window focus if it's being started
        // or if the method is called from a dialog that is being dismissed
        if (TextUtils.isEmpty(searchQuery)) {
            getHandler().postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            if (ignoreFocus || hasWindowFocus()) {
                                long currentSyncTime = System.currentTimeMillis();
                                mSyncInProgress = true;

                                // perform folder synchronization
                                RemoteOperation synchFolderOp = new RefreshFolderOperation(folder,
                                        currentSyncTime,
                                        false,
                                        ignoreETag,
                                        getStorageManager(),
                                        getAccount(),
                                        getApplicationContext()
                                );
                                synchFolderOp.execute(
                                        getAccount(),
                                        MainApp.getAppContext(),
                                        FileDisplayActivity.this,
                                        null,
                                        null
                                );

                                OCFileListFragment fragment = getListOfFilesFragment();

                                if (fragment != null) {
                                    fragment.setLoading(true);
                                }

                                setBackgroundText();

                            }   // else: NOTHING ; lets' not refresh when the user rotates the device but there is
                            // another window floating over
                        }
                    },
                    DELAY_TO_REQUEST_REFRESH_OPERATION_LATER
            );
        }
    }

    private void requestForDownload(OCFile file, String downloadBehaviour, String packageName, String activityName) {
        final User currentUser = getUser().orElseThrow(RuntimeException::new);
        if (!mDownloaderBinder.isDownloading(currentUser, mWaitingToPreview)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_USER, currentUser);
            i.putExtra(FileDownloader.EXTRA_FILE, file);
            i.putExtra(SendShareDialog.PACKAGE_NAME, packageName);
            i.putExtra(SendShareDialog.ACTIVITY_NAME, activityName);
            i.putExtra(OCFileListFragment.DOWNLOAD_BEHAVIOUR, downloadBehaviour);
            startService(i);
        }
    }

    private void sendDownloadedFile(String packageName, String activityName) {
        if (mWaitingToSend != null) {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType(mWaitingToSend.getMimeType());
            sendIntent.putExtra(Intent.EXTRA_STREAM, mWaitingToSend.getExposedFileUri(this));
            sendIntent.putExtra(Intent.ACTION_SEND, true);

            sendIntent.setComponent(new ComponentName(packageName, activityName));

            // Show dialog
            String sendTitle = getString(R.string.activity_chooser_send_file_title);
            startActivity(Intent.createChooser(sendIntent, sendTitle));
        } else {
            Log_OC.e(TAG, "Trying to send a NULL OCFile");
        }

        mWaitingToSend = null;
    }

    /**
     * Requests the download of the received {@link OCFile} , updates the UI
     * to monitor the download progress and prepares the activity to send the file
     * when the download finishes.
     *
     * @param file {@link OCFile} to download and preview.
     * @param packageName
     * @param activityName
     */
    public void startDownloadForSending(OCFile file, String downloadBehaviour, String packageName,
                                        String activityName) {
        mWaitingToSend = file;
        requestForDownload(mWaitingToSend, downloadBehaviour, packageName, activityName);
        boolean hasSecondFragment = getSecondFragment() != null;
        updateFragmentsVisibility(hasSecondFragment);
    }

    /**
     * Opens the image gallery showing the image {@link OCFile} received as parameter.
     *
     * @param file Image {@link OCFile} to show.
     */
    public void startImagePreview(OCFile file, boolean showPreview) {
        Intent showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        showDetailsIntent.putExtra(EXTRA_FILE, file);
        showDetailsIntent.putExtra(EXTRA_ACCOUNT, getAccount());
        if (showPreview) {
            startActivity(showDetailsIntent);
        } else {
            FileOperationsHelper fileOperationsHelper = new FileOperationsHelper(this,
                                                                                 getUserAccountManager(),
                                                                                 connectivityService);
            fileOperationsHelper.startSyncForFileAndIntent(file, showDetailsIntent);
        }
    }

    /**
     * Opens the image gallery showing the image {@link OCFile} received as parameter.
     *
     * @param file Image {@link OCFile} to show.
     */
    public void startImagePreview(OCFile file, VirtualFolderType type, boolean showPreview) {
        Intent showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        showDetailsIntent.putExtra(PreviewImageActivity.EXTRA_FILE, file);
        showDetailsIntent.putExtra(EXTRA_ACCOUNT, getAccount());
        showDetailsIntent.putExtra(PreviewImageActivity.EXTRA_VIRTUAL_TYPE, type);

        if (showPreview) {
            startActivity(showDetailsIntent);
        } else {
            FileOperationsHelper fileOperationsHelper = new FileOperationsHelper(this,
                                                                                 getUserAccountManager(),
                                                                                 connectivityService);
            fileOperationsHelper.startSyncForFileAndIntent(file, showDetailsIntent);
        }
    }

    /**
     * Stars the preview of an already down media {@link OCFile}.
     *
     * @param file                  Media {@link OCFile} to preview.
     * @param startPlaybackPosition Media position where the playback will be started,
     *                              in milliseconds.
     * @param autoplay              When 'true', the playback will start without user
     *                              interactions.
     */
    public void startMediaPreview(OCFile file, int startPlaybackPosition, boolean autoplay, boolean showPreview,
                                  boolean streamMedia) {
        Optional<User> user = getUser();
        if (!user.isPresent()) {
            return; // not reachable under normal conditions
        }
        if (showPreview && file.isDown() && !file.isDownloading() || streamMedia) {
            showSortListGroup(false);
            CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) findViewById(R.id.root_layout).getLayoutParams();
            params.setBehavior(null);

            Fragment mediaFragment = PreviewMediaFragment.newInstance(file, user.get(), startPlaybackPosition, autoplay);
            setLeftFragment(mediaFragment);
            //updateFragmentsVisibility(true);
            updateActionBarTitleAndHomeButton(file);
            setFile(file);
        } else {
            Intent previewIntent = new Intent();
            previewIntent.putExtra(EXTRA_FILE, file);
            previewIntent.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, startPlaybackPosition);
            previewIntent.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, autoplay);
            FileOperationsHelper fileOperationsHelper = new FileOperationsHelper(this,
                                                                                 getUserAccountManager(),
                                                                                 connectivityService);
            fileOperationsHelper.startSyncForFileAndIntent(file, previewIntent);
        }
    }

    /**
     * Stars the preview of a text file {@link OCFile}.
     *
     * @param file Text {@link OCFile} to preview.
     */
    public void startTextPreview(OCFile file, boolean showPreview) {
        if (showPreview) {
            showSortListGroup(false);
            Bundle args = new Bundle();
            args.putParcelable(EXTRA_FILE, file);
            args.putParcelable(EXTRA_ACCOUNT, getAccount());
            args.putBoolean(EXTRA_SEARCH, searchOpen);
            args.putString(EXTRA_SEARCH_QUERY, searchQuery);
            Fragment textPreviewFragment = Fragment.instantiate(getApplicationContext(),
                                                                PreviewTextFileFragment.class.getName(), args);
            setLeftFragment(textPreviewFragment);
            binding.rightFragmentContainer.setVisibility(View.GONE);
            super.updateActionBarTitleAndHomeButton(file);
        } else {
            Intent previewIntent = new Intent();
            previewIntent.putExtra(EXTRA_FILE, file);
            previewIntent.putExtra(TEXT_PREVIEW, true);
            FileOperationsHelper fileOperationsHelper = new FileOperationsHelper(this,
                                                                                 getUserAccountManager(),
                                                                                 connectivityService);
            fileOperationsHelper.startSyncForFileAndIntent(file, previewIntent);
        }
    }

    /**
     * Starts rich workspace preview for a folder.
     *
     * @param folder {@link OCFile} to preview its rich workspace.
     */
    public void startRichWorkspacePreview(OCFile folder) {
        showSortListGroup(false);
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_FILE, folder);
        Fragment textPreviewFragment = Fragment.instantiate(getApplicationContext(),
                                                            PreviewTextStringFragment.class.getName(),
                                                            args);
        setLeftFragment(textPreviewFragment);
        binding.rightFragmentContainer.setVisibility(View.GONE);
        super.updateActionBarTitleAndHomeButton(folder);
    }

    public void startContactListFragment(OCFile file) {
        final User user = getUser().orElseThrow(RuntimeException::new);
        ContactsPreferenceActivity.startActivityWithContactsFile(this, user, file);
    }

    /**
     * Requests the download of the received {@link OCFile} , updates the UI
     * to monitor the download progress and prepares the activity to preview
     * or open the file when the download finishes.
     *
     * @param file {@link OCFile} to download and preview.
     */
    public void startDownloadForPreview(OCFile file) {
        final User currentUser = getUser().orElseThrow(RuntimeException::new);
        Fragment detailFragment = FileDetailFragment.newInstance(file, currentUser);
        setSecondFragment(detailFragment);
        mWaitingToPreview = file;
        requestForDownload();
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }


    /**
     * Request stopping the upload/download operation in progress over the given {@link OCFile} file.
     *
     * @param file {@link OCFile} file which operation are wanted to be cancel
     */
    public void cancelTransference(OCFile file) {
        getFileOperationsHelper().cancelTransference(file);
        if (mWaitingToPreview != null &&
            mWaitingToPreview.getRemotePath().equals(file.getRemotePath())) {
            mWaitingToPreview = null;
        }
        if (mWaitingToSend != null &&
            mWaitingToSend.getRemotePath().equals(file.getRemotePath())) {
            mWaitingToSend = null;
        }
        onTransferStateChanged(file, false, false);
    }

    /**
     * Request stopping all upload/download operations in progress over the given {@link OCFile} files.
     *
     * @param files collection of {@link OCFile} files which operations are wanted to be cancel
     */
    public void cancelTransference(Collection<OCFile> files) {
        for (OCFile file : files) {
            cancelTransference(file);
        }
    }

    @Override
    public void onRefresh(boolean ignoreETag) {
        syncAndUpdateFolder(ignoreETag);
    }

    @Override
    public void onRefresh() {
        syncAndUpdateFolder(true);
    }

    private void syncAndUpdateFolder(boolean ignoreETag) {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null && !listOfFiles.isSearchFragment()) {
            OCFile folder = listOfFiles.getCurrentFile();
            if (folder != null) {
                startSyncFolderOperation(folder, ignoreETag);
            }
        }
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        if (onDeviceOnly) {
            updateActionBarTitleAndHomeButtonByString(getString(R.string.drawer_item_on_device));
        }
        OCFileListFragment ocFileListFragment = getListOfFilesFragment();
        if (ocFileListFragment != null) {
            ocFileListFragment.refreshDirectory();
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(final SearchEvent event) {
        if (SearchRemoteOperation.SearchType.PHOTO_SEARCH == event.searchType) {
            Log_OC.d(this, "Switch to photo search fragment");

            setLeftFragment(new GalleryFragment(true));
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SyncEventFinished event) {
        Bundle bundle = event.getIntent().getExtras();
        if (event.getIntent().getBooleanExtra(TEXT_PREVIEW, false)) {
            startTextPreview((OCFile) bundle.get(EXTRA_FILE), true);
        } else if (bundle.containsKey(PreviewVideoActivity.EXTRA_START_POSITION)) {
            startMediaPreview((OCFile) bundle.get(EXTRA_FILE),
                              (int) bundle.get(PreviewVideoActivity.EXTRA_START_POSITION),
                              (boolean) bundle.get(PreviewVideoActivity.EXTRA_AUTOPLAY), true, true);
        } else if (bundle.containsKey(PreviewImageActivity.EXTRA_VIRTUAL_TYPE)) {
            startImagePreview((OCFile)bundle.get(EXTRA_FILE),
                    (VirtualFolderType)bundle.get(PreviewImageActivity.EXTRA_VIRTUAL_TYPE),
                    true);
        } else {
            startImagePreview((OCFile)bundle.get(EXTRA_FILE),true);
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(TokenPushEvent event) {
        if (!preferences.isKeysReInitEnabled()) {
            PushUtils.reinitKeys(getUserAccountManager());
        } else {
            PushUtils.pushRegistrationToServer(getUserAccountManager(), preferences.getPushToken());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final Optional<User> optionalUser = getUser();
        final FileDataStorageManager storageManager = getStorageManager();
        if (optionalUser.isPresent() && storageManager != null) {
            /// Check whether the 'main' OCFile handled by the Activity is contained in the
            // current Account
            OCFile file = getFile();
            // get parent from path
            String parentPath = "";
            if (file != null) {
                if (file.isDown() && file.getLastSyncDateForProperties() == 0) {
                    // upload in progress - right now, files are not inserted in the local
                    // cache until the upload is successful get parent from path
                    parentPath = file.getRemotePath().substring(0,
                                                                file.getRemotePath().lastIndexOf(file.getFileName()));
                    if (storageManager.getFileByPath(parentPath) == null) {
                        file = null; // not able to know the directory where the file is uploading
                    }
                } else {
                    file = storageManager.getFileByPath(file.getRemotePath());
                    // currentDir = null if not in the current Account
                }
            }
            if (file == null) {
                // fall back to root folder
                file = storageManager.getFileByPath(OCFile.ROOT_PATH);  // never returns null
            }
            setFile(file);

            User user = optionalUser.get();
            setupDrawer();

            mSwitchAccountButton.setTag(user.getAccountName());
            DisplayUtils.setAvatar(user, this, getResources()
                                       .getDimension(R.dimen.nav_drawer_menu_avatar_radius), getResources(),
                                   mSwitchAccountButton, this);

            final String lastDisplayedAccountName = mLastDisplayedAccount != null ? mLastDisplayedAccount.name : null;
            final boolean accountChanged = !user.getAccountName().equals(lastDisplayedAccountName);
            if (accountChanged) {
                Log_OC.d(TAG, "Initializing Fragments in onAccountChanged..");
                initFragmentsWithFile(user, file);
                if (file.isFolder() && TextUtils.isEmpty(searchQuery)) {
                    startSyncFolderOperation(file, false);
                }
            } else {
                updateActionBarTitleAndHomeButton(file.isFolder() ? null : file);
            }
        }
        if (optionalUser.isPresent()) {
            mLastDisplayedAccount = optionalUser.get().toPlatformAccount();
        } else {
            mLastDisplayedAccount = null;
        }

        EventBus.getDefault().post(new TokenPushEvent());
        checkForNewDevVersionNecessary(getApplicationContext());
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        checkForNewDevVersionNecessary(getApplicationContext());
    }

    public void setSearchQuery(String query) {
        searchQuery = query;
    }

    private void handleOpenFileViaIntent(Intent intent) {
        showLoadingDialog(getString(R.string.retrieving_file));

        String userName = intent.getStringExtra(KEY_ACCOUNT);
        String fileId = intent.getStringExtra(KEY_FILE_ID);

        if (userName == null && fileId == null && intent.getData() != null) {
            openDeepLink(intent.getData());
        } else {
            Optional<User> optionalUser = userName == null ? getUser() : getUserAccountManager().getUser(userName);
            if (optionalUser.isPresent()) {
                openFile(optionalUser.get(), fileId);
            } else {
                dismissLoadingDialog();
                DisplayUtils.showSnackMessage(this, getString(R.string.associated_account_not_found));
            }
        }
    }

    private void openDeepLink(Uri uri) {
        DeepLinkHandler linkHandler = new DeepLinkHandler(getUserAccountManager());
        DeepLinkHandler.Match match = linkHandler.parseDeepLink(uri);
        if (match == null) {
            dismissLoadingDialog();
            DisplayUtils.showSnackMessage(this, getString(R.string.invalid_url));
        } else if (match.getUsers().isEmpty()) {
            dismissLoadingDialog();
            DisplayUtils.showSnackMessage(this, getString(R.string.associated_account_not_found));
        } else if (match.getUsers().size() == SINGLE_USER_SIZE) {
            openFile(match.getUsers().get(0), match.getFileId());
        } else {
            selectUserAndOpenFile(match.getUsers(), match.getFileId());
        }
    }

    private void selectUserAndOpenFile(List<User> users, String fileId) {
        final CharSequence[] userNames = new CharSequence[users.size()];
        for (int i = 0; i < userNames.length; i++) {
            userNames[i] = users.get(i).getAccountName();
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
            .setTitle(R.string.common_choose_account)
            .setItems(userNames, (dialog, which) -> {
                User user = users.get(which);
                openFile(user, fileId);
                showLoadingDialog(getString(R.string.retrieving_file));
            });
        final AlertDialog dialog = builder.create();
        dismissLoadingDialog();
        dialog.show();
    }

    private void openFile(User user, String fileId) {
        setUser(user);

        if (fileId == null) {
            dismissLoadingDialog();
            DisplayUtils.showSnackMessage(this, getString(R.string.error_retrieving_file));
            return;
        }

        FileDataStorageManager storageManager = getStorageManager();

        if (storageManager == null) {
            storageManager = new FileDataStorageManager(user.toPlatformAccount(), getContentResolver());
        }

        FetchRemoteFileTask fetchRemoteFileTask = new FetchRemoteFileTask(user.toPlatformAccount(),
                                                                          fileId,
                                                                          storageManager,
                                                                          this);
        fetchRemoteFileTask.execute();

    }
}
