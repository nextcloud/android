/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.DataHolderUtil;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.ThemeUtils;

import java.util.ArrayList;

import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import static com.owncloud.android.db.PreferenceManager.getSortOrderByFolder;

public class FolderPickerActivity extends FileActivity implements FileFragment.ContainerActivity,
    OnClickListener, OnEnforceableRefreshListener {

    public static final String EXTRA_FOLDER = FolderPickerActivity.class.getCanonicalName() + ".EXTRA_FOLDER";
    public static final String EXTRA_FILES = FolderPickerActivity.class.getCanonicalName() + ".EXTRA_FILES";
    public static final String EXTRA_ACTION = FolderPickerActivity.class.getCanonicalName() + ".EXTRA_ACTION";
    public static final String EXTRA_CURRENT_FOLDER = FolderPickerActivity.class.getCanonicalName() +
        ".EXTRA_CURRENT_FOLDER";
    public static final String MOVE = "MOVE";
    public static final String COPY = "COPY";

    private SyncBroadcastReceiver mSyncBroadcastReceiver;

    private static final String TAG = FolderPickerActivity.class.getSimpleName();

    protected static final String TAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS";

    private boolean mSyncInProgress;

    private boolean mSearchOnlyFolders;
    private boolean mDoNotEnterEncryptedFolder;

    protected Button mCancelBtn;
    protected Button mChooseBtn;
    private String caption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate() start");

        super.onCreate(savedInstanceState);

        if (this instanceof FilePickerActivity) {
            setContentView(R.layout.files_picker);
        } else {
            setContentView(R.layout.files_folder_picker);
        }

        // sets callback listeners for UI elements
        initControls();

        // Action bar setup
        setupToolbar();

        if (getIntent().getStringExtra(EXTRA_ACTION) != null) {
            switch (getIntent().getStringExtra(EXTRA_ACTION)) {
                case MOVE:
                    caption = getResources().getText(R.string.move_to).toString();
                    mSearchOnlyFolders = true;
                    mDoNotEnterEncryptedFolder = true;
                    break;
                case COPY:
                    caption = getResources().getText(R.string.copy_to).toString();
                    mSearchOnlyFolders = true;
                    mDoNotEnterEncryptedFolder = true;
                    break;
                default:
                    caption = ThemeUtils.getDefaultDisplayNameForRootFolder(this);
                    break;
            }
        } else {
            caption = ThemeUtils.getDefaultDisplayNameForRootFolder(this);
        }

        if (getIntent().getParcelableExtra(EXTRA_CURRENT_FOLDER) != null) {
            setFile(getIntent().getParcelableExtra(EXTRA_CURRENT_FOLDER));
        }

        if (savedInstanceState == null) {
            createFragments();
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(true);
            ThemeUtils.setColoredTitle(getSupportActionBar(), caption, this);
        }

        setIndeterminate(mSyncInProgress);
        // always AFTER setContentView(...) ; to work around bug in its implementation

        // sets message for empty list of folders
        setBackgroundText();

        Log_OC.d(TAG, "onCreate() end");
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {

            updateFileFromDB();

            OCFile folder = getFile();
            if (folder == null || !folder.isFolder()) {
                // fall back to root folder
                setFile(getStorageManager().getFileByPath(OCFile.ROOT_PATH));
                folder = getFile();
            }

            if (!stateWasRecovered) {
                OCFileListFragment listOfFolders = getListOfFilesFragment();
                listOfFolders.listDirectory(folder, false, false);

                startSyncFolderOperation(folder, false);
            }

            updateNavigationElementsInActionBar();
        }
    }

    private Activity getActivity() {
        return this;
    }

    protected void createFragments() {
        OCFileListFragment listOfFiles = new OCFileListFragment();
        Bundle args = new Bundle();
        args.putBoolean(OCFileListFragment.ARG_ONLY_FOLDERS_CLICKABLE, true);
        args.putBoolean(OCFileListFragment.ARG_HIDE_FAB, true);
        args.putBoolean(OCFileListFragment.ARG_HIDE_ITEM_OPTIONS, true);
        args.putBoolean(OCFileListFragment.ARG_SEARCH_ONLY_FOLDER, mSearchOnlyFolders);
        listOfFiles.setArguments(args);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, listOfFiles, TAG_LIST_OF_FOLDERS);
        transaction.commit();
    }

    /**
     * Show a text message on screen view for notifying user if content is
     * loading or folder is empty
     */
    private void setBackgroundText() {
        OCFileListFragment listFragment = getListOfFilesFragment();
        if (listFragment != null) {
            if (!mSyncInProgress) {
                listFragment.setMessageForEmptyList(
                    R.string.file_list_empty_headline,
                    R.string.file_list_empty_moving,
                    R.drawable.ic_list_empty_create_folder,
                    true
                );
            } else {
                listFragment.setEmptyListLoadingMessage();
            }
        } else {
            Log.e(TAG, "OCFileListFragment is null");
        }
    }

    protected OCFileListFragment getListOfFilesFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(FolderPickerActivity.TAG_LIST_OF_FOLDERS);
        if (listOfFiles != null) {
            return (OCFileListFragment) listOfFiles;
        }
        Log_OC.e(TAG, "Access to non existing list of files fragment!!");
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Updates action bar and second fragment, if in dual pane mode.
     */
    @Override
    public void onBrowsedDownTo(OCFile directory) {
        setFile(directory);
        updateNavigationElementsInActionBar();
        // Sync Folder
        startSyncFolderOperation(directory, false);
    }

    @Override
    public void onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir(), false);
    }

    public void startSyncFolderOperation(OCFile folder, boolean ignoreETag) {
        long currentSyncTime = System.currentTimeMillis();

        mSyncInProgress = true;

        // perform folder synchronization
        RemoteOperation refreshFolderOperation = new RefreshFolderOperation(folder, currentSyncTime, false,
            ignoreETag, getStorageManager(), getAccount(), getApplicationContext());

        refreshFolderOperation.execute(getAccount(), this, null, null);
        setIndeterminate(true);
        setBackgroundText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log_OC.e(TAG, "onResume() start");

        // refresh list of files
        refreshListOfFilesFragment(false);

        // Listen for sync messages
        IntentFilter syncIntentFilter = new IntentFilter(FileSyncAdapter.EVENT_FULL_SYNC_START);
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_END);
        syncIntentFilter.addAction(FileSyncAdapter.EVENT_FULL_SYNC_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED);
        mSyncBroadcastReceiver = new SyncBroadcastReceiver();
        registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);

        Log_OC.d(TAG, "onResume() end");
    }

    @Override
    protected void onPause() {
        Log_OC.e(TAG, "onPause() start");
        if (mSyncBroadcastReceiver != null) {
            unregisterReceiver(mSyncBroadcastReceiver);
            //LocalBroadcastManager.getInstance(this).unregisterReceiver(mSyncBroadcastReceiver);
            mSyncBroadcastReceiver = null;
        }

        Log_OC.d(TAG, "onPause() end");
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.action_switch_view).setVisible(false);
        menu.findItem(R.id.action_sync_account).setVisible(false);
        menu.findItem(R.id.action_select_all).setVisible(false);
        // menu.findItem(R.id.action_sort).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case R.id.action_create_dir: {
                CreateFolderDialogFragment dialog = CreateFolderDialogFragment.newInstance(getCurrentFolder());
                dialog.show(getSupportFragmentManager(), CreateFolderDialogFragment.CREATE_FOLDER_FRAGMENT);
                break;
            }
            case android.R.id.home: {
                OCFile currentDir = getCurrentFolder();
                if (currentDir != null && currentDir.getParentId() != 0) {
                    onBackPressed();
                }
                break;
            }
            case R.id.action_sort: {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.addToBackStack(null);

                SortingOrderDialogFragment mSortingOrderDialogFragment = SortingOrderDialogFragment.newInstance(
                    getSortOrderByFolder(this, getListOfFilesFragment().getCurrentFile()));
                mSortingOrderDialogFragment.show(ft, SortingOrderDialogFragment.SORTING_ORDER_FRAGMENT);

                break;
            }
            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }

        return retval;
    }

    protected OCFile getCurrentFolder() {
        OCFile file = getFile();
        if (file != null) {
            if (file.isFolder()) {
                return file;
            } else if (getStorageManager() != null) {
                String parentPath = file.getRemotePath().substring(0, file.getRemotePath().lastIndexOf(file.getFileName()));
                return getStorageManager().getFileByPath(parentPath);
            }
        }
        return null;
    }

    public void refreshListOfFilesFragment(boolean fromSearch) {
        OCFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) {
            fileListFragment.listDirectory(false, fromSearch);
        }
    }

    public void browseToRoot() {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            listOfFiles.listDirectory(root, false, false);
            setFile(listOfFiles.getCurrentFile());
            updateNavigationElementsInActionBar();
            startSyncFolderOperation(root, false);
        }
    }

    @Override
    public void onBackPressed() {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            int levelsUp = listOfFiles.onBrowseUp();
            if (levelsUp == 0) {
                finish();
                return;
            }
            setFile(listOfFiles.getCurrentFile());
            updateNavigationElementsInActionBar();
        }
    }

    protected void updateNavigationElementsInActionBar() {
        OCFile currentDir = getCurrentFolder();
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            boolean atRoot = currentDir == null || currentDir.getParentId() == 0;
            actionBar.setDisplayHomeAsUpEnabled(!atRoot);
            actionBar.setHomeButtonEnabled(!atRoot);

            Drawable backArrow = getResources().getDrawable(R.drawable.ic_arrow_back);

            actionBar.setHomeAsUpIndicator(ThemeUtils.tintDrawable(backArrow, ThemeUtils.fontColor(this)));

            ThemeUtils.setColoredTitle(getSupportActionBar(), atRoot ? caption : currentDir.getFileName(), this);
        }
    }

    /**
     * Set per-view controllers
     */
    private void initControls() {
        mCancelBtn = findViewById(R.id.folder_picker_btn_cancel);
        mCancelBtn.setOnClickListener(this);
        mChooseBtn = findViewById(R.id.folder_picker_btn_choose);

        if (mChooseBtn != null) {
            mChooseBtn.getBackground().setColorFilter(ThemeUtils.primaryColor(this, true), PorterDuff.Mode.SRC_ATOP);
            mChooseBtn.setOnClickListener(this);
        }

        if (mCancelBtn != null) {
            mCancelBtn.setTextColor(ThemeUtils.primaryColor(this, true));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mCancelBtn)) {
            finish();
        } else if (v.equals(mChooseBtn)) {
            Intent i = getIntent();
            ArrayList<Parcelable> targetFiles = i.getParcelableArrayListExtra(FolderPickerActivity.EXTRA_FILES);

            Intent data = new Intent();
            data.putExtra(EXTRA_FOLDER, getCurrentFolder());
            data.putParcelableArrayListExtra(EXTRA_FILES, targetFiles);
            setResult(RESULT_OK, data);

            finish();
        }
    }


    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);

        if (operation instanceof CreateFolderOperation) {
            onCreateFolderOperationFinish((CreateFolderOperation) operation, result);

        }
    }


    /**
     * Updates the view associated to the activity after the finish of an operation trying
     * to create a new folder.
     *
     * @param operation Creation operation performed.
     * @param result    Result of the creation.
     */
    private void onCreateFolderOperationFinish(
        CreateFolderOperation operation, RemoteOperationResult result
    ) {

        if (result.isSuccess()) {
            refreshListOfFilesFragment(false);
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
                String syncFolderRemotePath = intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH);
                RemoteOperationResult syncResult = (RemoteOperationResult)
                    DataHolderUtil.getInstance().retrieve(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT));
                boolean sameAccount = getAccount() != null && accountName.equals(getAccount().name)
                    && getStorageManager() != null;

                if (sameAccount) {
                    if (FileSyncAdapter.EVENT_FULL_SYNC_START.equals(event)) {
                        mSyncInProgress = true;
                    } else {
                        OCFile currentFile = (getFile() == null) ? null :
                            getStorageManager().getFileByPath(getFile().getRemotePath());
                        OCFile currentDir = (getCurrentFolder() == null) ? null :
                            getStorageManager().getFileByPath(getCurrentFolder().getRemotePath());

                        if (currentDir == null) {
                            // current folder was removed from the server
                            DisplayUtils.showSnackMessage(getActivity(), R.string.sync_current_folder_was_removed,
                                getCurrentFolder().getFileName());
                            browseToRoot();
                        } else {
                            if (currentFile == null && !getFile().isFolder()) {
                                // currently selected file was removed in the server, and now we know it
                                currentFile = currentDir;
                            }

                            if (currentDir.getRemotePath().equals(syncFolderRemotePath)) {
                                OCFileListFragment fileListFragment = getListOfFilesFragment();
                                if (fileListFragment != null) {
                                    fileListFragment.listDirectory(currentDir, false, false);
                                }
                            }
                            setFile(currentFile);
                        }

                        mSyncInProgress = (!FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) &&
                            !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED.equals(event));

                        if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.equals(event) &&
                            /// TODO refactor and make common
                            syncResult != null && !syncResult.isSuccess()) {

                            if (ResultCode.UNAUTHORIZED.equals(syncResult.getCode()) || (syncResult.isException()
                                && syncResult.getException() instanceof AuthenticatorException)) {
                                requestCredentialsUpdate(context);
                            } else if (RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED
                                .equals(syncResult.getCode())) {
                                showUntrustedCertDialog(syncResult);
                            }

                        }
                    }
                    removeStickyBroadcast(intent);
                    DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT));
                    Log_OC.d(TAG, "Setting progress visibility to " + mSyncInProgress);

                    setIndeterminate(mSyncInProgress);

                    setBackgroundText();
                }

            } catch (RuntimeException e) {
                // avoid app crashes after changing the serial id of RemoteOperationResult
                // in owncloud library with broadcast notifications pending to process
                removeStickyBroadcast(intent);
                DataHolderUtil.getInstance().delete(intent.getStringExtra(FileSyncAdapter.EXTRA_RESULT));
            }
        }
    }

    @Override
    public void showDetails(OCFile file) {
        // not used at the moment
    }

    @Override
    public void showDetails(OCFile file, int activeTab) {
        // not used at the moment
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading) {
        // not used at the moment
    }

    @Override
    public void onRefresh() {
        refreshList(true);
    }

    @Override
    public void onRefresh(boolean enforced) {
        refreshList(enforced);
    }

    private void refreshList(boolean ignoreETag) {
        OCFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {
            OCFile folder = listOfFiles.getCurrentFile();
            if (folder != null) {
                startSyncFolderOperation(folder, ignoreETag);
            }
        }
    }

    public boolean isDoNotEnterEncryptedFolder() {
        return mDoNotEnterEncryptedFolder;
    }
}
