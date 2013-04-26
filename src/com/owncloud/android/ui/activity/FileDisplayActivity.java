/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

import android.accounts.Account;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.owncloud.android.AccountUtils;
import com.owncloud.android.Log_OC;
import com.owncloud.android.R;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileObserverService;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.OnRemoteOperationListener;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.syncadapter.FileSyncService;
import com.owncloud.android.ui.dialog.EditNameDialog;
import com.owncloud.android.ui.dialog.SslValidatorDialog;
import com.owncloud.android.ui.dialog.EditNameDialog.EditNameDialogListener;
import com.owncloud.android.ui.dialog.SslValidatorDialog.OnSslValidatorListener;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewVideoActivity;

import eu.alefzero.webdav.WebdavClient;

/**
 * Displays, what files the user has available in his ownCloud.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */

public class FileDisplayActivity extends SherlockFragmentActivity implements
    OCFileListFragment.ContainerActivity, FileDetailFragment.ContainerActivity, OnNavigationListener, OnSslValidatorListener, OnRemoteOperationListener, EditNameDialogListener {
    
    private ArrayAdapter<String> mDirectories;
    private OCFile mCurrentDir = null;
    private OCFile mCurrentFile = null;

    private DataStorageManager mStorageManager;
    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private UploadFinishReceiver mUploadFinishReceiver;
    private DownloadFinishReceiver mDownloadFinishReceiver;
    private FileDownloaderBinder mDownloaderBinder = null;
    private FileUploaderBinder mUploaderBinder = null;
    private ServiceConnection mDownloadConnection = null, mUploadConnection = null;
    private RemoteOperationResult mLastSslUntrustedServerResult = null;
    
    private OCFileListFragment mFileList;
    
    private boolean mDualPane;
    private boolean mBackFromCreatingFirstAccount;
    
    private static final int DIALOG_SETUP_ACCOUNT = 0;
    private static final int DIALOG_CREATE_DIR = 1;
    public static final int DIALOG_SHORT_WAIT = 3;
    private static final int DIALOG_CHOOSE_UPLOAD_SOURCE = 4;
    private static final int DIALOG_SSL_VALIDATOR = 5;
    private static final int DIALOG_CERT_NOT_SAVED = 6;
    private static final String DIALOG_CHANGELOG_TAG = "DIALOG_CHANGELOG";

    
    private static final int ACTION_SELECT_CONTENT_FROM_APPS = 1;
    private static final int ACTION_SELECT_MULTIPLE_FILES = 2;
    
    private static final String TAG = "FileDisplayActivity";

    private OCFile mWaitingToPreview;
    private Handler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.d(getClass().toString(), "onCreate() start");
        super.onCreate(savedInstanceState);

        /// Load of parameters from received intent
        Account account = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_ACCOUNT);
        if (account != null && AccountUtils.setCurrentOwnCloudAccount(this, account.name)) {
            mCurrentDir = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE); 
        }
        
        /// Load of saved instance state: keep this always before initDataFromCurrentAccount()
        if(savedInstanceState != null) {
            // TODO - test if savedInstanceState should take precedence over file in the intent ALWAYS (now), NEVER. SOME TIMES
            mCurrentDir = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_FILE);
            mWaitingToPreview = (OCFile) savedInstanceState.getParcelable(FileDetailActivity.KEY_WAITING_TO_PREVIEW);

        } else {
            mWaitingToPreview = null;
        }
        
        if (!AccountUtils.accountsAreSetup(this)) {
            /// no account available: FORCE ACCOUNT CREATION
            mStorageManager = null;
            createFirstAccount();
            
        } else {    /// at least an account is available
            
            initDataFromCurrentAccount();   // it checks mCurrentDir and mCurrentFile with the current account
            
        }
        
        mUploadConnection = new ListServiceConnection(); 
        mDownloadConnection = new ListServiceConnection();
        bindService(new Intent(this, FileUploader.class), mUploadConnection, Context.BIND_AUTO_CREATE);
        bindService(new Intent(this, FileDownloader.class), mDownloadConnection, Context.BIND_AUTO_CREATE);

        // PIN CODE request ;  best location is to decide, let's try this first
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_MAIN) && savedInstanceState == null) {
            requestPinCode();
        }

        // file observer
        Intent observer_intent = new Intent(this, FileObserverService.class);
        observer_intent.putExtra(FileObserverService.KEY_FILE_CMD, FileObserverService.CMD_INIT_OBSERVED_LIST);
        startService(observer_intent);
        
            
        /// USER INTERFACE
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
            
        // Drop-down navigation 
        mDirectories = new CustomArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item);
        OCFile currFile = mCurrentDir;
        while(mStorageManager != null && currFile != null && currFile.getFileName() != OCFile.PATH_SEPARATOR) {
            mDirectories.add(currFile.getFileName());
            currFile = mStorageManager.getFileById(currFile.getParentId());
        }
        mDirectories.add(OCFile.PATH_SEPARATOR);

        // Inflate and set the layout view
        setContentView(R.layout.files);    
        mFileList = (OCFileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
        mDualPane = (findViewById(R.id.file_details_container) != null);
        if (mDualPane && savedInstanceState == null) {
            initFileDetailsInDualPane();
        }
            
        // Action bar setup
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);   // mandatory since Android ICS, according to the official documentation
        actionBar.setDisplayHomeAsUpEnabled(mCurrentDir != null && mCurrentDir.getParentId() != 0);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(mDirectories, this);
        setSupportProgressBarIndeterminateVisibility(false);        // always AFTER setContentView(...) ; to workaround bug in its implementation
        
        
        mBackFromCreatingFirstAccount = false;
        
        Log_OC.d(getClass().toString(), "onCreate() end");
    }

    
    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        FileFragment fragment = (FileFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
        if (fragment != null 
                && fragment.getFile() != null 
                && (newConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
                && newConfig.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            
            onFileClick(fragment.getFile(), true);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.remove((Fragment)fragment);
            transaction.commit();
        
        } else {
            finish();
            Intent intent = new Intent(this, FileDisplayActivity.class);
            intent .putExtra(FileDetailFragment.EXTRA_FILE, mCurrentDir);
            intent .putExtra(FileDetailFragment.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
            startActivity(intent);
        }
    }
    
    
    /**
     * Launches the account creation activity. To use when no ownCloud account is available
     */
    private void createFirstAccount() {
        Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
        intent.putExtra(android.provider.Settings.EXTRA_AUTHORITIES, new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
        startActivity(intent);  // the new activity won't be created until this.onStart() and this.onResume() are finished;
    }

    
    /**
     *  Load of state dependent of the existence of an ownCloud account
     */
    private void initDataFromCurrentAccount() {
        /// Storage manager initialization - access to local database
        mStorageManager = new FileDataStorageManager(
                AccountUtils.getCurrentOwnCloudAccount(this),
                getContentResolver());

        /// Check if mCurrentDir is a directory
        if(mCurrentDir != null && !mCurrentDir.isDirectory()) {
            mCurrentFile = mCurrentDir;
            mCurrentDir = mStorageManager.getFileById(mCurrentDir.getParentId());
        }
        
        /// Check if mCurrentDir and mCurrentFile are in the current account, and update them
        if (mCurrentDir != null) {
            mCurrentDir = mStorageManager.getFileByPath(mCurrentDir.getRemotePath());   // mCurrentDir == null if it is not in the current account
        }
        if (mCurrentFile != null) {
            if (mCurrentFile.fileExists()) {
                mCurrentFile = mStorageManager.getFileByPath(mCurrentFile.getRemotePath());   // mCurrentFile == null if it is not in the current account
            }   // else : keep mCurrentFile with the received value; this is currently the case of an upload in progress, when the user presses the status notification in a landscape tablet
        }
        
        /// Default to root if mCurrentDir was not found
        if (mCurrentDir == null) {
            mCurrentDir = mStorageManager.getFileByPath("/"); // will be NULL if the database was never synchronized
        }
    }
        
    
    private void initFileDetailsInDualPane() {
        if (mDualPane && getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG) == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (mCurrentFile != null) {
                if (PreviewMediaFragment.canBePreviewed(mCurrentFile)) {
                    if (mCurrentFile.isDown()) {
                        int startPlaybackPosition = getIntent().getIntExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0);
                        boolean autoplay = getIntent().getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, true);
                        transaction.replace(R.id.file_details_container, new PreviewMediaFragment(mCurrentFile, AccountUtils.getCurrentOwnCloudAccount(this), startPlaybackPosition, autoplay), FileDetailFragment.FTAG);
                    } else {
                        transaction.replace(R.id.file_details_container, new FileDetailFragment(mCurrentFile, AccountUtils.getCurrentOwnCloudAccount(this)), FileDetailFragment.FTAG);
                        mWaitingToPreview = mCurrentFile;
                    }
                } else {
                    transaction.replace(R.id.file_details_container, new FileDetailFragment(mCurrentFile, AccountUtils.getCurrentOwnCloudAccount(this)), FileDetailFragment.FTAG);
                }
                mCurrentFile = null;
                
            } else {
                transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null), FileDetailFragment.FTAG); // empty FileDetailFragment
            }
            transaction.commit();
        }
    }
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDownloadConnection != null)
            unbindService(mDownloadConnection);
        if (mUploadConnection != null)
            unbindService(mUploadConnection);
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
                EditNameDialog dialog = EditNameDialog.newInstance(getString(R.string.uploader_info_dirname), "", -1, -1, this);
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
            case android.R.id.home: {
                if(mCurrentDir != null && mCurrentDir.getParentId() != 0){
                    onBackPressed(); 
                }
                break;
            }
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    private void startSynchronization() {
        ContentResolver.cancelSync(null, AccountAuthenticator.AUTH_TOKEN_TYPE);   // cancel the current synchronizations of any ownCloud account
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(
                AccountUtils.getCurrentOwnCloudAccount(this),
                AccountAuthenticator.AUTH_TOKEN_TYPE, bundle);
    }


    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        int i = itemPosition;
        while (i-- != 0) {
            onBackPressed();
        }
        // the next operation triggers a new call to this method, but it's necessary to 
        // ensure that the name exposed in the action bar is the current directory when the 
        // user selected it in the navigation list
        if (itemPosition != 0)
            getSupportActionBar().setSelectedNavigationItem(0);
        return true;
    }

    /**
     * Called, when the user selected something for uploading
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        
        if (requestCode == ACTION_SELECT_CONTENT_FROM_APPS && (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)) {
            requestSimpleUpload(data, resultCode);
            
        } else if (requestCode == ACTION_SELECT_MULTIPLE_FILES && (resultCode == RESULT_OK || resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)) {
            requestMultipleUpload(data, resultCode);
            
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
            i.putExtra(FileUploader.KEY_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
            i.putExtra(FileUploader.KEY_LOCAL_FILE, filePaths);
            i.putExtra(FileUploader.KEY_REMOTE_FILE, remotePaths);
            i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_MULTIPLE_FILES);
            if (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)
                i.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_MOVE);
            startService(i);
            
        } else {
            Log_OC.d("FileDisplay", "User clicked on 'Update' with no selection");
            Toast t = Toast.makeText(this, getString(R.string.filedisplay_no_file_selected), Toast.LENGTH_LONG);
            t.show();
            return;
        }
    }


    private void requestSimpleUpload(Intent data, int resultCode) {
        String filepath = null;
        try {
            Uri selectedImageUri = data.getData();

            String filemanagerstring = selectedImageUri.getPath();
            String selectedImagePath = getPath(selectedImageUri);

            if (selectedImagePath != null)
                filepath = selectedImagePath;
            else
                filepath = filemanagerstring;
            
        } catch (Exception e) {
            Log_OC.e("FileDisplay", "Unexpected exception when trying to read the result of Intent.ACTION_GET_CONTENT", e);
            e.printStackTrace();
            
        } finally {
            if (filepath == null) {
                Log_OC.e("FileDisplay", "Couldnt resolve path to file");
                Toast t = Toast.makeText(this, getString(R.string.filedisplay_unexpected_bad_get_content), Toast.LENGTH_LONG);
                t.show();
                return;
            }
        }

        Intent i = new Intent(this, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT,
                AccountUtils.getCurrentOwnCloudAccount(this));
        String remotepath = new String();
        for (int j = mDirectories.getCount() - 2; j >= 0; --j) {
            remotepath += OCFile.PATH_SEPARATOR + mDirectories.getItem(j);
        }
        if (!remotepath.endsWith(OCFile.PATH_SEPARATOR))
            remotepath += OCFile.PATH_SEPARATOR;
        remotepath += new File(filepath).getName();

        i.putExtra(FileUploader.KEY_LOCAL_FILE, filepath);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, remotepath);
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        if (resultCode == UploadFilesActivity.RESULT_OK_AND_MOVE)
            i.putExtra(FileUploader.KEY_LOCAL_BEHAVIOUR, FileUploader.LOCAL_BEHAVIOUR_MOVE);
        startService(i);
    }


    @Override
    public void onBackPressed() {
        if (mDirectories.getCount() <= 1) {
            finish();
            return;
        }
        popDirname();
        mFileList.onNavigateUp();
        mCurrentDir = mFileList.getCurrentFile();
        
        if (mDualPane) {
            // Resets the FileDetailsFragment on Tablets so that it always displays
            Fragment fileFragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fileFragment != null && (fileFragment instanceof PreviewMediaFragment || !((FileDetailFragment) fileFragment).isEmpty())) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null), FileDetailFragment.FTAG); // empty FileDetailFragment                
                transaction.commit();
            }
        }
        
        if(mCurrentDir.getParentId() == 0){
            ActionBar actionBar = getSupportActionBar(); 
            actionBar.setDisplayHomeAsUpEnabled(false);
        } 
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // responsibility of restore is preferred in onCreate() before than in onRestoreInstanceState when there are Fragments involved
        Log_OC.d(getClass().toString(), "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDetailFragment.EXTRA_FILE, mCurrentDir);
        if (mDualPane) {
            FileFragment fragment = (FileFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fragment != null) {
                OCFile file = fragment.getFile();
                if (file != null) {
                    outState.putParcelable(FileDetailFragment.EXTRA_FILE, file);
                }
            }
        }
        outState.putParcelable(FileDetailActivity.KEY_WAITING_TO_PREVIEW, mWaitingToPreview);
        Log_OC.d(getClass().toString(), "onSaveInstanceState() end");
    }

    
    @Override
    protected void onResume() {
        Log_OC.d(getClass().toString(), "onResume() start");
        super.onResume();

        if (AccountUtils.accountsAreSetup(this)) {
            
            if (mStorageManager == null) {
                // this is necessary for handling the come back to FileDisplayActivity when the first ownCloud account is created 
                initDataFromCurrentAccount();
                if (mDualPane) {
                    initFileDetailsInDualPane();
                }
                mBackFromCreatingFirstAccount = true;
            }
            
            // Listen for sync messages
            IntentFilter syncIntentFilter = new IntentFilter(FileSyncService.SYNC_MESSAGE);
            mSyncBroadcastReceiver = new SyncBroadcastReceiver();
            registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);
            
            // Listen for upload messages
            IntentFilter uploadIntentFilter = new IntentFilter(FileUploader.UPLOAD_FINISH_MESSAGE);
            mUploadFinishReceiver = new UploadFinishReceiver();
            registerReceiver(mUploadFinishReceiver, uploadIntentFilter);
            
            // Listen for download messages
            IntentFilter downloadIntentFilter = new IntentFilter(FileDownloader.DOWNLOAD_ADDED_MESSAGE);
            downloadIntentFilter.addAction(FileDownloader.DOWNLOAD_FINISH_MESSAGE);
            mDownloadFinishReceiver = new DownloadFinishReceiver();
            registerReceiver(mDownloadFinishReceiver, downloadIntentFilter);
        
            // List current directory
            mFileList.listDirectory(mCurrentDir);   // TODO we should find the way to avoid the need of this (maybe it's not necessary yet; to check)
            
        } else {
            
            mStorageManager = null;     // an invalid object will be there if all the ownCloud accounts are removed
            showDialog(DIALOG_SETUP_ACCOUNT);
            
        }
        Log_OC.d(getClass().toString(), "onResume() end");
    }

    
    @Override
    protected void onPause() {
        Log_OC.d(getClass().toString(), "onPause() start");
        super.onPause();
        if (mSyncBroadcastReceiver != null) {
            unregisterReceiver(mSyncBroadcastReceiver);
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
        if (!AccountUtils.accountsAreSetup(this)) {
            dismissDialog(DIALOG_SETUP_ACCOUNT);
        }
        
        Log_OC.d(getClass().toString(), "onPause() end");
    }

    
    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        if (id == DIALOG_SSL_VALIDATOR && mLastSslUntrustedServerResult != null) {
            ((SslValidatorDialog)dialog).updateResult(mLastSslUntrustedServerResult);
        }
    }

    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder;
        switch (id) {
        case DIALOG_SETUP_ACCOUNT: {
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.main_tit_accsetup);
            builder.setMessage(R.string.main_wrn_accsetup);
            builder.setCancelable(false);
            builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    createFirstAccount();
                    dialog.dismiss();
                }
            });
            String message = String.format(getString(R.string.common_exit), getString(R.string.app_name));
            builder.setNegativeButton(message, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            //builder.setNegativeButton(android.R.string.cancel, this);
            dialog = builder.create();
            break;
        }
        case DIALOG_CREATE_DIR: {
            builder = new Builder(this);
            final EditText dirNameInput = new EditText(getBaseContext());
            builder.setView(dirNameInput);
            builder.setTitle(R.string.uploader_info_dirname);
            int typed_color = getResources().getColor(R.color.setup_text_typed);
            dirNameInput.setTextColor(typed_color);
            builder.setPositiveButton(android.R.string.ok,
                    new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String directoryName = dirNameInput.getText().toString();
                            if (directoryName.trim().length() == 0) {
                                dialog.cancel();
                                return;
                            }
    
                            // Figure out the path where the dir needs to be created
                            String path;
                            if (mCurrentDir == null) {
                                // this is just a patch; we should ensure that mCurrentDir never is null
                                if (!mStorageManager.fileExists(OCFile.PATH_SEPARATOR)) {
                                    OCFile file = new OCFile(OCFile.PATH_SEPARATOR);
                                    mStorageManager.saveFile(file);
                                }
                                mCurrentDir = mStorageManager.getFileByPath(OCFile.PATH_SEPARATOR);
                            }
                            path = FileDisplayActivity.this.mCurrentDir.getRemotePath();
                            
                            // Create directory
                            path += directoryName + OCFile.PATH_SEPARATOR;
                            Thread thread = new Thread(new DirectoryCreator(path,  AccountUtils.getCurrentOwnCloudAccount(FileDisplayActivity.this), new Handler()));
                            thread.start();
                            
                            dialog.dismiss();
                            
                            showDialog(DIALOG_SHORT_WAIT);
                        }
                    });
            builder.setNegativeButton(R.string.common_cancel,
                    new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            dialog = builder.create();
            break;
        }
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
            
            String[] items = null;
            
            String[] allTheItems = { getString(R.string.actionbar_upload_files),
                                     getString(R.string.actionbar_upload_from_apps),
                                     getString(R.string.actionbar_failed_instant_upload) };
            
            String[] commonItems = { getString(R.string.actionbar_upload_files),
                                     getString(R.string.actionbar_upload_from_apps) };
            
            if (InstantUploadActivity.IS_ENABLED)
                items = allTheItems;
            else 
                items = commonItems;
            
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionbar_upload);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0) {
                        // if (!mDualPane) {
                        Intent action = new Intent(FileDisplayActivity.this, UploadFilesActivity.class);
                        action.putExtra(UploadFilesActivity.EXTRA_ACCOUNT,
                                AccountUtils.getCurrentOwnCloudAccount(FileDisplayActivity.this));
                        startActivityForResult(action, ACTION_SELECT_MULTIPLE_FILES);
                        // } else {
                        // TODO create and handle new fragment
                        // LocalFileListFragment
                        // }
                    } else if (item == 1) {
                        Intent action = new Intent(Intent.ACTION_GET_CONTENT);
                        action = action.setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                                ACTION_SELECT_CONTENT_FROM_APPS);
                    } else if (item == 2 && InstantUploadActivity.IS_ENABLED) {
                        Account account = AccountUtils.getCurrentOwnCloudAccount(FileDisplayActivity.this);
                        Intent action = new Intent(FileDisplayActivity.this, InstantUploadActivity.class);
                        action.putExtra(FileUploader.KEY_ACCOUNT, account);
                        startActivity(action);
                    }
                }
            });
            dialog = builder.create();
            break;
        }
        case DIALOG_SSL_VALIDATOR: {
            dialog = SslValidatorDialog.newInstance(this, mLastSslUntrustedServerResult, this);
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
     * Translates a content URI of an image to a physical path
     * on the disk
     * @param uri The URI to resolve
     * @return The path to the image or null if it could not be found
     */
    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } 
        return null;
    }
    
    /**
     * Pushes a directory to the drop down list
     * @param directory to push
     * @throws IllegalArgumentException If the {@link OCFile#isDirectory()} returns false.
     */
    public void pushDirname(OCFile directory) {
        if(!directory.isDirectory()){
            throw new IllegalArgumentException("Only directories may be pushed!");
        }
        mDirectories.insert(directory.getFileName(), 0);
        mCurrentDir = directory;
    }

    /**
     * Pops a directory name from the drop down list
     * @return True, unless the stack is empty
     */
    public boolean popDirname() {
        mDirectories.remove(mDirectories.getItem(0));
        return !mDirectories.isEmpty();
    }

    private class DirectoryCreator implements Runnable {
        private String mTargetPath;
        private Account mAccount;
        private Handler mHandler; 
    
        public DirectoryCreator(String targetPath, Account account, Handler handler) {
            mTargetPath = targetPath;
            mAccount = account;
            mHandler = handler;
        }
    
        @Override
        public void run() {
            WebdavClient wdc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getApplicationContext());
            boolean created = wdc.createDirectory(mTargetPath);
            if (created) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() { 
                        dismissDialog(DIALOG_SHORT_WAIT);
                        
                        // Save new directory in local database
                        OCFile newDir = new OCFile(mTargetPath);
                        newDir.setMimetype("DIR");
                        newDir.setParentId(mCurrentDir.getFileId());
                        mStorageManager.saveFile(newDir);
    
                        // Display the new folder right away
                        mFileList.listDirectory();
                    }
                });
                
            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        dismissDialog(DIALOG_SHORT_WAIT);
                        try {
                            Toast msg = Toast.makeText(FileDisplayActivity.this, R.string.create_dir_fail_msg, Toast.LENGTH_LONG); 
                            msg.show();
                        
                        } catch (NotFoundException e) {
                            Log_OC.e(TAG, "Error while trying to show fail message ", e);
                        }
                    }
                });
            }
        }
    
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
            return v;
        }
    
        public View getDropDownView(int position, View convertView,
                ViewGroup parent) {
            View v = super.getDropDownView(position, convertView, parent);
    
            ((TextView) v).setTextColor(getResources().getColorStateList(
                    android.R.color.white));
    
            return v;
        }
    
    }

    private class SyncBroadcastReceiver extends BroadcastReceiver {

        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean inProgress = intent.getBooleanExtra(FileSyncService.IN_PROGRESS, false);
            String accountName = intent.getStringExtra(FileSyncService.ACCOUNT_NAME);

            Log_OC.d("FileDisplay", "sync of account " + accountName + " is in_progress: " + inProgress);

            if (accountName.equals(AccountUtils.getCurrentOwnCloudAccount(context).name)) {  
            
                String synchFolderRemotePath = intent.getStringExtra(FileSyncService.SYNC_FOLDER_REMOTE_PATH); 
                 
                boolean fillBlankRoot = false;
                if (mCurrentDir == null) {
                    mCurrentDir = mStorageManager.getFileByPath("/");
                    fillBlankRoot = (mCurrentDir != null);
                }

                if ((synchFolderRemotePath != null && mCurrentDir != null && (mCurrentDir.getRemotePath().equals(synchFolderRemotePath)))
                        || fillBlankRoot ) {
                    if (!fillBlankRoot) 
                        mCurrentDir = getStorageManager().getFileByPath(synchFolderRemotePath);
                    OCFileListFragment fileListFragment = (OCFileListFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.fileList);
                    if (fileListFragment != null) {
                        fileListFragment.listDirectory(mCurrentDir);
                    }
                }
                
                setSupportProgressBarIndeterminateVisibility(inProgress);
                if (mBackFromCreatingFirstAccount) {
                    // awful patch to fix problem with visibility of progress circle with the first refresh of the first account
                    // TODO - kill this Activity when the first account has to be created instead of stack the account creation on it
                    getSupportActionBar().hide();
                    getSupportActionBar().show();
                    mBackFromCreatingFirstAccount = false;
                }
                removeStickyBroadcast(intent);
                
            }
            
            RemoteOperationResult synchResult = (RemoteOperationResult)intent.getSerializableExtra(FileSyncService.SYNC_RESULT);
            if (synchResult != null) {
                if (synchResult.getCode().equals(RemoteOperationResult.ResultCode.SSL_RECOVERABLE_PEER_UNVERIFIED)) {
                    mLastSslUntrustedServerResult = synchResult;
                    showDialog(DIALOG_SSL_VALIDATOR); 
                }
            }
        }
    }
    

    private class UploadFinishReceiver extends BroadcastReceiver {
        /**
         * Once the file upload has finished -> update view
         *  @author David A. Velasco
         * {@link BroadcastReceiver} to enable upload feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            String uploadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
            String accountName = intent.getStringExtra(FileUploader.ACCOUNT_NAME);
            boolean sameAccount = accountName.equals(AccountUtils.getCurrentOwnCloudAccount(context).name);
            boolean isDescendant = (mCurrentDir != null) && (uploadedRemotePath != null) && (uploadedRemotePath.startsWith(mCurrentDir.getRemotePath()));
            if (sameAccount && isDescendant) {
                OCFileListFragment fileListFragment = (OCFileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
                if (fileListFragment != null) { 
                    fileListFragment.listDirectory();
                }
            }
        }
        
    }
    
    
    /**
     * Class waiting for broadcast events from the {@link FielDownloader} service.
     * 
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * current folder.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean sameAccount = isSameAccount(context, intent);
            String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
            boolean isDescendant = isDescendant(downloadedRemotePath);
            
            if (sameAccount && isDescendant) {
                updateLeftPanel();
                if (mDualPane) {
                    updateRightPanel(intent.getAction(), downloadedRemotePath, intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false));
                }
            }
            
            removeStickyBroadcast(intent);
        }

        private boolean isDescendant(String downloadedRemotePath) {
            return (mCurrentDir != null && downloadedRemotePath != null && downloadedRemotePath.startsWith(mCurrentDir.getRemotePath()));
        }

        private boolean isSameAccount(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);
            return (accountName != null && accountName.equals(AccountUtils.getCurrentOwnCloudAccount(context).name));
        }
    }
    
    
    protected void updateLeftPanel() {
        OCFileListFragment fileListFragment = (OCFileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
        if (fileListFragment != null) { 
            fileListFragment.listDirectory();
        }
    }

    protected void updateRightPanel(String downloadEvent, String downloadedRemotePath, boolean success) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
        boolean waitedPreview = (mWaitingToPreview != null && mWaitingToPreview.getRemotePath().equals(downloadedRemotePath));
        if (fragment != null && fragment instanceof FileDetailFragment) {
            FileDetailFragment detailsFragment = (FileDetailFragment) fragment;
            OCFile fileInFragment = detailsFragment.getFile();
            if (fileInFragment != null && !downloadedRemotePath.equals(fileInFragment.getRemotePath())) {
                // the user browsed to other file ; forget the automatic preview 
                mWaitingToPreview = null;
                
            } else if (downloadEvent.equals(FileDownloader.DOWNLOAD_ADDED_MESSAGE)) {
                // grant that the right panel updates the progress bar
                detailsFragment.listenForTransferProgress();
                detailsFragment.updateFileDetails(true, false);
                
            } else if (downloadEvent.equals(FileDownloader.DOWNLOAD_FINISH_MESSAGE)) {
                //  update the right panel 
                if (success && waitedPreview) {
                    mWaitingToPreview = mStorageManager.getFileById(mWaitingToPreview.getFileId());   // update the file from database, for the local storage path
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.file_details_container, new PreviewMediaFragment(mWaitingToPreview, AccountUtils.getCurrentOwnCloudAccount(this), 0, true), FileDetailFragment.FTAG);
                    transaction.commit();
                    mWaitingToPreview = null;
                    
                } else {
                    detailsFragment.updateFileDetails(false, (success));
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public DataStorageManager getStorageManager() {
        return mStorageManager;
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDirectoryClick(OCFile directory) {
        pushDirname(directory);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        
        if (mDualPane) {
            // Resets the FileDetailsFragment on Tablets so that it always displays
            Fragment fileFragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fileFragment != null && (fileFragment instanceof PreviewMediaFragment || !((FileDetailFragment) fileFragment).isEmpty())) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null), FileDetailFragment.FTAG); // empty FileDetailFragment                
                transaction.commit();
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileClick(OCFile file, boolean onOrientationChange) {
        if (file != null && PreviewImageFragment.canBePreviewed(file)) {
            // preview image - it handles the download, if needed
            startPreviewImage(file);
            
        } else if (file != null && PreviewMediaFragment.canBePreviewed(file)) {
            if (file.isDown()) {
                // general preview
                if (!onOrientationChange) {
                    startMediaPreview(file, 0, true, onOrientationChange);
                } else {
                    int startPlaybackPosition = 0;
                    boolean autoplay = true;
                    Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
                    if (fragment != null && file.isVideo()) {
                        PreviewMediaFragment videoFragment = (PreviewMediaFragment)fragment;
                        startPlaybackPosition = videoFragment.getPosition();
                        autoplay = videoFragment.isPlaying();
                    }
                    startMediaPreview(file, startPlaybackPosition, autoplay, onOrientationChange);
                }
                
            } else {
                // automatic download, preview on finish
                startDownloadForPreview(file, onOrientationChange);
                
            }
        } else {
            // details view
            startDetails(file, onOrientationChange);
        }
    }

    private void startPreviewImage(OCFile file) {
        Intent showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, file);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
        startActivity(showDetailsIntent);
    }
    
    private void startMediaPreview(OCFile file, int startPlaybackPosition, boolean autoplay, boolean onOrientationChange) {
        if (mDualPane && !onOrientationChange) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.file_details_container, new PreviewMediaFragment(file, AccountUtils.getCurrentOwnCloudAccount(this), startPlaybackPosition, autoplay), FileDetailFragment.FTAG);
            transaction.commit();
            
        } else {
            Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, file);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
            showDetailsIntent.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, startPlaybackPosition);
            showDetailsIntent.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, autoplay);
            startActivity(showDetailsIntent);
        }
    }
    
    private void startDownloadForPreview(OCFile file, boolean onOrientationChange) {
        if (mDualPane && !onOrientationChange) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.file_details_container, new FileDetailFragment(file, AccountUtils.getCurrentOwnCloudAccount(this)), FileDetailFragment.FTAG);
            transaction.commit();
            mWaitingToPreview = file;
            requestForDownload();
            
        } else {
            Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, file);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
            startActivity(showDetailsIntent);
        }
    }

    
    private void startDetails(OCFile file, boolean onOrientationChange) {
        if (mDualPane && !file.isImage() && !onOrientationChange) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.file_details_container, new FileDetailFragment(file, AccountUtils.getCurrentOwnCloudAccount(this)), FileDetailFragment.FTAG);
            transaction.commit();
        } else {
            Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, file);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
            startActivity(showDetailsIntent);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public OCFile getInitialDirectory() {
        return mCurrentDir;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileStateChanged() {
        OCFileListFragment fileListFragment = (OCFileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
        if (fileListFragment != null) { 
            fileListFragment.listDirectory();
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public FileDownloaderBinder getFileDownloaderBinder() {
        return mDownloaderBinder;
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public FileUploaderBinder getFileUploaderBinder() {
        return mUploaderBinder;
    }
    
    
    /** Defines callbacks for service binding, passed to bindService() */
    private class ListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(new ComponentName(FileDisplayActivity.this, FileDownloader.class))) {
                Log_OC.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mWaitingToPreview != null) {
                    requestForDownload();
                }
                
            } else if (component.equals(new ComponentName(FileDisplayActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            // a new chance to get the mDownloadBinder through getFileDownloadBinder() - THIS IS A MESS
            if (mFileList != null)
                mFileList.listDirectory();
            if (mDualPane) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
                if (fragment != null && fragment instanceof FileDetailFragment) {
                    FileDetailFragment detailFragment = (FileDetailFragment)fragment;
                    detailFragment.listenForTransferProgress();
                    detailFragment.updateFileDetails(false, false);
                }
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
        startSynchronization();                
    }


    @Override
    public void onFailedSavingCertificate() {
        showDialog(DIALOG_CERT_NOT_SAVED);
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
        if (operation instanceof RemoveFileOperation) {
            onRemoveFileOperationFinish((RemoveFileOperation)operation, result);
                
        } else if (operation instanceof RenameFileOperation) {
            onRenameFileOperationFinish((RenameFileOperation)operation, result);
            
        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation)operation, result);
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
        dismissDialog(DIALOG_SHORT_WAIT);
        if (result.isSuccess()) {
            Toast msg = Toast.makeText(this, R.string.remove_success_msg, Toast.LENGTH_LONG);
            msg.show();
            OCFile removedFile = operation.getFile();
            if (mDualPane) {
                FileFragment details = (FileFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
                if (details != null && removedFile.equals(details.getFile())) {
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null)); // empty FileDetailFragment
                    transaction.commit();
                }
            }
            if (mStorageManager.getFileById(removedFile.getParentId()).equals(mCurrentDir)) {
                mFileList.listDirectory();
            }
                
        } else {
            Toast msg = Toast.makeText(this, R.string.remove_fail_msg, Toast.LENGTH_LONG); 
            msg.show();
            if (result.isSslRecoverableException()) {
                mLastSslUntrustedServerResult = result;
                showDialog(DIALOG_SSL_VALIDATOR); 
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
        dismissDialog(DIALOG_SHORT_WAIT);
        OCFile renamedFile = operation.getFile();
        if (result.isSuccess()) {
            if (mDualPane) {
                FileFragment details = (FileFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
                if (details != null && details instanceof FileDetailFragment && renamedFile.equals(details.getFile()) ) {
                    ((FileDetailFragment) details).updateFileDetails(renamedFile, AccountUtils.getCurrentOwnCloudAccount(this));
                }
            }
            if (mStorageManager.getFileById(renamedFile.getParentId()).equals(mCurrentDir)) {
                mFileList.listDirectory();
            }
            
        } else {
            if (result.getCode().equals(ResultCode.INVALID_LOCAL_FILE_NAME)) {
                Toast msg = Toast.makeText(this, R.string.rename_local_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
                // TODO throw again the new rename dialog
            } else {
                Toast msg = Toast.makeText(this, R.string.rename_server_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
                if (result.isSslRecoverableException()) {
                    mLastSslUntrustedServerResult = result;
                    showDialog(DIALOG_SSL_VALIDATOR); 
                }
            }
        }
    }


    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation, RemoteOperationResult result) {
        dismissDialog(DIALOG_SHORT_WAIT);
        OCFile syncedFile = operation.getLocalFile();
        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                Intent i = new Intent(this, ConflictsResolveActivity.class);
                i.putExtra(ConflictsResolveActivity.EXTRA_FILE, syncedFile);
                i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
                startActivity(i);
                
            } else {
                Toast msg = Toast.makeText(this, R.string.sync_file_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
            }
            
        } else {
            if (operation.transferWasRequested()) {
                mFileList.listDirectory();
                onTransferStateChanged(syncedFile, true, true);
                
            } else {
                Toast msg = Toast.makeText(this, R.string.sync_file_nothing_to_do_msg, Toast.LENGTH_LONG); 
                msg.show();
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading) {
        /*OCFileListFragment fileListFragment = (OCFileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
        if (fileListFragment != null) { 
            fileListFragment.listDirectory();
        }*/
        if (mDualPane) {
            FileFragment details = (FileFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (details != null && details instanceof FileDetailFragment && file.equals(details.getFile()) ) {
                if (downloading || uploading) {
                    ((FileDetailFragment)details).updateFileDetails(file, AccountUtils.getCurrentOwnCloudAccount(this));
                } else {
                    ((FileDetailFragment)details).updateFileDetails(false, true);
                }
            }
        }
    }


    @Override
    public void showFragmentWithDetails(OCFile file) {
        if (mDualPane) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.file_details_container, new FileDetailFragment(file, AccountUtils.getCurrentOwnCloudAccount(this)), FileDetailFragment.FTAG); 
            transaction.commit();
            
        } else {
            Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, file);
            showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
            showDetailsIntent.putExtra(FileDetailActivity.EXTRA_MODE, FileDetailActivity.MODE_DETAILS);
            startActivity(showDetailsIntent);
        }
    }

    public void onDismiss(EditNameDialog dialog) {
        //dialog.dismiss();
        if (dialog.getResult()) {
            String newDirectoryName = dialog.getNewFilename().trim();
            Log.d(TAG, "'create directory' dialog dismissed with new name " + newDirectoryName);
            if (newDirectoryName.length() > 0) {
                String path;
                if (mCurrentDir == null) {
                    // this is just a patch; we should ensure that mCurrentDir never is null
                    if (!mStorageManager.fileExists(OCFile.PATH_SEPARATOR)) {
                        OCFile file = new OCFile(OCFile.PATH_SEPARATOR);
                        mStorageManager.saveFile(file);
                    }
                    mCurrentDir = mStorageManager.getFileByPath(OCFile.PATH_SEPARATOR);
                }
                path = FileDisplayActivity.this.mCurrentDir.getRemotePath();
                
                // Create directory
                path += newDirectoryName + OCFile.PATH_SEPARATOR;
                Thread thread = new Thread(new DirectoryCreator(path,  AccountUtils.getCurrentOwnCloudAccount(FileDisplayActivity.this), new Handler()));
                thread.start();
                
                showDialog(DIALOG_SHORT_WAIT);
            }
        }
    }

    private void requestForDownload() {
        Account account = AccountUtils.getCurrentOwnCloudAccount(this);
        if (!mDownloaderBinder.isDownloading(account, mWaitingToPreview)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, account);
            i.putExtra(FileDownloader.EXTRA_FILE, mWaitingToPreview);
            startService(i);
        }
    }

    
}
