/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
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
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
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
import com.owncloud.android.ui.dialog.ChangelogDialog;
import com.owncloud.android.ui.dialog.SslValidatorDialog;
import com.owncloud.android.ui.dialog.SslValidatorDialog.OnSslValidatorListener;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;

import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavClient;

/**
 * Displays, what files the user has available in his ownCloud.
 * 
 * @author Bartek Przybylski
 * 
 */

public class FileDisplayActivity extends SherlockFragmentActivity implements
    OCFileListFragment.ContainerActivity, FileDetailFragment.ContainerActivity, OnNavigationListener, OnSslValidatorListener, OnRemoteOperationListener {
    
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
    
    private static final int DIALOG_SETUP_ACCOUNT = 0;
    private static final int DIALOG_CREATE_DIR = 1;
    private static final int DIALOG_ABOUT_APP = 2;
    public static final int DIALOG_SHORT_WAIT = 3;
    private static final int DIALOG_CHOOSE_UPLOAD_SOURCE = 4;
    private static final int DIALOG_SSL_VALIDATOR = 5;
    private static final int DIALOG_CERT_NOT_SAVED = 6;
    private static final String DIALOG_CHANGELOG_TAG = "DIALOG_CHANGELOG";

    
    private static final int ACTION_SELECT_CONTENT_FROM_APPS = 1;
    private static final int ACTION_SELECT_MULTIPLE_FILES = 2;
    
    private static final String TAG = "FileDisplayActivity";

    private static int[] mMenuIdentifiersToPatch = {R.id.about_app};
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(getClass().toString(), "onCreate() start");
        super.onCreate(savedInstanceState);

        /// Load of parameters from received intent
        mCurrentDir = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE); // no check necessary, mCurrenDir == null if the parameter is not in the intent
        Account account = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_ACCOUNT);
        if (account != null)
            AccountUtils.setCurrentOwnCloudAccount(this, account.name);
        
        /// Load of saved instance state: keep this always before initDataFromCurrentAccount()
        if(savedInstanceState != null) {
            // TODO - test if savedInstanceState should take precedence over file in the intent ALWAYS (now), NEVER, or SOME TIMES
            mCurrentDir = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_FILE);
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
        while(currFile != null && currFile.getFileName() != OCFile.PATH_SEPARATOR) {
            mDirectories.add(currFile.getFileName());
            currFile = mStorageManager.getFileById(currFile.getParentId());
        }
        mDirectories.add(OCFile.PATH_SEPARATOR);

        // Inflate and set the layout view
        setContentView(R.layout.files);    
        mFileList = (OCFileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
        mDualPane = (findViewById(R.id.file_details_container) != null);
        if (mDualPane) {
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
        
        
        // show changelog, if needed
        showChangeLog();
        
        Log.d(getClass().toString(), "onCreate() end");
    }

    
    /**
     * Shows a dialog with the change log of the current version after each app update
     * 
     *  TODO make it permanent; by now, only to advice the workaround app for 4.1.x
     */
    private void showChangeLog() {
        if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.JELLY_BEAN) {
            final String KEY_VERSION = "version";
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            int currentVersionNumber = 0;
            int savedVersionNumber = sharedPref.getInt(KEY_VERSION, 0);
            try {
                PackageInfo pi          = getPackageManager().getPackageInfo(getPackageName(), 0);
                currentVersionNumber    = pi.versionCode;
            } catch (Exception e) {}
     
            if (currentVersionNumber > savedVersionNumber) {
                ChangelogDialog.newInstance(true).show(getSupportFragmentManager(), DIALOG_CHANGELOG_TAG);
                Editor editor   = sharedPref.edit();
                editor.putInt(KEY_VERSION, currentVersionNumber);
                editor.commit();
            }
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
                transaction.replace(R.id.file_details_container, new FileDetailFragment(mCurrentFile, AccountUtils.getCurrentOwnCloudAccount(this)), FileDetailFragment.FTAG); // empty FileDetailFragment
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
            inflater.inflate(R.menu.menu, menu);
            
            patchHiddenAccents(menu);
            
            return true;
    }

    /**
     * Workaround for this: <a href="http://code.google.com/p/android/issues/detail?id=3974">http://code.google.com/p/android/issues/detail?id=3974</a> 
     * 
     * @param menu      Menu to patch
     */
    private void patchHiddenAccents(Menu menu) {
        for (int i = 0; i < mMenuIdentifiersToPatch.length ; i++) {
            MenuItem aboutItem = menu.findItem(mMenuIdentifiersToPatch[i]);
            if (aboutItem != null && aboutItem.getIcon() instanceof BitmapDrawable) {
                // Clip off the bottom three (density independent) pixels of transparent padding
                Bitmap original = ((BitmapDrawable) aboutItem.getIcon()).getBitmap();
                float scale = getResources().getDisplayMetrics().density;
                int clippedHeight = (int) (original.getHeight() - (3 * scale));
                Bitmap scaled = Bitmap.createBitmap(original, 0, 0, original.getWidth(), clippedHeight);
                aboutItem.setIcon(new BitmapDrawable(getResources(), scaled));
            }
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case R.id.createDirectoryItem: {
                showDialog(DIALOG_CREATE_DIR);
                break;
            }
            case R.id.startSync: {
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
            case R.id.about_app : {
                showDialog(DIALOG_ABOUT_APP);
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
            Log.d("FileDisplay", "User clicked on 'Update' with no selection");
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
            Log.e("FileDisplay", "Unexpected exception when trying to read the result of Intent.ACTION_GET_CONTENT", e);
            e.printStackTrace();
            
        } finally {
            if (filepath == null) {
                Log.e("FileDisplay", "Couldnt resolve path to file");
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
            FileDetailFragment fileDetails = (FileDetailFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fileDetails != null && !fileDetails.isEmpty()) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(fileDetails);
                transaction.add(R.id.file_details_container, new FileDetailFragment(null, null), FileDetailFragment.FTAG);
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
        Log.d(getClass().toString(), "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileDetailFragment.EXTRA_FILE, mCurrentDir);
        if (mDualPane) {
            FileDetailFragment fragment = (FileDetailFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fragment != null) {
                OCFile file = fragment.getDisplayedFile();
                if (file != null) {
                    outState.putParcelable(FileDetailFragment.EXTRA_FILE, file);
                }
            }
        }
        Log.d(getClass().toString(), "onSaveInstanceState() end");
    }

    @Override
    protected void onResume() {
        Log.d(getClass().toString(), "onResume() start");
        super.onResume();

        if (AccountUtils.accountsAreSetup(this)) {
            
            if (mStorageManager == null) {
                // this is necessary for handling the come back to FileDisplayActivity when the first ownCloud account is created 
                initDataFromCurrentAccount();
                if (mDualPane) {
                    initFileDetailsInDualPane();
                }
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
            IntentFilter downloadIntentFilter = new IntentFilter(FileDownloader.DOWNLOAD_FINISH_MESSAGE);
            mDownloadFinishReceiver = new DownloadFinishReceiver();
            registerReceiver(mDownloadFinishReceiver, downloadIntentFilter);
        
            // List current directory
            mFileList.listDirectory(mCurrentDir);   // TODO we should find the way to avoid the need of this (maybe it's not necessary yet; to check)
            
        } else {
            
            mStorageManager = null;     // an invalid object will be there if all the ownCloud accounts are removed
            showDialog(DIALOG_SETUP_ACCOUNT);
            
        }
        Log.d(getClass().toString(), "onResume() end");
    }

    
    @Override
    protected void onPause() {
        Log.d(getClass().toString(), "onPause() start");
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
        
        Log.d(getClass().toString(), "onPause() end");
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
        case DIALOG_ABOUT_APP: {
            builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.about_title));
            PackageInfo pkg;
            try {
                pkg = getPackageManager().getPackageInfo(getPackageName(), 0);
                builder.setMessage(String.format(getString(R.string.about_message), getString(R.string.app_name), pkg.versionName));
                builder.setIcon(android.R.drawable.ic_menu_info_details);
                dialog = builder.create();
            } catch (NameNotFoundException e) {
                builder = null;
                dialog = null;
                Log.e(TAG, "Error while showing about dialog", e);
            }
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
            final String [] items = {   getString(R.string.actionbar_upload_files), 
                                        getString(R.string.actionbar_upload_from_apps) }; 
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.actionbar_upload);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    if (item == 0) {
                        //if (!mDualPane) { 
                            Intent action = new Intent(FileDisplayActivity.this, UploadFilesActivity.class);
                            action.putExtra(UploadFilesActivity.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(FileDisplayActivity.this));
                            startActivityForResult(action, ACTION_SELECT_MULTIPLE_FILES);
                        //} else {
                            // TODO create and handle new fragment LocalFileListFragment
                        //}
                    } else if (item == 1) {
                        Intent action = new Intent(Intent.ACTION_GET_CONTENT);
                        action = action.setType("*/*")
                                .addCategory(Intent.CATEGORY_OPENABLE);
                        startActivityForResult(
                                Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                                ACTION_SELECT_CONTENT_FROM_APPS);
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
                            Log.e(TAG, "Error while trying to show fail message " , e);
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
            boolean inProgress = intent.getBooleanExtra(
                    FileSyncService.IN_PROGRESS, false);
            String accountName = intent
                    .getStringExtra(FileSyncService.ACCOUNT_NAME);

            Log.d("FileDisplay", "sync of account " + accountName
                    + " is in_progress: " + inProgress);

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
     * Once the file download has finished -> update view
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);
            boolean sameAccount = accountName.equals(AccountUtils.getCurrentOwnCloudAccount(context).name);
            boolean isDescendant = (mCurrentDir != null) && (downloadedRemotePath != null) && (downloadedRemotePath.startsWith(mCurrentDir.getRemotePath()));
            if (sameAccount && isDescendant) {
                OCFileListFragment fileListFragment = (OCFileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
                if (fileListFragment != null) { 
                    fileListFragment.listDirectory();
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
            FileDetailFragment fileDetails = (FileDetailFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fileDetails != null && !fileDetails.isEmpty()) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(fileDetails);
                transaction.add(R.id.file_details_container, new FileDetailFragment(null, null), FileDetailFragment.FTAG);
                transaction.commit();
            }
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileClick(OCFile file) {
        
        // If we are on a large device -> update fragment
        if (mDualPane) {
            // buttons in the details view are problematic when trying to reuse an existing fragment; create always a new one solves some of them, BUT no all; downloads are 'dangerous'
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.file_details_container, new FileDetailFragment(file, AccountUtils.getCurrentOwnCloudAccount(this)), FileDetailFragment.FTAG);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commit();
            
        } else {    // small or medium screen device -> new Activity
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
                Log.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
            } else if (component.equals(new ComponentName(FileDisplayActivity.this, FileUploader.class))) {
                Log.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            // a new chance to get the mDownloadBinder through getFileDownloadBinder() - THIS IS A MESS
            if (mFileList != null)
                mFileList.listDirectory();
            if (mDualPane) {
                FileDetailFragment fragment = (FileDetailFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
                if (fragment != null)
                    fragment.updateFileDetails(false);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(FileDisplayActivity.this, FileDownloader.class))) {
                Log.d(TAG, "Download service disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(FileDisplayActivity.this, FileUploader.class))) {
                Log.d(TAG, "Upload service disconnected");
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
                FileDetailFragment details = (FileDetailFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
                if (details != null && removedFile.equals(details.getDisplayedFile()) ) {
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
                FileDetailFragment details = (FileDetailFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
                if (details != null && renamedFile.equals(details.getDisplayedFile()) ) {
                    details.updateFileDetails(renamedFile, AccountUtils.getCurrentOwnCloudAccount(this));
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
            FileDetailFragment details = (FileDetailFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (details != null && file.equals(details.getDisplayedFile()) ) {
                if (downloading || uploading) {
                    details.updateFileDetails(file, AccountUtils.getCurrentOwnCloudAccount(this));
                } else {
                    details.updateFileDetails(downloading || uploading);
                }
            }
        }
    }


    


}
