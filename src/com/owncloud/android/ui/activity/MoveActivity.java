package com.owncloud.android.ui.activity;

import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
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
import com.owncloud.android.operations.SynchronizeFolderOperation;
import com.owncloud.android.services.observer.FileObserverService;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.MoveFileListFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.Log_OC;

public class MoveActivity extends HookActivity implements FileFragment.ContainerActivity, 
    OnNavigationListener, OnClickListener{
    
    private ArrayAdapter<String> mDirectories;
    
    private SyncBroadcastReceiver mSyncBroadcastReceiver;

    public static final int DIALOG_SHORT_WAIT = 0;
    
    public static final String ACTION_DETAILS = "com.owncloud.android.ui.activity.action.DETAILS";

    private static final String TAG = MoveActivity.class.getSimpleName();
    
    private static final String TAG_LIST_OF_FILES = "LIST_OF_FILES";
       
    private boolean mSyncInProgress = false;

    private Button mCancelBtn;
    private Button mChooseBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate() start");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState); // this calls onAccountChanged() when ownCloud Account is valid

        /// grant that FileObserverService is watching favourite files
        if (savedInstanceState == null) {
            Intent initObserversIntent = FileObserverService.makeInitIntent(this);
            startService(initObserversIntent);
        }

        /// USER INTERFACE

        // Inflate and set the layout view
        setContentView(R.layout.files_move);    
        if (savedInstanceState == null) {
            createMinFragments();
        }

        initControls();

        // Action bar setup
        mDirectories = new CustomArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item);
        getSupportActionBar().setHomeButtonEnabled(true);       // mandatory since Android ICS, according to the official documentation
        setSupportProgressBarIndeterminateVisibility(mSyncInProgress /*|| mRefreshSharesInProgress*/);    // always AFTER setContentView(...) ; to work around bug in its implementation
        
        setBackgroundText();

        Log_OC.d(TAG, "onCreate() end");
        
    }

    private void createMinFragments() {
        MoveFileListFragment listOfFiles = new MoveFileListFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.fragment_container, listOfFiles, TAG_LIST_OF_FILES);
        transaction.commit();
    }

    /**
     * Show a text message on screen view for notifying user if content is
     * loading or folder is empty
     */
    private void setBackgroundText() {
        MoveFileListFragment MoveFileListFragment = getListOfFilesFragment();
        if (MoveFileListFragment != null) {
            int message = R.string.file_list_loading;
            if (!mSyncInProgress) {
                // In case folder list is empty
                message = R.string.file_list_empty_moving;
            }
            MoveFileListFragment.setMessageForEmptyList(getString(message));
        } else {
            Log.e(TAG, "MoveFileListFragment is null");
        }
    }

    private MoveFileListFragment getListOfFilesFragment() {
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(MoveActivity.TAG_LIST_OF_FILES);
        if (listOfFiles != null) {
            return (MoveFileListFragment)listOfFiles;
        }
        Log_OC.wtf(TAG, "Access to unexisting list of files fragment!!");
        return null;
    }

    // Custom array adapter to override text colors
    private class CustomArrayAdapter<T> extends ArrayAdapter<T> {

        public CustomArrayAdapter(MoveActivity ctx, int view) {
            super(ctx, view);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);

            ((TextView) v).setTextColor(getResources().getColorStateList(
                    android.R.color.white));
            
            fixRoot((TextView) v );
            return v;
        }

        public View getDropDownView(int position, View convertView,
                ViewGroup parent) {
            View v = super.getDropDownView(position, convertView, parent);

            ((TextView) v).setTextColor(getResources().getColorStateList(
                    android.R.color.white));

            fixRoot((TextView) v );
            return v;
        }

        private void fixRoot(TextView v) {
            if (v.getText().equals(OCFile.PATH_SEPARATOR)) {
                v.setText(R.string.default_display_name_for_root_folder);
            }
        }

    }

    /**
     * {@inheritDoc}
     * 
     * Updates action bar and second fragment, if in dual pane mode.
     */
    @Override
    public void onBrowsedDownTo(OCFile directory) {
        pushDirname(directory);
        
        // Sync Folder
        startSyncFolderOperation(directory);
        
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

    /**
     * Pushes a directory to the drop down list
     * @param directory to push
     * @throws IllegalArgumentException If the {@link OCFile#isFolder()} returns false.
     */
    public void pushDirname(OCFile directory) {
        if(!directory.isFolder()){
            throw new IllegalArgumentException("Only directories may be pushed!");
        }
        mDirectories.insert(directory.getFileName(), 0);
        setFile(directory);
    }

    public void startSyncFolderOperation(OCFile folder) {
        long currentSyncTime = System.currentTimeMillis(); 
        
        mSyncInProgress = true;
                
        // perform folder synchronization
        RemoteOperation synchFolderOp = new SynchronizeFolderOperation( folder,  
                                                                        currentSyncTime, 
                                                                        false,
                                                                        getFileOperationsHelper().isSharedSupported(),
                                                                        getStorageManager(), 
                                                                        getAccount(), 
                                                                        getApplicationContext()
                                                                      );
        synchFolderOp.execute(getAccount(), this, null, null);
        
        setSupportProgressBarIndeterminateVisibility(true);

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
        syncIntentFilter.addAction(SynchronizeFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED);
        syncIntentFilter.addAction(SynchronizeFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED);
        mSyncBroadcastReceiver = new SyncBroadcastReceiver();
        registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);
        
        Log_OC.d(TAG, "onResume() end");
    }

    @Override
    protected void onStart() {
        super.onStart();
        getSupportActionBar().setIcon(DisplayUtils.getSeasonalIconId());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.action_upload).setVisible(false);
        menu.findItem(R.id.action_settings).setVisible(false);
        menu.findItem(R.id.action_sync_account).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
        case R.id.action_create_dir: {
            CreateFolderDialogFragment dialog = 
                    CreateFolderDialogFragment.newInstance(getCurrentDir());
            dialog.show(getSupportFragmentManager(), "createdirdialog");
            break;
        }
        case android.R.id.home: {
            OCFile currentDir = getCurrentDir();
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

    private OCFile getCurrentDir() {
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
        MoveFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) { 
            fileListFragment.listDirectory();
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
                RemoteOperationResult synchResult = (RemoteOperationResult)intent.getSerializableExtra(FileSyncAdapter.EXTRA_RESULT);
                boolean sameAccount = (getAccount() != null && accountName.equals(getAccount().name) && getStorageManager() != null); 
    
                if (sameAccount) {
                    
                    if (FileSyncAdapter.EVENT_FULL_SYNC_START.equals(event)) {
                        mSyncInProgress = true;
                        
                    } else {
                        OCFile currentFile = (getFile() == null) ? null : getStorageManager().getFileByPath(getFile().getRemotePath());
                        OCFile currentDir = (getCurrentDir() == null) ? null : getStorageManager().getFileByPath(getCurrentDir().getRemotePath());
    
                        if (currentDir == null) {
                            // current folder was removed from the server 
                            Toast.makeText( MoveActivity.this, 
                                            String.format(getString(R.string.sync_current_folder_was_removed), mDirectories.getItem(0)), 
                                            Toast.LENGTH_LONG)
                                .show();
                            browseToRoot();
                            
                        } else {
                            if (currentFile == null && !getFile().isFolder()) {
                                // currently selected file was removed in the server, and now we know it
                                currentFile = currentDir;
                            }

                            if (synchFolderRemotePath != null && currentDir.getRemotePath().equals(synchFolderRemotePath)) {
                                MoveFileListFragment fileListFragment = getListOfFilesFragment();
                                if (fileListFragment != null) {
                                    fileListFragment.listDirectory(currentDir);
                                }
                            }
                            setFile(currentFile);
                        }
                        
                        mSyncInProgress = (!FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) && !SynchronizeFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED.equals(event));
                                
                        if (SynchronizeFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.
                                    equals(event) &&
                                /// TODO refactor and make common
                                synchResult != null && !synchResult.isSuccess() &&  
                                (synchResult.getCode() == ResultCode.UNAUTHORIZED   || 
                                    synchResult.isIdPRedirection()                  ||
                                    (synchResult.isException() && synchResult.getException() 
                                            instanceof AuthenticatorException))) {

                            OwnCloudClient client = null;
                            try {
                                OwnCloudAccount ocAccount = 
                                        new OwnCloudAccount(getAccount(), context);
                                client = (OwnCloudClientManagerFactory.getDefaultSingleton().
                                        removeClientFor(ocAccount));
                                // TODO get rid of these exceptions
                            } catch (AccountNotFoundException e) {
                                e.printStackTrace();
                            } catch (AuthenticatorException e) {
                                e.printStackTrace();
                            } catch (OperationCanceledException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            
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
                            
                        }
                    }
                    removeStickyBroadcast(intent);
                    Log_OC.d(TAG, "Setting progress visibility to " + mSyncInProgress);
                    setSupportProgressBarIndeterminateVisibility(mSyncInProgress /*|| mRefreshSharesInProgress*/);

                    setBackgroundText();
                        
                }
                
            } catch (RuntimeException e) {
                // avoid app crashes after changing the serial id of RemoteOperationResult 
                // in owncloud library with broadcast notifications pending to process
                removeStickyBroadcast(intent);
            }
        }
    }

    public void browseToRoot() {
        MoveFileListFragment listOfFiles = getListOfFilesFragment(); 
        if (listOfFiles != null) {  // should never be null, indeed
            while (mDirectories.getCount() > 1) {
                popDirname();
            }
            OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            listOfFiles.listDirectory(root);
            setFile(listOfFiles.getCurrentFile());
            startSyncFolderOperation(root);
        }
    }

    /**
     * Pops a directory name from the drop down list
     * @return True, unless the stack is empty
     */
    public boolean popDirname() {
        mDirectories.remove(mDirectories.getItem(0));
        return !mDirectories.isEmpty();
    }

    private void setNavigationListWithFolder(OCFile file) {
        mDirectories.clear();
        OCFile fileIt = file;
        String parentPath;
        while(fileIt != null && fileIt.getFileName() != OCFile.ROOT_PATH) {
            if (fileIt.isFolder()) {
                mDirectories.add(fileIt.getFileName());
            }
            // get parent from path
            parentPath = fileIt.getRemotePath().substring(0, fileIt.getRemotePath().lastIndexOf(fileIt.getFileName()));
            fileIt = getStorageManager().getFileByPath(parentPath);
        }
        mDirectories.add(OCFile.PATH_SEPARATOR);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (itemPosition != 0) {
            String targetPath = "";
            for (int i=itemPosition; i < mDirectories.getCount() - 1; i++) {
                targetPath = mDirectories.getItem(i) + OCFile.PATH_SEPARATOR + targetPath; 
            }
            targetPath = OCFile.PATH_SEPARATOR + targetPath;
            OCFile targetFolder = getStorageManager().getFileByPath(targetPath);
            if (targetFolder != null) {
                browseTo(targetFolder);
            }

            // the next operation triggers a new call to this method, but it's necessary to 
            // ensure that the name exposed in the action bar is the current directory when the 
            // user selected it in the navigation list
            if (getSupportActionBar().getNavigationMode() == ActionBar.NAVIGATION_MODE_LIST  && itemPosition != 0)
                getSupportActionBar().setSelectedNavigationItem(0);
        }
        return true;
    }

    public void browseTo(OCFile folder) {
        if (folder == null || !folder.isFolder()) {
            throw new IllegalArgumentException("Trying to browse to invalid folder " + folder);
        }
        MoveFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {
            setNavigationListWithFolder(folder);
            listOfFiles.listDirectory(folder);
            setFile(listOfFiles.getCurrentFile());
            startSyncFolderOperation(folder);
        } else {
            Log_OC.e(TAG, "Unexpected null when accessing list fragment");
        }
    }

    @Override
    public void onBackPressed() {
        MoveFileListFragment listOfFiles = getListOfFilesFragment();
        if (listOfFiles != null) {  // should never be null, indeed
            if (mDirectories.getCount() <= 1) {
                finish();
                return;
            }
            int levelsUp = listOfFiles.onBrowseUp();
            for (int i=0; i < levelsUp && mDirectories.getCount() > 1 ; i++) {
                popDirname();
            }
        }
        if (listOfFiles != null) {  // should never be null, indeed
            setFile(listOfFiles.getCurrentFile());
        }
    }

    private void updateNavigationElementsInActionBar(OCFile chosenFile) {
        ActionBar actionBar = getSupportActionBar();
        if (chosenFile == null) {
            // only list of files - set for browsing through folders
            OCFile currentDir = getCurrentDir();
            boolean noRoot = (currentDir != null && currentDir.getParentId() != 0);
            actionBar.setDisplayHomeAsUpEnabled(noRoot);
            actionBar.setDisplayShowTitleEnabled(!noRoot);
            if (!noRoot) {
                actionBar.setTitle(getString(R.string.default_display_name_for_root_folder));
            }
            actionBar.setNavigationMode(!noRoot ? ActionBar.NAVIGATION_MODE_STANDARD : ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(mDirectories, this);   // assuming mDirectories is updated

        } else {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(chosenFile.getFileName());
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
    }

    /**
     *  Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {
            /// Check whether the 'main' OCFile handled by the Activity is contained in the current Account
            OCFile file = getFile();
            // get parent from path
            String parentPath = "";
            if (file != null) {
                if (file.isDown() && file.getLastSyncDateForProperties() == 0) {
                    // upload in progress - right now, files are not inserted in the local cache until the upload is successful
                    // get parent from path
                    parentPath = file.getRemotePath().substring(0, file.getRemotePath().lastIndexOf(file.getFileName()));
                    if (getStorageManager().getFileByPath(parentPath) ==  null)
                        file = null; // not able to know the directory where the file is uploading
                } else {
                    file = getStorageManager().getFileByPath(file.getRemotePath());   // currentDir = null if not in the current Account
                }
            }
            if (file == null) {
                // fall back to root folder
                file = getStorageManager().getFileByPath(OCFile.ROOT_PATH);  // never returns null
            }
            setFile(file);
            setNavigationListWithFolder(file);

            if (!stateWasRecovered) {
                Log_OC.e(TAG, "Initializing Fragments in onAccountChanged..");
                if (file.isFolder()) {
                    startSyncFolderOperation(file);
                }
            } else {
                updateNavigationElementsInActionBar(file.isFolder() ? null : file);
            }
        }
    }

    /**
     * Set controllers
     */
    private void initControls(){
        mCancelBtn = (Button) findViewById(R.id.move_files_btn_cancel);
        mCancelBtn.setOnClickListener(this);
        mChooseBtn = (Button) findViewById(R.id.move_files_btn_choose);
        mChooseBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mCancelBtn) {
            finish();
        }
    }
}
