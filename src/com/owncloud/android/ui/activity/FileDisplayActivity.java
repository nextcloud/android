/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

import java.io.File;
import java.io.IOException;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.owncloud.android.BuildConfig;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.OwnCloudCredentials;
import com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException;
import com.owncloud.android.lib.common.network.CertificateCombinedException;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CreateFolderOperation;
import com.owncloud.android.operations.CreateShareOperation;
import com.owncloud.android.operations.MoveFileOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.operations.UnshareLinkOperation;
import com.owncloud.android.services.observer.FileObserverService;
import com.owncloud.android.syncadapter.FileSyncAdapter;
import com.owncloud.android.ui.adapter.FileListListAdapter;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog.OnSslUntrustedCertListener;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewVideoActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ErrorMessageAdapter;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.UriUtils;


/**
 * Displays, what files the user has available in his ownCloud.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */

public class FileDisplayActivity extends HookActivity implements
FileFragment.ContainerActivity, OnNavigationListener, 
OnSslUntrustedCertListener, OnEnforceableRefreshListener {
    
    private ArrayAdapter<String> mDirectories;

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

    public static final int DIALOG_SHORT_WAIT = 0;
    private static final int DIALOG_CHOOSE_UPLOAD_SOURCE = 1;
    private static final int DIALOG_CERT_NOT_SAVED = 2;
    
    public static final String ACTION_DETAILS = "com.owncloud.android.ui.activity.action.DETAILS";

    private static final int ACTION_SELECT_CONTENT_FROM_APPS = 1;
    private static final int ACTION_SELECT_MULTIPLE_FILES = 2;
    public static final int ACTION_MOVE_FILES = 3;

    private static final String TAG = FileDisplayActivity.class.getSimpleName();

    private static final String TAG_LIST_OF_FILES = "LIST_OF_FILES";
    private static final String TAG_SECOND_FRAGMENT = "SECOND_FRAGMENT";

    private OCFile mWaitingToPreview;
    
    private boolean mSyncInProgress = false;

    private String DIALOG_UNTRUSTED_CERT;
    
    private OCFile mWaitingToSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreate() start");
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState); // this calls onAccountChanged() when ownCloud Account is valid

        // PIN CODE request ;  best location is to decide, let's try this first
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_MAIN) && savedInstanceState == null) {
            requestPinCode();
        } else if (getIntent().getAction() == null && savedInstanceState == null) {
            requestPinCode();
        }

        /// grant that FileObserverService is watching favourite files
        if (savedInstanceState == null) {
            Intent initObserversIntent = FileObserverService.makeInitIntent(this);
            startService(initObserversIntent);
        }
        
        /// Load of saved instance state
        if(savedInstanceState != null) {
            mWaitingToPreview = (OCFile) savedInstanceState.getParcelable(FileDisplayActivity.KEY_WAITING_TO_PREVIEW);
            mSyncInProgress = savedInstanceState.getBoolean(KEY_SYNC_IN_PROGRESS);
            mWaitingToSend = (OCFile) savedInstanceState.getParcelable(FileDisplayActivity.KEY_WAITING_TO_SEND);
           
        } else {
            mWaitingToPreview = null;
            mSyncInProgress = false;
            mWaitingToSend = null;
        }        

        /// USER INTERFACE

        // Inflate and set the layout view
        setContentView(R.layout.files);    
        mDualPane = getResources().getBoolean(R.bool.large_land_layout);
        mLeftFragmentContainer = findViewById(R.id.left_fragment_container);
        mRightFragmentContainer = findViewById(R.id.right_fragment_container);
        if (savedInstanceState == null) {
            createMinFragments();
        }

        // Action bar setup
        mDirectories = new CustomArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item);
        getSupportActionBar().setHomeButtonEnabled(true);       // mandatory since Android ICS, according to the official documentation
        setSupportProgressBarIndeterminateVisibility(mSyncInProgress /*|| mRefreshSharesInProgress*/);    // always AFTER setContentView(...) ; to work around bug in its implementation
        
        setBackgroundText();

        Log_OC.d(TAG, "onCreate() end");
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
                Log_OC.d(TAG, "Initializing Fragments in onAccountChanged..");
                initFragmentsWithFile();
                if (file.isFolder()) {
                    startSyncFolderOperation(file, false);
                }
                
            } else {
                updateFragmentsVisibility(!file.isFolder());
                updateNavigationElementsInActionBar(file.isFolder() ? null : file);
            }
        }
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


    private void createMinFragments() {
        OCFileListFragment listOfFiles = new OCFileListFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.left_fragment_container, listOfFiles, TAG_LIST_OF_FILES);
        transaction.commit();
    }
    
    private void initFragmentsWithFile() {
        if (getAccount() != null && getFile() != null) {
            /// First fragment
            OCFileListFragment listOfFiles = getListOfFilesFragment(); 
            if (listOfFiles != null) {
                listOfFiles.listDirectory(getCurrentDir());   
            } else {
                Log_OC.e(TAG, "Still have a chance to lose the initializacion of list fragment >(");
            }
            
            /// Second fragment
            OCFile file = getFile(); 
            Fragment secondFragment = chooseInitialSecondFragment(file);
            if (secondFragment != null) {
                setSecondFragment(secondFragment);
                updateFragmentsVisibility(true);
                updateNavigationElementsInActionBar(file);
                
            } else {
                cleanSecondFragment();
            }

        } else {
            Log_OC.wtf(TAG, "initFragments() called with invalid NULLs!");
            if (getAccount() == null) {
                Log_OC.wtf(TAG, "\t account is NULL");
            }
            if (getFile() == null) {
                Log_OC.wtf(TAG, "\t file is NULL");
            }
        }
    }

    private Fragment chooseInitialSecondFragment(OCFile file) {
        Fragment secondFragment = null;
        if (file != null && !file.isFolder()) {
            if (file.isDown() && PreviewMediaFragment.canBePreviewed(file) 
                    && file.getLastSyncDateForProperties() > 0  // temporal fix
                    ) {
                int startPlaybackPosition = getIntent().getIntExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0);
                boolean autoplay = getIntent().getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, true);
                secondFragment = new PreviewMediaFragment(file, getAccount(), startPlaybackPosition, autoplay);

            } else {
                secondFragment = new FileDetailFragment(file, getAccount());
            }
        }
        return secondFragment;
    }


    /**
     * Replaces the second fragment managed by the activity with the received as
     * a parameter.
     * 
     * Assumes never will be more than two fragments managed at the same time. 
     * 
     * @param fragment      New second Fragment to set.
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
        Fragment listOfFiles = getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES);
        if (listOfFiles != null) {
            return (OCFileListFragment)listOfFiles;
        }
        Log_OC.wtf(TAG, "Access to unexisting list of files fragment!!");
        return null;
    }

    public FileFragment getSecondFragment() {
        Fragment second = getSupportFragmentManager().findFragmentByTag(FileDisplayActivity.TAG_SECOND_FRAGMENT);
        if (second != null) {
            return (FileFragment)second;
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
        updateNavigationElementsInActionBar(null);
    }

    protected void refreshListOfFilesFragment() {
        OCFileListFragment fileListFragment = getListOfFilesFragment();
        if (fileListFragment != null) { 
            fileListFragment.listDirectory();
        }
    }

    protected void refreshSecondFragment(String downloadEvent, String downloadedRemotePath, boolean success) {
        FileFragment secondFragment = getSecondFragment();
        boolean waitedPreview = (mWaitingToPreview != null && mWaitingToPreview.getRemotePath().equals(downloadedRemotePath));
        if (secondFragment != null && secondFragment instanceof FileDetailFragment) {
            FileDetailFragment detailsFragment = (FileDetailFragment) secondFragment;
            OCFile fileInFragment = detailsFragment.getFile();
            if (fileInFragment != null && !downloadedRemotePath.equals(fileInFragment.getRemotePath())) {
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
                        mWaitingToPreview = getStorageManager().getFileById(mWaitingToPreview.getFileId());   // update the file from database, for the local storage path
                        if (PreviewMediaFragment.canBePreviewed(mWaitingToPreview)) {
                            startMediaPreview(mWaitingToPreview, 0, true);
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
        if (BuildConfig.DEBUG) {
            menu.findItem(R.id.action_logger).setVisible(true);
        } else {
            menu.findItem(R.id.action_logger).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
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
        case R.id.action_sync_account: {
            startSynchronization();
            break;
        }
        case R.id.action_upload: {
            showDialog(DIALOG_CHOOSE_UPLOAD_SOURCE);
            break;
        }
        case R.id.action_settings: {
            Intent settingsIntent = new Intent(this, Preferences.class);
            startActivity(settingsIntent);
            break;
        }
        case R.id.action_logger: {
            Intent loggerIntent = new Intent(getApplicationContext(),LogHistoryActivity.class);
            startActivity(loggerIntent);
            break;
        }
        case android.R.id.home: {
            FileFragment second = getSecondFragment();
            OCFile currentDir = getCurrentDir();
            if((currentDir != null && currentDir.getParentId() != 0) || 
                    (second != null && second.getFile() != null)) {                
                onBackPressed(); 
                
            }
            break;
        }
        case R.id.action_sort: {
            SharedPreferences appPreferences = PreferenceManager
                    .getDefaultSharedPreferences(this);
            
            // Read sorting order, default to sort by name ascending
            Integer sortOrder = appPreferences
                    .getInt("sortOrder", FileStorageUtils.SORT_NAME);
            
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionbar_sort_title)
            .setSingleChoiceItems(R.array.actionbar_sortby, sortOrder , new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    
                    switch (which){
                    case 0:
                        sortByName(true);
                        break;
                    case 1:
                        sortByDate(false);
                        break;
                        
// TODO re-enable when server-side folder size calculation is available                       
//                    case 2:
//                        sortBySize(false);
//                        break;
                    }
                    
                    dialog.dismiss();
                    
                }
            });
            builder.create().show();
            break;
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
            ContentResolver.cancelSync(null, MainApp.getAuthority());   // cancel the current synchronizations of any ownCloud account
            Bundle bundle = new Bundle();
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
            Log_OC.d(TAG, "Requesting sync for " + getAccount().name + " at " + MainApp.getAuthority());
            ContentResolver.requestSync(
                    getAccount(),
                    MainApp.getAuthority(), bundle);
        } else {
            Log_OC.d(TAG, "Requesting sync for " + getAccount().name + " at " + MainApp.getAuthority() + " with new API");
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

    /**
     * Called, when the user selected something for uploading
     *
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_SELECT_CONTENT_FROM_APPS && (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)) {
            //getClipData is only supported on api level 16+, Jelly Bean
            if (data.getData() == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN){
                for( int i = 0; i < data.getClipData().getItemCount(); i++){
                    Intent intent = new Intent();
                    intent.setData(data.getClipData().getItemAt(i).getUri());
                    requestSimpleUpload(intent, resultCode);
                }
            }else {
                requestSimpleUpload(data, resultCode);
            }
        } else if (requestCode == ACTION_SELECT_MULTIPLE_FILES && (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)) {
            requestMultipleUpload(data, resultCode);

        } else if (requestCode == ACTION_MOVE_FILES && resultCode == RESULT_OK){

            final Intent fData = data;
            final int fResultCode = resultCode; 
            getHandler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        requestMoveOperation(fData, fResultCode);
                    }
                }, 
                DELAY_TO_REQUEST_OPERATION_ON_ACTIVITY_RESULTS
            );
        }
    }

    private void requestMultipleUpload(Intent data, int resultCode) {
        String[] filePaths = data.getStringArrayExtra(UploadFilesActivity.EXTRA_CHOSEN_FILES);
        if (filePaths != null) {
            String[] remotePaths = new String[filePaths.length];
            String remotePathBase = "";
            for (int j = mDirectories.getCount() - 2; j >= 0; --j) {
                remotePathBase += OCFile.PATH_SEPARATOR + mDirectories.getItem(j);
            }
            if (!remotePathBase.endsWith(OCFile.PATH_SEPARATOR))
                remotePathBase += OCFile.PATH_SEPARATOR;
            for (int j = 0; j< remotePaths.length; j++) {
                remotePaths[j] = remotePathBase + (new File(filePaths[j])).getName();
            }

            Intent i = new Intent(this, FileUploader.class);
            i.putExtra(FileUploader.KEY_ACCOUNT, getAccount());
            i.putExtra(FileUploader.KEY_LOCAL_FILE, filePaths);
            i.putExtra(FileUploader.KEY_REMOTE_FILE, remotePaths);
            i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_MULTIPLE_FILES);
            if (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)
                i.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_MOVE);
            startService(i);

        } else {
            Log_OC.d(TAG, "User clicked on 'Update' with no selection");
            Toast t = Toast.makeText(this, getString(R.string.filedisplay_no_file_selected), Toast.LENGTH_LONG);
            t.show();
            return;
        }
    }


    private void requestSimpleUpload(Intent data, int resultCode) {
        String filepath = null;
        String mimeType = null;

        Uri selectedImageUri = data.getData();

        try {
            mimeType = getContentResolver().getType(selectedImageUri);

            String filemanagerstring = selectedImageUri.getPath();
            String selectedImagePath = getPath(selectedImageUri);

            if (selectedImagePath != null)
                filepath = selectedImagePath;
            else
                filepath = filemanagerstring;

        } catch (Exception e) {
            Log_OC.e(TAG, "Unexpected exception when trying to read the result of Intent.ACTION_GET_CONTENT", e);
            e.printStackTrace();

        } finally {
            if (filepath == null) {
                Log_OC.e(TAG, "Couldn't resolve path to file");
                Toast t = Toast.makeText(this, getString(R.string.filedisplay_unexpected_bad_get_content), Toast.LENGTH_LONG);
                t.show();
                return;
            }
        }

        Intent i = new Intent(this, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT,
                getAccount());
        String remotepath = new String();
        for (int j = mDirectories.getCount() - 2; j >= 0; --j) {
            remotepath += OCFile.PATH_SEPARATOR + mDirectories.getItem(j);
        }
        if (!remotepath.endsWith(OCFile.PATH_SEPARATOR))
            remotepath += OCFile.PATH_SEPARATOR;

        if (filepath.startsWith(UriUtils.URI_CONTENT_SCHEME)) {

            Cursor cursor = MainApp.getAppContext().getContentResolver()
                    .query(Uri.parse(filepath), null, null, null, null, null);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    String displayName = cursor.getString(
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    Log.i(TAG, "Display Name: " + displayName + "; mimeType: " + mimeType);

                    displayName.replace(File.separatorChar, '_');
                    displayName.replace(File.pathSeparatorChar, '_');
                    remotepath += displayName + DisplayUtils.getComposedFileExtension(filepath);

                }
            } finally {
                cursor.close();
            }

        } else {
            remotepath += new File(filepath).getName();
        }

        i.putExtra(FileUploader.KEY_LOCAL_FILE, filepath);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, remotepath);
        i.putExtra(FileUploader.KEY_MIME_TYPE, mimeType);
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        if (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)
            i.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_MOVE);
        startService(i);
    }

    /**
     * Request the operation for moving the file/folder from one path to another
     * 
     * @param data              Intent received
     * @param resultCode        Result code received
     */
    private void requestMoveOperation(Intent data, int resultCode) {
        OCFile folderToMoveAt = (OCFile) data.getParcelableExtra(FolderPickerActivity.EXTRA_FOLDER);
        OCFile targetFile = (OCFile) data.getParcelableExtra(FolderPickerActivity.EXTRA_FILE);
        getFileOperationsHelper().moveFile(folderToMoveAt, targetFile);
    }

    @Override
    public void onBackPressed() {
        OCFileListFragment listOfFiles = getListOfFilesFragment(); 
        if (mDualPane || getSecondFragment() == null) {
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
        }
        if (listOfFiles != null) {  // should never be null, indeed
            setFile(listOfFiles.getCurrentFile());
        }
        cleanSecondFragment();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // responsibility of restore is preferred in onCreate() before than in onRestoreInstanceState when there are Fragments involved
        Log_OC.d(TAG, "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDisplayActivity.KEY_WAITING_TO_PREVIEW, mWaitingToPreview);
        outState.putBoolean(FileDisplayActivity.KEY_SYNC_IN_PROGRESS, mSyncInProgress);
        //outState.putBoolean(FileDisplayActivity.KEY_REFRESH_SHARES_IN_PROGRESS, mRefreshSharesInProgress);
        outState.putParcelable(FileDisplayActivity.KEY_WAITING_TO_SEND, mWaitingToSend);

        Log_OC.d(TAG, "onSaveInstanceState() end");
    }
    


    @Override
    protected void onResume() {
        super.onResume();
        Log_OC.d(TAG, "onResume() start");
        
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
        //LocalBroadcastManager.getInstance(this).registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);

        // Listen for upload messages
        IntentFilter uploadIntentFilter = new IntentFilter(FileUploader.getUploadFinishMessage());
        mUploadFinishReceiver = new UploadFinishReceiver();
        registerReceiver(mUploadFinishReceiver, uploadIntentFilter);

        // Listen for download messages
        IntentFilter downloadIntentFilter = new IntentFilter(FileDownloader.getDownloadAddedMessage());
        downloadIntentFilter.addAction(FileDownloader.getDownloadFinishMessage());
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        registerReceiver(mDownloadFinishReceiver, downloadIntentFilter);
        
        Log_OC.d(TAG, "onResume() end");
    }


    @Override
    protected void onPause() {
        Log_OC.d(TAG, "onPause() start");
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
        
        
        Log_OC.d(TAG, "onPause() end");
        super.onPause();
    }


    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder;
        switch (id) {
        case DIALOG_SHORT_WAIT: {
            ProgressDialog working_dialog = new ProgressDialog(this);
            working_dialog.setMessage(getResources().getString(
                    R.string.wait_a_moment));
            working_dialog.setIndeterminate(true);
            working_dialog.setCancelable(false);
            dialog = working_dialog;
            break;
        }
        case DIALOG_CHOOSE_UPLOAD_SOURCE: {


            String[] allTheItems = { getString(R.string.actionbar_upload_files),
                    getString(R.string.actionbar_upload_from_apps) };

            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionbar_upload);
            builder.setItems(allTheItems, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0) {
                        // if (!mDualPane) {
                            Intent action = new Intent(FileDisplayActivity.this, UploadFilesActivity.class);
                            action.putExtra(UploadFilesActivity.EXTRA_ACCOUNT, FileDisplayActivity.this.getAccount());
                            startActivityForResult(action, ACTION_SELECT_MULTIPLE_FILES);
                            // } else {
                            // TODO create and handle new fragment
                            // LocalFileListFragment
                            // }
                    } else if (item == 1) {
                        Intent action = new Intent(Intent.ACTION_GET_CONTENT);
                        action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                        //Intent.EXTRA_ALLOW_MULTIPLE is only supported on api level 18+, Jelly Bean
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                            action.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        }
                        startActivityForResult(Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                                ACTION_SELECT_CONTENT_FROM_APPS);
                    }
                }
            });
            dialog = builder.create();
            break;
        }
        case DIALOG_CERT_NOT_SAVED: {
            builder = new AlertDialog.Builder(this);
            builder.setMessage(getResources().getString(R.string.ssl_validator_not_saved));
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.common_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                };
            });
            dialog = builder.create();
            break;
        }
        default:
            dialog = null;
        }

        return dialog;
    }

    /**
     * Translates a content URI of an content to a physical path on the disk
     * 
     * @param uri The URI to resolve
     * @return The path to the content or null if it could not be found
     */
    public String getPath(Uri uri) {
        final boolean isKitKatOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKatOrLater && DocumentsContract.isDocumentUri(getApplicationContext(), uri)) {
            // ExternalStorageProvider
            if (UriUtils.isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (UriUtils.isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return UriUtils.getDataColumn(getApplicationContext(), contentUri, null, null);
            }
            // MediaProvider
            else if (UriUtils.isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };

                return UriUtils.getDataColumn(getApplicationContext(), contentUri, selection, selectionArgs);
            }
            // Documents providers returned as content://...
            else if (UriUtils.isContentDocument(uri)) {
                return uri.toString();
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (UriUtils.isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return UriUtils.getDataColumn(getApplicationContext(), uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
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

    /**
     * Pops a directory name from the drop down list
     * @return True, unless the stack is empty
     */
    public boolean popDirname() {
        mDirectories.remove(mDirectories.getItem(0));
        return !mDirectories.isEmpty();
    }

    // Custom array adapter to override text colors
    private class CustomArrayAdapter<T> extends ArrayAdapter<T> {

        public CustomArrayAdapter(FileDisplayActivity ctx, int view) {
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
                            Toast.makeText( FileDisplayActivity.this, 
                                            String.format(getString(R.string.sync_current_folder_was_removed), mDirectories.getItem(0)), 
                                            Toast.LENGTH_LONG)
                                .show();
                            browseToRoot();
                            
                        } else {
                            if (currentFile == null && !getFile().isFolder()) {
                                // currently selected file was removed in the server, and now we know it
                                cleanSecondFragment();
                                currentFile = currentDir;
                            }

                            if (synchFolderRemotePath != null && currentDir.getRemotePath().equals(synchFolderRemotePath)) {
                                OCFileListFragment fileListFragment = getListOfFilesFragment();
                                if (fileListFragment != null) {
                                    fileListFragment.listDirectory(currentDir);
                                }
                            }
                            setFile(currentFile);
                        }
                        
                        mSyncInProgress = (!FileSyncAdapter.EVENT_FULL_SYNC_END.equals(event) && !RefreshFolderOperation.EVENT_SINGLE_FOLDER_SHARES_SYNCED.equals(event));
                                
                        if (RefreshFolderOperation.EVENT_SINGLE_FOLDER_CONTENTS_SYNCED.
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
                
                if (synchResult != null) {
                    if (synchResult.getCode().equals(RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED)) {
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
         *  @author David A. Velasco
         * {@link BroadcastReceiver} to enable upload feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String uploadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
                String accountName = intent.getStringExtra(FileUploader.ACCOUNT_NAME);
                boolean sameAccount = getAccount() != null && accountName.equals(getAccount().name);
                OCFile currentDir = getCurrentDir();
                boolean isDescendant = (currentDir != null) && (uploadedRemotePath != null) && 
                        (uploadedRemotePath.startsWith(currentDir.getRemotePath()));
                
                if (sameAccount && isDescendant) {
                    refreshListOfFilesFragment();
                }
                
                boolean uploadWasFine = intent.getBooleanExtra(FileUploader.EXTRA_UPLOAD_RESULT, false);
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
                        ((FileDetailFragment)details).updateFileDetails(false, true);
                    } else {
                        cleanSecondFragment();
                    }
                    
                    // Force the preview if the file is an image
                    if (uploadWasFine && PreviewImageFragment.canBePreviewed(getFile())) {
                        startImagePreview(getFile());
                    } // TODO what about other kind of previews?
                }
                
            } finally {
                if (intent != null) {
                    removeStickyBroadcast(intent);
                }
            }
            
        }
        
    }


    /**
     * Class waiting for broadcast events from the {@link FileDownloader} service.
     * 
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * current folder.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {

        //int refreshCounter = 0;
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                boolean sameAccount = isSameAccount(context, intent);
                String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
                boolean isDescendant = isDescendant(downloadedRemotePath);

                if (sameAccount && isDescendant) {
                    String linkedToRemotePath = intent.getStringExtra(FileDownloader.EXTRA_LINKED_TO_PATH);
                    if (linkedToRemotePath == null || isAscendant(linkedToRemotePath)) {
                        //Log_OC.v(TAG, "refresh #" + ++refreshCounter);
                        refreshListOfFilesFragment();
                    }
                    refreshSecondFragment(
                            intent.getAction(),
                            downloadedRemotePath,
                            intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false)
                    );
                }
    
                if (mWaitingToSend != null) {
                    mWaitingToSend = getStorageManager().getFileByPath(mWaitingToSend.getRemotePath());
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

        private boolean isSameAccount(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);
            return (accountName != null && getAccount() != null && accountName.equals(getAccount().name));
        }
    }
    
    
    public void browseToRoot() {
        OCFileListFragment listOfFiles = getListOfFilesFragment(); 
        if (listOfFiles != null) {  // should never be null, indeed
            while (mDirectories.getCount() > 1) {
                popDirname();
            }
            OCFile root = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
            listOfFiles.listDirectory(root);
            setFile(listOfFiles.getCurrentFile());
            startSyncFolderOperation(root, false);
        }
        cleanSecondFragment();
    }
    
    
    public void browseTo(OCFile folder) {
        if (folder == null || !folder.isFolder()) {
            throw new IllegalArgumentException("Trying to browse to invalid folder " + folder);
        }
        OCFileListFragment listOfFiles = getListOfFilesFragment(); 
        if (listOfFiles != null) {
            setNavigationListWithFolder(folder);
            listOfFiles.listDirectory(folder);
            setFile(listOfFiles.getCurrentFile());
            startSyncFolderOperation(folder, false);
        } else {
            Log_OC.e(TAG, "Unexpected null when accessing list fragment");
        }
        cleanSecondFragment();
    }


    /**
     * {@inheritDoc}
     * 
     * Updates action bar and second fragment, if in dual pane mode.
     */
    @Override
    public void onBrowsedDownTo(OCFile directory) {
        pushDirname(directory);
        cleanSecondFragment();
        
        // Sync Folder
        startSyncFolderOperation(directory, false);
        
    }

    /**
     * Shows the information of the {@link OCFile} received as a 
     * parameter in the second fragment.
     * 
     * @param file          {@link OCFile} whose details will be shown
     */
    @Override
    public void showDetails(OCFile file) {
        Fragment detailFragment = new FileDetailFragment(file, getAccount());
        setSecondFragment(detailFragment);
        updateFragmentsVisibility(true);
        updateNavigationElementsInActionBar(file);
        setFile(file);
    }


    /**
     * TODO
     */
    private void updateNavigationElementsInActionBar(OCFile chosenFile) {
        ActionBar actionBar = getSupportActionBar(); 
        if (chosenFile == null || mDualPane) {
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


    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new ListServiceConnection();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private class ListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(new ComponentName(FileDisplayActivity.this, FileDownloader.class))) {
                Log_OC.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mWaitingToPreview != null)
                    if (getStorageManager() != null) {
                        mWaitingToPreview = getStorageManager().getFileById(mWaitingToPreview.getFileId()); // update the file
                        if (!mWaitingToPreview.isDown()) {
                            requestForDownload();
                        }
                }

            } else if (component.equals(new ComponentName(FileDisplayActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            // a new chance to get the mDownloadBinder through getFileDownloadBinder() - THIS IS A MESS
            OCFileListFragment listOfFiles = getListOfFilesFragment(); 
            if (listOfFiles != null) {
                listOfFiles.listDirectory();
            }
            FileFragment secondFragment = getSecondFragment();
            if (secondFragment != null && secondFragment instanceof FileDetailFragment) {
                FileDetailFragment detailFragment = (FileDetailFragment)secondFragment;
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
    };    



    /**
     * Launch an intent to request the PIN code to the user before letting him use the app
     */
    private void requestPinCode() {
        boolean pinStart = false;
        SharedPreferences appPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        pinStart = appPrefs.getBoolean("set_pincode", false);
        if (pinStart) {
            Intent i = new Intent(getApplicationContext(), PinCodeActivity.class);
            i.putExtra(PinCodeActivity.EXTRA_ACTIVITY, "FileDisplayActivity");
            startActivity(i);
        }
    }


    @Override
    public void onSavedCertificate() {
        startSyncFolderOperation(getCurrentDir(), false);
    }


    @Override
    public void onFailedSavingCertificate() {
        showDialog(DIALOG_CERT_NOT_SAVED);
    }

    @Override
    public void onCancelCertificate() {
        // nothing to do
    }

    /**
     * Updates the view associated to the activity after the finish of some operation over files
     * in the current account.
     * 
     * @param operation     Removal operation performed.
     * @param result        Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);
        
        if (operation instanceof RemoveFileOperation) {
            onRemoveFileOperationFinish((RemoveFileOperation)operation, result);

        } else if (operation instanceof RenameFileOperation) {
            onRenameFileOperationFinish((RenameFileOperation)operation, result);

        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation)operation, result);

        } else if (operation instanceof CreateFolderOperation) {
            onCreateFolderOperationFinish((CreateFolderOperation)operation, result);
            
        } else if (operation instanceof CreateShareOperation) {
            onCreateShareOperationFinish((CreateShareOperation) operation, result);
            
        } else if (operation instanceof UnshareLinkOperation) {
            onUnshareLinkOperationFinish((UnshareLinkOperation)operation, result);
        
        } else if (operation instanceof MoveFileOperation) {
            onMoveFileOperationFinish((MoveFileOperation)operation, result);
        }
        
    }

    
    private void onCreateShareOperationFinish(CreateShareOperation operation, RemoteOperationResult result) {
        if (result.isSuccess()) {
            refreshShowDetails();
            refreshListOfFilesFragment();
        }
    }

    
    private void onUnshareLinkOperationFinish(UnshareLinkOperation operation, RemoteOperationResult result) {
        if (result.isSuccess()) {
            refreshShowDetails();
            refreshListOfFilesFragment();
            
        } else if (result.getCode() == ResultCode.SHARE_NOT_FOUND) {
            cleanSecondFragment();
            refreshListOfFilesFragment();
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
                } else {
                    showDetails(file);
                } 
            }
            invalidateOptionsMenu();
        } 
    }
    
    /**
     * Updates the view associated to the activity after the finish of an operation trying to remove a 
     * file. 
     * 
     * @param operation     Removal operation performed.
     * @param result        Result of the removal.
     */
    private void onRemoveFileOperationFinish(RemoveFileOperation operation, RemoteOperationResult result) {
        dismissLoadingDialog();
        
        Toast msg = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                Toast.LENGTH_LONG); 
        msg.show();
        
        if (result.isSuccess()) {
            OCFile removedFile = operation.getFile();
            FileFragment second = getSecondFragment();
            if (second != null && removedFile.equals(second.getFile())) {
                if (second instanceof PreviewMediaFragment) {
                    ((PreviewMediaFragment)second).stopPreview(true);
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
     * @param operation     Move operation performed.
     * @param result        Result of the move operation.
     */
    private void onMoveFileOperationFinish(MoveFileOperation operation, RemoteOperationResult result) {
        if (result.isSuccess()) {
            dismissLoadingDialog();
            refreshListOfFilesFragment();
        } else {
            dismissLoadingDialog();
            try {
                Toast msg = Toast.makeText(FileDisplayActivity.this, 
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                        Toast.LENGTH_LONG); 
                msg.show();

            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message " , e);
            }
        }
    }


    /**
     * Updates the view associated to the activity after the finish of an operation trying to rename a 
     * file. 
     * 
     * @param operation     Renaming operation performed.
     * @param result        Result of the renaming.
     */
    private void onRenameFileOperationFinish(RenameFileOperation operation, RemoteOperationResult result) {
        dismissLoadingDialog();
        OCFile renamedFile = operation.getFile();
        if (result.isSuccess()) {
            FileFragment details = getSecondFragment();
            if (details != null) {
                if (details instanceof FileDetailFragment && renamedFile.equals(details.getFile()) ) {
                    ((FileDetailFragment) details).updateFileDetails(renamedFile, getAccount());
                    showDetails(renamedFile);

                } else if (details instanceof PreviewMediaFragment && renamedFile.equals(details.getFile())) {
                    ((PreviewMediaFragment) details).updateFile(renamedFile);
                    if (PreviewMediaFragment.canBePreviewed(renamedFile)) {
                        int position = ((PreviewMediaFragment)details).getPosition();
                        startMediaPreview(renamedFile, position, true);
                    } else {
                        getFileOperationsHelper().openFile(renamedFile);
                    }
                }
            }
            
            if (getStorageManager().getFileById(renamedFile.getParentId()).equals(getCurrentDir())) {
                refreshListOfFilesFragment();
            }

        } else {
            Toast msg = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                    Toast.LENGTH_LONG); 
            msg.show();
            
            if (result.isSslRecoverableException()) {
                mLastSslUntrustedServerResult = result;
                showUntrustedCertDialog(mLastSslUntrustedServerResult);
            }
        }
    }

    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation, RemoteOperationResult result) {
        dismissLoadingDialog();
        OCFile syncedFile = operation.getLocalFile();
        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                Intent i = new Intent(this, ConflictsResolveActivity.class);
                i.putExtra(ConflictsResolveActivity.EXTRA_FILE, syncedFile);
                i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, getAccount());
                startActivity(i);

            } 
            
        } else {
            if (operation.transferWasRequested()) {
                onTransferStateChanged(syncedFile, true, true);
                
            } else {
                Toast msg = Toast.makeText(this, ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                        Toast.LENGTH_LONG); 
                msg.show();
            }
        }
    }

    /**
     * Updates the view associated to the activity after the finish of an operation trying create a new folder
     * 
     * @param operation     Creation operation performed.
     * @param result        Result of the creation.
     */
    private void onCreateFolderOperationFinish(CreateFolderOperation operation, RemoteOperationResult result) {
        if (result.isSuccess()) {
            dismissLoadingDialog();
            refreshListOfFilesFragment();
        } else {
            dismissLoadingDialog();
            try {
                Toast msg = Toast.makeText(FileDisplayActivity.this, 
                        ErrorMessageAdapter.getErrorCauseMessage(result, operation, getResources()), 
                        Toast.LENGTH_LONG); 
                msg.show();

            } catch (NotFoundException e) {
                Log_OC.e(TAG, "Error while trying to show fail message " , e);
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
        if (details != null && details instanceof FileDetailFragment && file.equals(details.getFile()) ) {
            if (downloading || uploading) {
                ((FileDetailFragment)details).updateFileDetails(file, getAccount());
            } else {
                if (!file.fileExists()) {
                    cleanSecondFragment();
                } else {
                    ((FileDetailFragment)details).updateFileDetails(false, true);
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
        
        setSupportProgressBarIndeterminateVisibility(true);

        setBackgroundText();
    }

    /**
     * Show untrusted cert dialog 
     */
    public void showUntrustedCertDialog(RemoteOperationResult result) {
        // Show a dialog with the certificate info
        SslUntrustedCertDialog dialog = SslUntrustedCertDialog.newInstanceForFullSslError((CertificateCombinedException)result.getException());
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        dialog.show(ft, DIALOG_UNTRUSTED_CERT);
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
    
    private void sendDownloadedFile(){
        getFileOperationsHelper().sendDownloadedFile(mWaitingToSend);
        mWaitingToSend = null;
    }

    
    /**
     * Requests the download of the received {@link OCFile} , updates the UI
     * to monitor the download progress and prepares the activity to send the file
     * when the download finishes.
     * 
     * @param file          {@link OCFile} to download and preview.
     */
    public void startDownloadForSending(OCFile file) {
        mWaitingToSend = file;
        requestForDownload(mWaitingToSend);
        boolean hasSecondFragment = (getSecondFragment()!= null);
        updateFragmentsVisibility(hasSecondFragment);
    }
    
    /**
     * Opens the image gallery showing the image {@link OCFile} received as parameter.
     * 
     * @param file                      Image {@link OCFile} to show.
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
     * @param file                      Media {@link OCFile} to preview.
     * @param startPlaybackPosition     Media position where the playback will be started, in milliseconds.
     * @param autoplay                  When 'true', the playback will start without user interactions.
     */
    public void startMediaPreview(OCFile file, int startPlaybackPosition, boolean autoplay) {
        Fragment mediaFragment = new PreviewMediaFragment(file, getAccount(), startPlaybackPosition, autoplay);
        setSecondFragment(mediaFragment);
        updateFragmentsVisibility(true);
        updateNavigationElementsInActionBar(file);
        setFile(file);
    }

    /**
     * Requests the download of the received {@link OCFile} , updates the UI
     * to monitor the download progress and prepares the activity to preview
     * or open the file when the download finishes.
     * 
     * @param file          {@link OCFile} to download and preview.
     */
    public void startDownloadForPreview(OCFile file) {
        Fragment detailFragment = new FileDetailFragment(file, getAccount());
        setSecondFragment(detailFragment);
        mWaitingToPreview = file;
        requestForDownload();
        updateFragmentsVisibility(true);
        updateNavigationElementsInActionBar(file);
        setFile(file);
    }


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

    private void sortByDate(boolean ascending){
        getListOfFilesFragment().sortByDate(ascending);
    }

    private void sortBySize(boolean ascending){
        getListOfFilesFragment().sortBySize(ascending);
    }

    private void sortByName(boolean ascending){
        getListOfFilesFragment().sortByName(ascending);
    }
}
