/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
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
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.utils.ErrorMessageAdapter;

public class FolderPickerActivity extends FileActivity implements FileFragment.ContainerActivity, 
    OnClickListener, OnEnforceableRefreshListener {

    public static final String EXTRA_FOLDER = UploadFilesActivity.class.getCanonicalName()
                                                            + ".EXTRA_FOLDER";
    public static final String EXTRA_FILE = UploadFilesActivity.class.getCanonicalName()
                                                            + ".EXTRA_FILE";
    //TODO: Think something better

    private SyncBroadcastReceiver mSyncBroadcastReceiver;

    private static final String TAG = FolderPickerActivity.class.getSimpleName();
    
    private static final String TAG_LIST_OF_FOLDERS = "LIST_OF_FOLDERS";
       
    private boolean mSyncInProgress = false;

    protected Button mCancelBtn;
    protected Button mChooseBtn;
    private ProgressBar mProgressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate() start");

        super.onCreate(savedInstanceState); 

        setContentView(R.layout.files_folder_picker);
        
        if (savedInstanceState == null) {
            createFragments();
        }

        // sets callback listeners for UI elements
        initControls();

        // Action bar setup
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setIndeterminateDrawable(
                getResources().getDrawable(
                        R.drawable.actionbar_progress_indeterminate_horizontal));
        mProgressBar.setIndeterminate(mSyncInProgress);
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
     *  Called when the ownCloud {@link Account} associated to the Activity was just updated.
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
                listOfFolders.listDirectory(folder/*, false*/);
                
                startSyncFolderOperation(folder, false);
            }
            
            updateNavigationElementsInActionBar();
        }
    }

    private void createFragments() {
        OCFileListFragment listOfFiles = new OCFileListFragment();
        Bundle args = new Bundle();
        args.putBoolean(OCFileListFragment.ARG_JUST_FOLDERS, true);
        args.putBoolean(OCFileListFragment.ARG_ALLOW_CONTEXTUAL_ACTIONS, false);
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
            int message = R.string.file_list_loading;
            if (!mSyncInProgress) {
                // In case folder list is empty
                message = R.string.file_list_empty_moving;
            }
            listFragment.setMessageForEmptyList(getString(message));
        } else {
            Log.e(TAG, "OCFileListFragment is null");
        }
    }

    protected OCFileListFragment getListOfFilesFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(FolderPickerActivity.TAG_LIST_OF_FOLDERS);
        if (listOfFiles != null) {
            return (OCFileListFragment)listOfFiles;
        }
        Log_OC.wtf(TAG, "Access to unexisting list of files fragment!!");
        return null;
    }

    
    /**
     * {@inheritDoc}
     * 
     * Updates action bar and second fragment, if in dual pane mode.
     */
    @Override
    public void onBrowsedDownTo(OCFile directory) {
        setFile(directory);
        updateNavigationElementsInActionBar();
        // Sync Folder
        startSyncFolderOperation(directory, false);
        
    }

    
    public void startSyncFolderOperation(OCFile folder, boolean ignoreETag) {
        long currentSyncTime = System.currentTimeMillis(); 
        
        mSyncInProgress = true;
                
        // perform folder synchronization
        RemoteOperation synchFolderOp = new RefreshFolderOperation( folder,
                                                                        currentSyncTime, 
                                                                        false,
                                                                        getFileOperationsHelper().isSharedSupported(),
                                                                        ignoreETag,
                                                                        getStorageManager(), 
                                                                        getAccount(), 
                                                                        getApplicationContext()
                                                                      );
        synchFolderOp.execute(getAccount(), this, null, null);

        mProgressBar.setIndeterminate(true);

        setBackgroundText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log_OC.e(TAG, "onResume() start");
        
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
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_sort).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
        case R.id.action_create_dir: {
            CreateFolderDialogFragment dialog = 
                    CreateFolderDialogFragment.newInstance(getCurrentFolder());
            dialog.show(
                    getSupportFragmentManager(), 
                    CreateFolderDialogFragment.CREATE_FOLDER_FRAGMENT
            );
            break;
        }
        case android.R.id.home: {
            OCFile currentDir = getCurrentFolder();
            if(currentDir != null && currentDir.getParentId() != 0) {
                onBackPressed();
            }
            break;
        }
        default:
            retval = super.onOptionsItemSelected(item);
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
    
    protected void refreshListOfFilesFragment() {
        OCFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) {
            fileListFragment.listDirectory();
            // TODO Enable when "On Device" is recovered ?
            // fileListFragment.listDirectory(false);
        }
    }

    public void browseToRoot() {
        OCFileListFragment listOfFiles = getListOfFilesFragment(); 
        if (listOfFiles != null) {  // should never be null, indeed
            OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            listOfFiles.listDirectory(root);
            // TODO Enable when "On Device" is recovered ?
            // listOfFiles.listDirectory(root, false);
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
        ActionBar actionBar = getSupportActionBar();
        OCFile currentDir = getCurrentFolder();
        boolean atRoot = (currentDir == null || currentDir.getParentId() == 0);
        actionBar.setDisplayHomeAsUpEnabled(!atRoot);
        actionBar.setHomeButtonEnabled(!atRoot);
        actionBar.setTitle(
                atRoot
                        ? getString(R.string.default_display_name_for_root_folder)
                        : currentDir.getFileName()
        );
    }

    /**
     * Set per-view controllers
     */
    private void initControls(){
        mCancelBtn = (Button) findViewById(R.id.folder_picker_btn_cancel);
        mCancelBtn.setOnClickListener(this);
        mChooseBtn = (Button) findViewById(R.id.folder_picker_btn_choose);
        mChooseBtn.setOnClickListener(this);
    }
    
    @Override
    public void onClick(View v) {
        if (v == mCancelBtn) {
            finish();
        } else if (v == mChooseBtn) {
            Intent i = getIntent();
            Parcelable targetFile = i.getParcelableExtra(FolderPickerActivity.EXTRA_FILE);

            Intent data = new Intent();
            data.putExtra(EXTRA_FOLDER, getCurrentFolder());
            if (targetFile != null) {
                data.putExtra(EXTRA_FILE, targetFile);
            }
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
     * @param operation     Creation operation performed.
     * @param result        Result of the creation.
     */
    private void onCreateFolderOperationFinish(
            CreateFolderOperation operation, RemoteOperationResult result
            ) {
        
        if (result.isSuccess()) {
            refreshListOfFilesFragment();
        } else {
            try {
                Toast msg = Toast.makeText(FolderPickerActivity.this, 
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                        Toast.LENGTH_LONG); 
                msg.show();

            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message " , e);
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
                String synchFolderRemotePath = intent.getStringExtra(FileSyncAdapter.EXTRA_FOLDER_PATH); 
                RemoteOperationResult synchResult = (RemoteOperationResult)intent.
                        getSerializableExtra(FileSyncAdapter.EXTRA_RESULT);
                boolean sameAccount = (getAccount() != null && 
                        accountName.equals(getAccount().name) && getStorageManager() != null); 
    
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
                            Toast.makeText( FolderPickerActivity.this, 
                                            String.format(
                                                    getString(R.string.sync_current_folder_was_removed), 
                                                    getCurrentFolder().getFileName()), 
                                            Toast.LENGTH_LONG)
                                .show();
                            browseToRoot();
                            
                        } else {
                            if (currentFile == null && !getFile().isFolder()) {
                                // currently selected file was removed in the server, and now we know it
                                currentFile = currentDir;
                            }

                            if (synchFolderRemotePath != null && currentDir.getRemotePath().
                                    equals(synchFolderRemotePath)) {
                                OCFileListFragment fileListFragment = getListOfFilesFragment();
                                if (fileListFragment != null) {
                                    fileListFragment.listDirectory(currentDir);
                                    // TODO Enable when "On Device" is recovered ?
                                    // fileListFragment.listDirectory(currentDir, false);
                                }
                            }
                            setFile(currentFile);
                        }
                        
                        mSyncInProgress = (!FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) && 
                                !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED.equals(event));
                                
                        if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.
                                    equals(event) &&
                                /// TODO refactor and make common
                                synchResult != null && !synchResult.isSuccess() &&  
                                (synchResult.getCode() == ResultCode.UNAUTHORIZED   || 
                                    synchResult.isIdPRedirection()                  ||
                                    (synchResult.isException() && synchResult.getException() 
                                            instanceof AuthenticatorException))) {

                            try {
                                OwnCloudClient client;
                                OwnCloudAccount ocAccount =
                                        new OwnCloudAccount(getAccount(), context);
                                client = (OwnCloudClientManagerFactory.getDefaultSingleton().
                                        removeClientFor(ocAccount));

                                if (client != null) {
                                    OwnCloudCredentials cred = client.getCredentials();
                                    if (cred != null) {
                                        AccountManager am = AccountManager.get(context);
                                        if (cred.authTokenExpires()) {
                                            am.invalidateAuthToken(
                                                    getAccount().type,
                                                    cred.getAuthToken()
                                            );
                                        } else {
                                            am.clearPassword(getAccount());
                                        }
                                    }
                                }
                                requestCredentialsUpdate();

                            } catch (AccountNotFoundException e) {
                                Log_OC.e(TAG, "Account " + getAccount() + " was removed!", e);
                            }

                        }
                    }
                    removeStickyBroadcast(intent);
                    Log_OC.d(TAG, "Setting progress visibility to " + mSyncInProgress);

                    mProgressBar.setIndeterminate(mSyncInProgress);

                    setBackgroundText();
                }
                
            } catch (RuntimeException e) {
                // avoid app crashes after changing the serial id of RemoteOperationResult 
                // in owncloud library with broadcast notifications pending to process
                removeStickyBroadcast(intent);
            }
        }
    }

    

    /**
     * Shows the information of the {@link OCFile} received as a
     * parameter in the second fragment.
     *
     * @param file          {@link OCFile} whose details will be shown
     */
    @Override
    public void showDetails(OCFile file) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading) {
            
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
}
