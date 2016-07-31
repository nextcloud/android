/**
 *  ownCloud Android client application
 *
 *  @author Bartek Przybylski
 *  @author David A. Velasco
 *  Copyright (C) 2011  Bartek Przybylski
 *  Copyright (C) 2016 ownCloud Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2,
 *  as published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SyncRequest;
import android.content.pm.PackageManager;
import android.content.res.Resources.NotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CopyFileOperation;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.services.observer.FileObserverService;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.fragment.TaskRetainerFragment;
import com.owncloud.android.ui.helpers.UriUploader;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.ui.preview.PreviewVideoActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.PermissionUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import static com.owncloud.android.db.PreferenceManager.getSortOrder;

/**
 * Displays, what files the user has available in his ownCloud. This is the main view.
 */

public class FileDisplayActivity extends HookActivity
        implements FileFragment.ContainerActivity,
        OnEnforceableRefreshListener {

    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private UploadFinishReceiver mUploadFinishReceiver;
    private DownloadFinishReceiver mDownloadFinishReceiver;
    private RemoteOperationResult mLastSslUntrustedServerResult = null;

    private boolean mDualPane;
    private View mLeftFragmentContainer;
    private View mRightFragmentContainer;

    private static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    private static final String KEY_SYNC_IN_PROGRESS = "SYNC_IN_PROGRESS";
    private static final String KEY_WAITING_TO_SEND = "WAITING_TO_SEND";

    public static final String ACTION_DETAILS = "com.owncloud.android.ui.activity.action.DETAILS";

    public static final int REQUEST_CODE__SELECT_CONTENT_FROM_APPS = REQUEST_CODE__LAST_SHARED + 1;
    public static final int REQUEST_CODE__SELECT_FILES_FROM_FILE_SYSTEM = REQUEST_CODE__LAST_SHARED + 2;
    public static final int REQUEST_CODE__MOVE_FILES = REQUEST_CODE__LAST_SHARED + 3;
    public static final int REQUEST_CODE__COPY_FILES = REQUEST_CODE__LAST_SHARED + 4;

    private static final String TAG = FileDisplayActivity.class.getSimpleName();

    private static final String TAG_LIST_OF_FILES = "LIST_OF_FILES";
    private static final String TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT";

    private OCFile mWaitingToPreview;

    private boolean mSyncInProgress = false;

    private OCFile mWaitingToSend;

    private Collection<MenuItem> mDrawerMenuItemstoShowHideList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");

        super.onCreate(savedInstanceState); // this calls onAccountChanged() when ownCloud Account
        // is valid

        /// grant that FileObserverService is watching favorite files
        if (savedInstanceState == null) {
            Intent initObserversIntent = FileObserverService.makeInitIntent(this);
            startService(initObserversIntent);
        }

        /// Load of saved instance state
        if (savedInstanceState != null) {
            mWaitingToPreview = (OCFile) savedInstanceState.getParcelable(
                    FileDisplayActivity.KEY_WAITING_TO_PREVIEW);
            mSyncInProgress = savedInstanceState.getBoolean(KEY_SYNC_IN_PROGRESS);
            mWaitingToSend = (OCFile) savedInstanceState.getParcelable(
                    FileDisplayActivity.KEY_WAITING_TO_SEND);
        } else {
            mWaitingToPreview = null;
            mSyncInProgress = false;
            mWaitingToSend = null;
        }

        /// USER INTERFACE

        // Inflate and set the layout view
        setContentView(R.layout.files);

        // setup toolbar
        setupToolbar();

        // setup drawer
        if(MainApp.getOnlyOnDevice()) {
            setupDrawer(R.id.nav_on_device);
        } else {
            setupDrawer(R.id.nav_all_files);
        }

        mDualPane = getResources().getBoolean(R.bool.large_land_layout);
        mLeftFragmentContainer = findViewById(R.id.left_fragment_container);
        mRightFragmentContainer = findViewById(R.id.right_fragment_container);

        // Action bar setup
        getSupportActionBar().setHomeButtonEnabled(true);

        // Init Fragment without UI to retain AsyncTask across configuration changes
        FragmentManager fm = getSupportFragmentManager();
        TaskRetainerFragment taskRetainerFragment =
                (TaskRetainerFragment) fm.findFragmentByTag(TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT);
        if (taskRetainerFragment == null) {
            taskRetainerFragment = new TaskRetainerFragment();
            fm.beginTransaction()
                    .add(taskRetainerFragment, TaskRetainerFragment.FTAG_TASK_RETAINER_FRAGMENT).commit();
        }   // else, Fragment already created and retained across configuration change

        Log_OC.v(TAG, "onCreate() end");
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        if (PermissionUtil.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Check if we should show an explanation
            if (PermissionUtil.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show explanation to the user and then request permission
                Snackbar snackbar = Snackbar.make(findViewById(R.id.ListLayout), R.string.permission_storage_access,
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.common_ok, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                PermissionUtil.requestWriteExternalStoreagePermission(FileDisplayActivity.this);
                            }
                        });

                DisplayUtils.colorSnackbar(this, snackbar);

                snackbar.show();
            } else {
                // No explanation needed, request the permission.
                PermissionUtil.requestWriteExternalStoreagePermission(this);
            }
        }

        if (savedInstanceState == null) {
            createMinFragments();
        }

        refreshList(true);

        setIndeterminate(mSyncInProgress);
        // always AFTER setContentView(...) in onCreate(); to work around bug in its implementation

        setBackgroundText();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PermissionUtil.PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    startSynchronization();
                    // toggle on is save since this is the only scenario this code gets accessed
                } else {
                    // permission denied --> do nothing
                }
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        Log_OC.v(TAG, "onStart() start");
        super.onStart();
        Log_OC.v(TAG, "onStart() end");
    }

    @Override
    protected void onStop() {
        Log_OC.v(TAG, "onStop() start");
        super.onStop();
        Log_OC.v(TAG, "onStop() end");
    }

    @Override
    protected void onDestroy() {
        Log_OC.v(TAG, "onDestroy() start");
        super.onDestroy();
        Log_OC.v(TAG, "onDestroy() end");
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {
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
                    if (getStorageManager().getFileByPath(parentPath) == null)
                        file = null; // not able to know the directory where the file is uploading
                } else {
                    file = getStorageManager().getFileByPath(file.getRemotePath());
                    // currentDir = null if not in the current Account
                }
            }
            if (file == null) {
                // fall back to root folder
                file = getStorageManager().getFileByPath(OCFile.ROOT_PATH);  // never returns null
            }
            setFile(file);

            if (mAccountWasSet) {
                setAccountInDrawer(getAccount());
            }

            if (!stateWasRecovered) {
                Log_OC.d(TAG, "Initializing Fragments in onAccountChanged..");
                initFragmentsWithFile();
                if (file.isFolder()) {
                    startSyncFolderOperation(file, false);
                }

            } else {
                updateFragmentsVisibility(!file.isFolder());
                updateActionBarTitleAndHomeButton(file.isFolder() ? null : file);
            }
        }
    }

    private void createMinFragments() {
        OCFileListFragment listOfFiles = new OCFileListFragment();
        Bundle args = new Bundle();
        args.putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, true);
        listOfFiles.setArguments(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES);
        transaction.commit();
    }

    private void initFragmentsWithFile() {
        if (getAccount() != null && getFile() != null) {
            /// First fragment
            OCFileListFragment listOfFiles = getListOfFilesFragment();
            if (listOfFiles != null) {
                listOfFiles.listDirectory(getCurrentDir(), MainApp.getOnlyOnDevice());
            } else {
                Log_OC.e(TAG, "Still have a chance to lose the initializacion of list fragment >(");
            }

            /// Second fragment
            OCFile file = getFile();
            Fragment secondFragment = chooseInitialSecondFragment(file);
            if (secondFragment != null) {
                setSecondFragment(secondFragment);
                updateFragmentsVisibility(true);
                updateActionBarTitleAndHomeButton(file);
            } else {
                cleanSecondFragment();
                if (file.isDown() && PreviewTextFragment.canBePreviewed(file))
                    startTextPreview(file);
            }

        } else {
            Log_OC.e(TAG, "initFragments() called with invalid NULLs!");
            if (getAccount() == null) {
                Log_OC.e(TAG, "\t account is NULL");
            }
            if (getFile() == null) {
                Log_OC.e(TAG, "\t file is NULL");
            }
        }
    }

    private Fragment chooseInitialSecondFragment(OCFile file) {
        Fragment secondFragment = null;
        if (file != null && !file.isFolder()) {
            if (file.isDown() && PreviewMediaFragment.canBePreviewed(file)
                    && file.getLastSyncDateForProperties() > 0  // temporal fix
                    ) {
                int startPlaybackPosition =
                        getIntent().getIntExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0);
                boolean autoplay =
                        getIntent().getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, true);
                secondFragment = new PreviewMediaFragment(file, getAccount(),
                        startPlaybackPosition, autoplay);

            } else if (file.isDown() && PreviewTextFragment.canBePreviewed(file)) {
                secondFragment = null;
            } else {
                secondFragment = FileDetailFragment.newInstance(file, getAccount());
            }
        }
        return secondFragment;
    }


    /**
     * Replaces the second fragment managed by the activity with the received as
     * a parameter.
     * <p/>
     * Assumes never will be more than two fragments managed at the same time.
     *
     * @param fragment New second Fragment to set.
     */
    private void setSecondFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.right_fragment_container, fragment, TAG_SECOND_FRAGMENT);
        transaction.commit();
    }


    private void updateFragmentsVisibility(boolean existsSecondFragment) {
        if (mDualPane) {
            if (mLeftFragmentContainer.getVisibility() != View.VISIBLE) {
                mLeftFragmentContainer.setVisibility(View.VISIBLE);
            }
            if (mRightFragmentContainer.getVisibility() != View.VISIBLE) {
                mRightFragmentContainer.setVisibility(View.VISIBLE);
            }

        } else if (existsSecondFragment) {
            if (mLeftFragmentContainer.getVisibility() != View.GONE) {
                mLeftFragmentContainer.setVisibility(View.GONE);
            }
            if (mRightFragmentContainer.getVisibility() != View.VISIBLE) {
                mRightFragmentContainer.setVisibility(View.VISIBLE);
            }

        } else {
            if (mLeftFragmentContainer.getVisibility() != View.VISIBLE) {
                mLeftFragmentContainer.setVisibility(View.VISIBLE);
            }
            if (mRightFragmentContainer.getVisibility() != View.GONE) {
                mRightFragmentContainer.setVisibility(View.GONE);
            }
        }
    }


    private OCFileListFragment getListOfFilesFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(
                FileDisplayActivity.TAG_LIST_OF_FILES);
        if (listOfFiles != null) {
            return (OCFileListFragment) listOfFiles;
        }
        Log_OC.e(TAG, "Access to unexisting list of files fragment!!");
        return null;
    }

    public FileFragment getSecondFragment() {
        Fragment second = getSupportFragmentManager().findFragmentByTag(
                FileDisplayActivity.TAG_SECOND_FRAGMENT);
        if (second != null) {
            return (FileFragment) second;
        }
        return null;
    }

    protected void cleanSecondFragment() {
        Fragment second = getSecondFragment();
        if (second != null) {
            FragmentTransaction tr = getSupportFragmentManager().beginTransaction();
            tr.remove(second);
            tr.commit();
        }
        updateFragmentsVisibility(false);
        updateActionBarTitleAndHomeButton(null);
    }

    protected void refreshListOfFilesFragment() {
        OCFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) {
            fileListFragment.listDirectory(MainApp.getOnlyOnDevice());
        }
    }

    protected void refreshSecondFragment(String downloadEvent, String downloadedRemotePath,
                                         boolean success) {
        FileFragment secondFragment = getSecondFragment();
        boolean waitedPreview = (mWaitingToPreview != null &&
                mWaitingToPreview.getRemotePath().equals(downloadedRemotePath));
        if (secondFragment != null && secondFragment instanceof FileDetailFragment) {
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
                        mWaitingToPreview = getStorageManager().getFileById(
                                mWaitingToPreview.getFileId());   // update the file from database,
                        // for the local storage path
                        if (PreviewMediaFragment.canBePreviewed(mWaitingToPreview)) {
                            startMediaPreview(mWaitingToPreview, 0, true);
                            detailsFragmentChanged = true;
                        } else if (PreviewTextFragment.canBePreviewed(mWaitingToPreview)) {
                            startTextPreview(mWaitingToPreview);
                            detailsFragmentChanged = true;
                        } else {
                            getFileOperationsHelper().openFile(mWaitingToPreview);
                        }
                    }
                    mWaitingToPreview = null;
                }
                if (!detailsFragmentChanged) {
                    detailsFragment.updateFileDetails(false, (success));
                }
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = isDrawerOpen();

        for (MenuItem menuItem:mDrawerMenuItemstoShowHideList) {
            menuItem.setVisible(!drawerOpen);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.action_create_dir).setVisible(false);

        // populate list of menu items to show/hide when drawer is opened/closed
        mDrawerMenuItemstoShowHideList = new ArrayList<>(4);
        mDrawerMenuItemstoShowHideList.add(menu.findItem(R.id.action_sort));
        mDrawerMenuItemstoShowHideList.add(menu.findItem(R.id.action_sync_account));
        mDrawerMenuItemstoShowHideList.add(menu.findItem(R.id.action_switch_view));
        mDrawerMenuItemstoShowHideList.add(menu.findItem(R.id.action_search));

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case R.id.action_sync_account: {
                startSynchronization();
                break;
            }
            case android.R.id.home: {
                FileFragment second = getSecondFragment();
                OCFile currentDir = getCurrentDir();
                if (isDrawerOpen()) {
                    closeDrawer();
                } else if ((currentDir != null && currentDir.getParentId() != 0) ||
                        (second != null && second.getFile() != null)) {
                    onBackPressed();

                } else {
                    openDrawer();
                }
                break;
            }
            case R.id.action_sort: {
                Integer sortOrder = getSortOrder(this);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.actionbar_sort_title)
                        .setSingleChoiceItems(R.array.actionbar_sortby, sortOrder,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0:
                                                sortByName(true);
                                                break;
                                            case 1:
                                                sortByDate(false);
                                                break;
                                            case 2:
                                                sortBySize(false);
                                        }

                                        dialog.dismiss();
                                    }
                                });
                builder.create().show();
                break;
            }
            case R.id.action_switch_view: {
                if (isGridView()) {
                    item.setTitle(getString(R.string.action_switch_grid_view));
                    item.setIcon(ContextCompat.getDrawable(getApplicationContext(),
                            R.drawable.ic_view_module));
                    getListOfFilesFragment().setListAsPreferred();
                } else {
                    item.setTitle(getApplicationContext().getString(R.string.action_switch_list_view));
                    item.setIcon(ContextCompat.getDrawable(getApplicationContext(),
                            R.drawable.ic_view_list));
                    getListOfFilesFragment().setGridAsPreferred();
                }
                return true;
            }
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    private void startSynchronization() {
        Log_OC.d(TAG, "Got to start sync");
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
            Log_OC.d(TAG, "Canceling all syncs for " + MainApp.getAuthority());
            ContentResolver.cancelSync(null, MainApp.getAuthority());
            // cancel the current synchronizations of any ownCloud account
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            Log_OC.d(TAG, "Requesting sync for " + getAccount().name + " at " +
                    MainApp.getAuthority());
            ContentResolver.requestSync(
                    getAccount(),
                    MainApp.getAuthority(), bundle);
        } else {
            Log_OC.d(TAG, "Requesting sync for " + getAccount().name + " at " +
                    MainApp.getAuthority() + " with new API");
            SyncRequest.Builder builder = new SyncRequest.Builder();
            builder.setSyncAdapter(getAccount(), MainApp.getAuthority());
            builder.setExpedited(true);
            builder.setManual(true);
            builder.syncOnce();

            // Fix bug in Android Lollipop when you click on refresh the whole account
            Bundle extras = new Bundle();
            builder.setExtras(extras);

            SyncRequest request = builder.build();
            ContentResolver.requestSync(request);
        }
    }

    /**
     * Called, when the user selected something for uploading
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
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

        } else if (requestCode == REQUEST_CODE__MOVE_FILES && resultCode == RESULT_OK) {
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

            final Intent fData = data;
            final int fResultCode = resultCode;
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

    private void requestUploadOfFilesFromFileSystem(Intent data, int resultCode) {
        String[] filePaths = data.getStringArrayExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES);
        if (filePaths != null) {
            String[] remotePaths = new String[filePaths.length];
            String remotePathBase = getCurrentDir().getRemotePath();
            for (int j = 0; j < remotePaths.length; j++) {
                remotePaths[j] = remotePathBase + (new File(filePaths[j])).getName();
            }

            // default, as fallback
            int behaviour = FileUploader.LOCAL_BEHAVIOUR_FORGET;

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
            }

            FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
            requester.uploadNewFile(
                    this,
                    getAccount(),
                    filePaths,
                    remotePaths,
                    null,           // MIME type will be detected from file name
                    behaviour,
                    false,          // do not create parent folder if not existent
                    UploadFileOperation.CREATED_BY_USER
            );

        } else {
            Log_OC.d(TAG, "User clicked on 'Update' with no selection");
            Toast t = Toast.makeText(this, getString(R.string.filedisplay_no_file_selected),
                    Toast.LENGTH_LONG);
            t.show();
            return;
        }
    }


    private void requestUploadOfContentFromApps(Intent contentIntent, int resultCode) {

        ArrayList<Parcelable> streamsToUpload = new ArrayList<>();

        //getClipData is only supported on api level 16+, Jelly Bean
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
            contentIntent.getClipData() != null &&
            contentIntent.getClipData().getItemCount() > 0) {

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
     * @param data       Intent received
     */
    private void requestMoveOperation(Intent data) {
        OCFile folderToMoveAt = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
        ArrayList<OCFile> files = data.getParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES);
        getFileOperationsHelper().moveFiles(files, folderToMoveAt);
    }

    /**
     * Request the operation for copying the file/folder from one path to another
     *
     * @param data       Intent received
     */
    private void requestCopyOperation(Intent data) {
        OCFile folderToMoveAt = data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
        ArrayList<OCFile> files = data.getParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES);
        getFileOperationsHelper().copyFiles(files, folderToMoveAt);
    }

    @Override
    public void onBackPressed() {
        boolean isFabOpen = isFabOpen();
        boolean isDrawerOpen = isDrawerOpen();

        /*
         * BackPressed priority/hierarchy:
         *    1. close drawer if opened
         *    2. close FAB if open (only if drawer isn't open)
         *    3. navigate up (only if drawer and FAB aren't open)
         */
        if(isDrawerOpen && isFabOpen) {
            // close drawer first
            super.onBackPressed();
        } else if(isDrawerOpen && !isFabOpen) {
            // close drawer
            super.onBackPressed();
        } else if (!isDrawerOpen && isFabOpen) {
            // close fab
            getListOfFilesFragment().getFabMain().collapse();
        } else {
            // all closed
            OCFileListFragment listOfFiles = getListOfFilesFragment();
            if (mDualPane || getSecondFragment() == null) {
                OCFile currentDir = getCurrentDir();
                if (currentDir == null || currentDir.getParentId() == FileDataStorageManager.ROOT_PARENT_ID) {
                    finish();
                    return;
                }
                if (listOfFiles != null) {  // should never be null, indeed
                    listOfFiles.onBrowseUp();
                }
            }
            if (listOfFiles != null) {  // should never be null, indeed
                setFile(listOfFiles.getCurrentFile());
            }
            cleanSecondFragment();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // responsibility of restore is preferred in onCreate() before than in
        // onRestoreInstanceState when there are Fragments involved
        Log_OC.v(TAG, "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDisplayActivity.KEY_WAITING_TO_PREVIEW, mWaitingToPreview);
        outState.putBoolean(FileDisplayActivity.KEY_SYNC_IN_PROGRESS, mSyncInProgress);
        //outState.putBoolean(FileDisplayActivity.KEY_REFRESH_SHARES_IN_PROGRESS,
        // mRefreshSharesInProgress);
        outState.putParcelable(FileDisplayActivity.KEY_WAITING_TO_SEND, mWaitingToSend);

        Log_OC.v(TAG, "onSaveInstanceState() end");
    }

    @Override
    protected void onResume() {
        Log_OC.v(TAG, "onResume() start");
        super.onResume();

        // refresh list of files
        refreshListOfFilesFragment();

        // Listen for sync messages
        IntentFilter syncIntentFilter = new IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START);
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_END);
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED);
        mSyncBroadcastReceiver = new SyncBroadcastReceiver();
        registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);
        //LocalBroadcastManager.getInstance(this).registerReceiver(mSyncBroadcastReceiver,
        // syncIntentFilter);

        // Listen for upload messages
        IntentFilter uploadIntentFilter = new IntentFilter(FileUploader.getUploadFinishMessage());
        mUploadFinishReceiver = new UploadFinishReceiver();
        registerReceiver(mUploadFinishReceiver, uploadIntentFilter);

        // Listen for download messages
        IntentFilter downloadIntentFilter = new IntentFilter(
                FileDownloader.getDownloadAddedMessage());
        downloadIntentFilter.addAction(FileDownloader.getDownloadFinishMessage());
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        registerReceiver(mDownloadFinishReceiver, downloadIntentFilter);

        Log_OC.v(TAG, "onResume() end");

    }


    @Override
    protected void onPause() {
        Log_OC.v(TAG, "onPause() start");
        if (mSyncBroadcastReceiver != null) {
            unregisterReceiver(mSyncBroadcastReceiver);
            //LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncBroadcastReceiver);
            mSyncBroadcastReceiver = null;
        }
        if (mUploadFinishReceiver != null) {
            unregisterReceiver(mUploadFinishReceiver);
            mUploadFinishReceiver = null;
        }
        if (mDownloadFinishReceiver != null) {
            unregisterReceiver(mDownloadFinishReceiver);
            mDownloadFinishReceiver = null;
        }

        super.onPause();
        Log_OC.v(TAG, "onPause() end");
    }

    public boolean isFabOpen() {
        if(getListOfFilesFragment() != null
                && getListOfFilesFragment().getFabMain() != null
                && getListOfFilesFragment().getFabMain().isExpanded()) {
            return true;
        } else {
            return false;
        }
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
                RemoteOperationResult synchResult =
                        (RemoteOperationResult) intent.getSerializableExtra(
                                FileSyncAdapter.EXTRA_RESULT);
                boolean sameAccount = (getAccount() != null &&
                        accountName.equals(getAccount().name) && getStorageManager() != null);

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
                            Toast.makeText(FileDisplayActivity.this,
                                    String.format(
                                            getString(R.string.
                                                    sync_current_folder_was_removed),
                                            synchFolderRemotePath),

                                    Toast.LENGTH_LONG)
                                    .show();

                            browseToRoot();

                        } else {
                            if (currentFile == null && !getFile().isFolder()) {
                                // currently selected file was removed in the server, and now we
                                // know it
                                cleanSecondFragment();
                                currentFile = currentDir;
                            }

                            if (synchFolderRemotePath != null &&
                                    currentDir.getRemotePath().equals(synchFolderRemotePath)) {
                                OCFileListFragment fileListFragment = getListOfFilesFragment();
                                if (fileListFragment != null) {
                                    fileListFragment.listDirectory(currentDir,
                                    MainApp.getOnlyOnDevice());
                                }
                            }
                            setFile(currentFile);
                        }

                        mSyncInProgress = (!FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) &&
                                !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED
                                        .equals(event));

                        if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.
                            equals(event)) {

                            if (synchResult != null && !synchResult.isSuccess()) {
                                /// TODO refactor and make common

                                if (checkForRemoteOperationError(synchResult)) {

                                    requestCredentialsUpdate(context);

                                } else if (RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED.equals(
                                    synchResult.getCode())) {

                                    showUntrustedCertDialog(synchResult);
                                }

                            }

                        }
                        removeStickyBroadcast(intent);
                        Log_OC.d(TAG, "Setting progress visibility to " + mSyncInProgress);
                        setIndeterminate(mSyncInProgress);

                        setBackgroundText();
                    }
                }

                if (synchResult != null) {
                    if (synchResult.getCode().equals(
                            RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED)) {
                        mLastSslUntrustedServerResult = synchResult;
                    }
                }
            } catch (RuntimeException e) {
                // avoid app crashes after changing the serial id of RemoteOperationResult
                // in owncloud library with broadcast notifications pending to process
                removeStickyBroadcast(intent);
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
        OCFileListFragment ocFileListFragment = getListOfFilesFragment();
        if (ocFileListFragment != null) {
            int message = R.string.file_list_loading;
            if (!mSyncInProgress) {
                // In case file list is empty
                message = R.string.file_list_empty;
            }
            ocFileListFragment.setMessageForEmptyList(getString(message));
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
         * @author David A. Velasco
         * {@link BroadcastReceiver} to enable upload feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String uploadedRemotePath = intent.getStringExtra(FileUploader.EXTRA_REMOTE_PATH);
                String accountName = intent.getStringExtra(FileUploader.ACCOUNT_NAME);
                boolean sameAccount = getAccount() != null && accountName.equals(getAccount().name);
                OCFile currentDir = getCurrentDir();
                boolean isDescendant = (currentDir != null) && (uploadedRemotePath != null) &&
                        (uploadedRemotePath.startsWith(currentDir.getRemotePath()));

                if (sameAccount && isDescendant) {
                    String linkedToRemotePath =
                            intent.getStringExtra(FileUploader.EXTRA_LINKED_TO_PATH);
                    if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                        refreshListOfFilesFragment();
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
                boolean detailFragmentIsShown = (details != null &&
                        details instanceof FileDetailFragment);

                if (sameAccount && sameFile && detailFragmentIsShown) {
                    if (uploadWasFine) {
                        setFile(getStorageManager().getFileByPath(uploadedRemotePath));
                    } else {
                        //TODO remove upload progress bar after upload failed.
                    }
                    if (renamedInUpload) {
                        String newName = (new File(uploadedRemotePath)).getName();
                        Toast msg = Toast.makeText(
                                context,
                                String.format(
                                        getString(R.string.filedetails_renamed_in_upload_msg),
                                        newName),
                                Toast.LENGTH_LONG);
                        msg.show();
                    }
                    if (uploadWasFine || getFile().fileExists()) {
                        ((FileDetailFragment) details).updateFileDetails(false, true);
                    } else {
                        cleanSecondFragment();
                    }

                    // Force the preview if the file is an image or text file
                    if (uploadWasFine) {
                        OCFile ocFile = getFile();
                        if (PreviewImageFragment.canBePreviewed(ocFile))
                            startImagePreview(getFile());
                        else if (PreviewTextFragment.canBePreviewed(ocFile))
                            startTextPreview(ocFile);
                        // TODO what about other kind of previews?
                    }
                }

                setIndeterminate(false);

            } finally {
                if (intent != null) {
                    removeStickyBroadcast(intent);
                }
            }

        }

        // TODO refactor this receiver, and maybe DownloadFinishReceiver; this method is duplicated :S
        private boolean isAscendant(String linkedToRemotePath) {
            OCFile currentDir = getCurrentDir();
            return (
                    currentDir != null &&
                            currentDir.getRemotePath().startsWith(linkedToRemotePath)
            );
        }

    }


    /**
     * Class waiting for broadcast events from the {@link FileDownloader} service.
     * <p/>
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * current folder.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                boolean sameAccount = isSameAccount(intent);
                String downloadedRemotePath =
                        intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
                boolean isDescendant = isDescendant(downloadedRemotePath);

                if (sameAccount && isDescendant) {
                    String linkedToRemotePath =
                            intent.getStringExtra(FileDownloader.EXTRA_LINKED_TO_PATH);
                    if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                        refreshListOfFilesFragment();
                    }
                    refreshSecondFragment(
                            intent.getAction(),
                            downloadedRemotePath,
                            intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false)
                    );
                }

                if (mWaitingToSend != null) {
                    mWaitingToSend =
                            getStorageManager().getFileByPath(mWaitingToSend.getRemotePath());
                    if (mWaitingToSend.isDown()) {
                        sendDownloadedFile();
                    }
                }

            } finally {
                if (intent != null) {
                    removeStickyBroadcast(intent);
                }
            }
        }

        private boolean isDescendant(String downloadedRemotePath) {
            OCFile currentDir = getCurrentDir();
            return (
                    currentDir != null &&
                            downloadedRemotePath != null &&
                            downloadedRemotePath.startsWith(currentDir.getRemotePath())
            );
        }

        private boolean isAscendant(String linkedToRemotePath) {
            OCFile currentDir = getCurrentDir();
            return (
                    currentDir != null &&
                            currentDir.getRemotePath().startsWith(linkedToRemotePath)
            );
        }

        private boolean isSameAccount(Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);
            return (accountName != null && getAccount() != null &&
                    accountName.equals(getAccount().name));
        }
    }


    public void browseToRoot() {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            listOfFiles.listDirectory(root, MainApp.getOnlyOnDevice());
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
        Fragment detailFragment = FileDetailFragment.newInstance(file, getAccount());
        setSecondFragment(detailFragment);
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }

    @Override
    protected void updateActionBarTitleAndHomeButton(OCFile chosenFile) {
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
    protected ServiceConnection newTransferenceServiceConnection() {
        return new ListServiceConnection();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private class ListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(new ComponentName(
                    FileDisplayActivity.this, FileDownloader.class))) {
                Log_OC.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mWaitingToPreview != null)
                    if (getStorageManager() != null) {
                        // update the file
                        mWaitingToPreview =
                                getStorageManager().getFileById(mWaitingToPreview.getFileId());
                        if (!mWaitingToPreview.isDown()) {
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
            if (listOfFiles != null) {
                listOfFiles.listDirectory(MainApp.getOnlyOnDevice());
            }
            FileFragment secondFragment = getSecondFragment();
            if (secondFragment != null && secondFragment instanceof FileDetailFragment) {
                FileDetailFragment detailFragment = (FileDetailFragment) secondFragment;
                detailFragment.listenForTransferProgress();
                detailFragment.updateFileDetails(false, false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(FileDisplayActivity.this,
                    FileDownloader.class))) {
                Log_OC.d(TAG, "Download service disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(FileDisplayActivity.this,
                    FileUploader.class))) {
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
                    ((PreviewTextFragment) details).updateFile(file);
                } else {
                    showDetails(file);
                }
            }
            invalidateOptionsMenu();
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
        Toast msg = Toast.makeText(this,
            ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
            Toast.LENGTH_LONG);
        msg.show();

        if (result.isSuccess()) {
            OCFile removedFile = operation.getFile();
            FileFragment second = getSecondFragment();
            if (second != null && removedFile.equals(second.getFile())) {
                if (second instanceof PreviewMediaFragment) {
                    ((PreviewMediaFragment) second).stopPreview(true);
                }
                setFile(getStorageManager().getFileById(removedFile.getParentId()));
                cleanSecondFragment();
            }
            if (getStorageManager().getFileById(removedFile.getParentId()).equals(getCurrentDir())) {
                refreshListOfFilesFragment();
            }
            invalidateOptionsMenu();
        } else {
            if (result.isSslRecoverableException()) {
                mLastSslUntrustedServerResult = result;
                showUntrustedCertDialog(mLastSslUntrustedServerResult);
            }
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
            refreshListOfFilesFragment();
        } else {
            try {
                Toast msg = Toast.makeText(FileDisplayActivity.this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                msg.show();

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
            refreshListOfFilesFragment();
        } else {
            try {
                Toast msg = Toast.makeText(FileDisplayActivity.this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                msg.show();

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
        OCFile renamedFile = operation.getFile();
        if (result.isSuccess()) {
            FileFragment details = getSecondFragment();
            if (details != null) {
                if (details instanceof FileDetailFragment &&
                        renamedFile.equals(details.getFile())) {
                    ((FileDetailFragment) details).updateFileDetails(renamedFile, getAccount());
                    showDetails(renamedFile);

                } else if (details instanceof PreviewMediaFragment &&
                        renamedFile.equals(details.getFile())) {
                    ((PreviewMediaFragment) details).updateFile(renamedFile);
                    if (PreviewMediaFragment.canBePreviewed(renamedFile)) {
                        int position = ((PreviewMediaFragment) details).getPosition();
                        startMediaPreview(renamedFile, position, true);
                    } else {
                        getFileOperationsHelper().openFile(renamedFile);
                    }
                } else if (details instanceof PreviewTextFragment &&
                        renamedFile.equals(details.getFile())) {
                    ((PreviewTextFragment) details).updateFile(renamedFile);
                    if (PreviewTextFragment.canBePreviewed(renamedFile)) {
                        startTextPreview(renamedFile);
                    } else {
                        getFileOperationsHelper().openFile(renamedFile);
                    }
                }
            }

            if (getStorageManager().getFileById(renamedFile.getParentId()).equals(getCurrentDir())) {
                refreshListOfFilesFragment();
            }

        } else {
            Toast msg = Toast.makeText(this,
                    ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                    Toast.LENGTH_LONG);
            msg.show();

            if (result.isSslRecoverableException()) {
                mLastSslUntrustedServerResult = result;
                showUntrustedCertDialog(mLastSslUntrustedServerResult);
            }
        }
    }


    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation,
                                                  RemoteOperationResult result) {
        if (result.isSuccess()) {
            if (operation.transferWasRequested()) {
                OCFile syncedFile = operation.getLocalFile();
                onTransferStateChanged(syncedFile, true, true);
                invalidateOptionsMenu();
                refreshShowDetails();
            }
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
            refreshListOfFilesFragment();
        } else {
            try {
                Toast msg = Toast.makeText(FileDisplayActivity.this,
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()),
                        Toast.LENGTH_LONG);
                msg.show();

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
        refreshListOfFilesFragment();
        FileFragment details = getSecondFragment();
        if (details != null && details instanceof FileDetailFragment &&
                file.equals(details.getFile())) {
            if (downloading || uploading) {
                ((FileDetailFragment) details).updateFileDetails(file, getAccount());
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
        Account account = getAccount();
        //if (!mWaitingToPreview.isDownloading()) {
        if (!mDownloaderBinder.isDownloading(account, mWaitingToPreview)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
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
     * <p/>
     * The operation is run in a new background thread created on the fly.
     * <p/>
     * The refresh updates is a "light sync": properties of regular files in folder are updated (including
     * associated shares), but not their contents. Only the contents of files marked to be kept-in-sync are
     * synchronized too.
     *
     * @param folder     Folder to refresh.
     * @param ignoreETag If 'true', the data from the server will be fetched and sync'ed even if the eTag
     *                   didn't change.
     */
    public void startSyncFolderOperation(final OCFile folder, final boolean ignoreETag) {

        // the execution is slightly delayed to allow the activity get the window focus if it's being started
        // or if the method is called from a dialog that is being dismissed
        getHandler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        if (hasWindowFocus()) {
                            long currentSyncTime = System.currentTimeMillis();
                            mSyncInProgress = true;

                            // perform folder synchronization
                            RemoteOperation synchFolderOp = new RefreshFolderOperation(folder,
                                    currentSyncTime,
                                    false,
                                    getFileOperationsHelper().isSharedSupported(),
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

                            setIndeterminate(true);

                            setBackgroundText();

                        }   // else: NOTHING ; lets' not refresh when the user rotates the device but there is
                        // another window floating over
                    }
                },
                DELAY_TO_REQUEST_OPERATIONS_LATER
        );

    }

    private void requestForDownload(OCFile file) {
        Account account = getAccount();
        if (!mDownloaderBinder.isDownloading(account, mWaitingToPreview)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
            i.putExtra(FileDownloader.EXTRA_FILE, file);
            startService(i);
        }
    }

    private void sendDownloadedFile() {
        getFileOperationsHelper().sendDownloadedFile(mWaitingToSend);
        mWaitingToSend = null;
    }


    /**
     * Requests the download of the received {@link OCFile} , updates the UI
     * to monitor the download progress and prepares the activity to send the file
     * when the download finishes.
     *
     * @param file {@link OCFile} to download and preview.
     */
    public void startDownloadForSending(OCFile file) {
        mWaitingToSend = file;
        requestForDownload(mWaitingToSend);
        boolean hasSecondFragment = (getSecondFragment() != null);
        updateFragmentsVisibility(hasSecondFragment);
    }

    /**
     * Opens the image gallery showing the image {@link OCFile} received as parameter.
     *
     * @param file Image {@link OCFile} to show.
     */
    public void startImagePreview(OCFile file) {
        Intent showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        showDetailsIntent.putExtra(EXTRA_FILE, file);
        showDetailsIntent.putExtra(EXTRA_ACCOUNT, getAccount());
        startActivity(showDetailsIntent);

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
    public void startMediaPreview(OCFile file, int startPlaybackPosition, boolean autoplay) {
        Fragment mediaFragment = new PreviewMediaFragment(file, getAccount(), startPlaybackPosition,
                autoplay);
        setSecondFragment(mediaFragment);
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }

    /**
     * Stars the preview of a text file {@link OCFile}.
     *
     * @param file Text {@link OCFile} to preview.
     */
    public void startTextPreview(OCFile file) {
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_FILE, file);
        args.putParcelable(EXTRA_ACCOUNT, getAccount());
        Fragment textPreviewFragment = Fragment.instantiate(getApplicationContext(),
                PreviewTextFragment.class.getName(), args);
        setSecondFragment(textPreviewFragment);
        updateFragmentsVisibility(true);
        updateActionBarTitleAndHomeButton(file);
        setFile(file);
    }

    /**
     * Requests the download of the received {@link OCFile} , updates the UI
     * to monitor the download progress and prepares the activity to preview
     * or open the file when the download finishes.
     *
     * @param file {@link OCFile} to download and preview.
     */
    public void startDownloadForPreview(OCFile file) {
        Fragment detailFragment = FileDetailFragment.newInstance(file, getAccount());
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
        for(OCFile file: files) {
            cancelTransference(file);
        }
    }

    @Override
    public void onRefresh(boolean ignoreETag) {
        refreshList(ignoreETag);
    }

    @Override
    public void onRefresh() {
        refreshList(true);
    }

    private void refreshList(boolean ignoreETag) {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {
            OCFile folder = listOfFiles.getCurrentFile();
            if (folder != null) {
                /*mFile = mContainerActivity.getStorageManager().getFileById(mFile.getFileId());
                listDirectory(mFile);*/
                startSyncFolderOperation(folder, ignoreETag);
            }
        }
    }

    private void sortByDate(boolean ascending) {
        getListOfFilesFragment().sortByDate(ascending);
    }

    private void sortBySize(boolean ascending) {
        getListOfFilesFragment().sortBySize(ascending);
    }

    private void sortByName(boolean ascending) {
        getListOfFilesFragment().sortByName(ascending);
    }

    private boolean isGridView() {
        return getListOfFilesFragment().isGridEnabled();
    }

    public void allFilesOption() {
        browseToRoot();
    }

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        getListOfFilesFragment().refreshDirectory();
    }
}
