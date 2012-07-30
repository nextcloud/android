/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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

package eu.alefzero.owncloud.ui.activity;

import java.io.File;
import java.util.ArrayList;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.CrashHandler;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.datamodel.DataStorageManager;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.services.FileDownloader;
import eu.alefzero.owncloud.files.services.FileUploader;
import eu.alefzero.owncloud.syncadapter.FileSyncService;
import eu.alefzero.owncloud.ui.fragment.FileDetailFragment;
import eu.alefzero.owncloud.ui.fragment.FileListFragment;
import eu.alefzero.webdav.WebdavClient;

/**
 * Displays, what files the user has available in his ownCloud.
 * 
 * @author Bartek Przybylski
 * 
 */

public class FileDisplayActivity extends SherlockFragmentActivity implements
    FileListFragment.ContainerActivity, FileDetailFragment.ContainerActivity, OnNavigationListener, OnClickListener, android.view.View.OnClickListener  {
    
    private ArrayAdapter<String> mDirectories;
    private OCFile mCurrentDir;
    private String[] mDirs = null;

    private DataStorageManager mStorageManager;
    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private UploadFinishReceiver mUploadFinishReceiver;
    private DownloadFinishReceiver mDownloadFinishReceiver;
    
    private View mLayoutView = null;
    private FileListFragment mFileList;
    
    private boolean mDualPane;
    
    private boolean mForcedLoginToCreateFirstAccount = false;
    
    private static final String KEY_DIR_ARRAY = "DIR_ARRAY";
    private static final String KEY_CURRENT_DIR = "DIR";
    
    private static final int DIALOG_SETUP_ACCOUNT = 0;
    private static final int DIALOG_CREATE_DIR = 1;
    private static final int DIALOG_ABOUT_APP = 2;
    public static final int DIALOG_SHORT_WAIT = 3;
    
    private static final int ACTION_SELECT_FILE = 1;
    
    private static final String TAG = "FileDisplayActivity";
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(getClass().toString(), "onCreate() start");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));

        if(savedInstanceState != null) {
            mDirs = savedInstanceState.getStringArray(KEY_DIR_ARRAY);
            mDirectories = new CustomArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item);
            mDirectories.add(OCFile.PATH_SEPARATOR);
            if (mDirs != null)
                for (String s : mDirs)
                    mDirectories.insert(s, 0);
            mCurrentDir = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_FILE);
        }
        
        mLayoutView = getLayoutInflater().inflate(R.layout.files, null);  // always inflate this at onCreate() ; just once!
        
        if (AccountUtils.accountsAreSetup(this)) {
            
            initDelayedTilAccountAvailabe();
            
            // PIN CODE request ;  best location is to decide, let's try this first
            //if (savedInstanceState == null) {
            if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_MAIN) && savedInstanceState == null) {
                requestPinCode();
            }
            
            
        } else {
            
            setContentView(R.layout.no_account_available);
            getSupportActionBar().setNavigationMode(ActionBar.DISPLAY_SHOW_TITLE);
            findViewById(R.id.setup_account).setOnClickListener(this);

            setSupportProgressBarIndeterminateVisibility(false);

            Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(android.provider.Settings.EXTRA_AUTHORITIES, new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
            startActivity(intent);  // although the code is here, the activity won't be created until this.onStart() and this.onResume() are finished;
            mForcedLoginToCreateFirstAccount = true;
        }
        
        Log.i(getClass().toString(), "onCreate() end");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
            inflater.inflate(R.menu.menu, menu);
            return true;
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
                ContentResolver.cancelSync(null, "org.owncloud");   // cancel the current synchronizations of any ownCloud account
                Bundle bundle = new Bundle();
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                ContentResolver.requestSync(
                        AccountUtils.getCurrentOwnCloudAccount(this),
                        "org.owncloud", bundle);
                break;
            }
            case R.id.action_upload: {
                Intent action = new Intent(Intent.ACTION_GET_CONTENT);
                action = action.setType("*/*")
                        .addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(
                        Intent.createChooser(action, getString(R.string.upload_chooser_title)),
                        ACTION_SELECT_FILE);
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
                retval = false;
        }
        return retval;
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        int i = itemPosition;
        while (i-- != 0) {
            onBackPressed();
        }
        return true;
    }

    /**
     * Called, when the user selected something for uploading
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ACTION_SELECT_FILE) {
            if (resultCode == RESULT_OK) {
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
                startService(i);
            }
            
        }/* dvelasco: WIP - not working as expected ... yet :)
             else if (requestCode == ACTION_CREATE_FIRST_ACCOUNT) {
            if (resultCode != RESULT_OK) {
                finish();   // the user cancelled the AuthenticatorActivity
            }
        }*/
    }

    @Override
    public void onBackPressed() {
        if (mDirectories == null || mDirectories.getCount() <= 1) {
            finish();
            return;
        }
        popDirname();
        mFileList.onNavigateUp();
        mCurrentDir = mFileList.getCurrentFile();
        
        if(mCurrentDir.getParentId() == 0){
            ActionBar actionBar = getSupportActionBar(); 
            actionBar.setDisplayHomeAsUpEnabled(false);
        } 
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // responsability of restore is prefered in onCreate() before than in onRestoreInstanceState when there are Fragments involved
        Log.i(getClass().toString(), "onSaveInstanceState() start");
        super.onSaveInstanceState(outState);
        if(mDirectories != null && mDirectories.getCount() != 0){
            mDirs = new String[mDirectories.getCount()-1];
            for (int j = mDirectories.getCount() - 2, i = 0; j >= 0; --j, ++i) {
                mDirs[i] = mDirectories.getItem(j);
            }
        }
        outState.putStringArray(KEY_DIR_ARRAY, mDirs);
        outState.putParcelable(FileDetailFragment.EXTRA_FILE, mCurrentDir);
        Log.i(getClass().toString(), "onSaveInstanceState() end");
    }

    @Override
    protected void onResume() {
        Log.i(getClass().toString(), "onResume() start");
        super.onResume();

        if (AccountUtils.accountsAreSetup(this)) {
         // at least an account exist: normal operation
            
            // set the layout only if it couldn't be set in onCreate
            if (mForcedLoginToCreateFirstAccount) {
                initDelayedTilAccountAvailabe();
                mForcedLoginToCreateFirstAccount = false;
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
        
            // Storage manager initialization
            mStorageManager = new FileDataStorageManager(
                    AccountUtils.getCurrentOwnCloudAccount(this),
                    getContentResolver());
        
            // File list fragments   
            mFileList = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
            
            
            // Figure out what directory to list. 
            // Priority: Intent (here), savedInstanceState (onCreate), root dir (dir is null)
            if(getIntent().hasExtra(FileDetailFragment.EXTRA_FILE)){
                mCurrentDir = (OCFile) getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE);
                if(mCurrentDir != null && !mCurrentDir.isDirectory()){
                    mCurrentDir = mStorageManager.getFileById(mCurrentDir.getParentId());
                }
            
                // Clear intent extra, so rotating the screen will not return us to this directory
                getIntent().removeExtra(FileDetailFragment.EXTRA_FILE);
            }
            
            if (mCurrentDir == null)
                mCurrentDir = mStorageManager.getFileByPath("/");
                
            // Drop-Down navigation and file list restore
            mDirectories = new CustomArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item);
        
        
            // Given the case we have a file to display:
            if(mCurrentDir != null){
                ArrayList<OCFile> files = new ArrayList<OCFile>();
                OCFile currFile = mCurrentDir;
                while(currFile != null){
                    files.add(currFile);
                    currFile = mStorageManager.getFileById(currFile.getParentId());
                }
            
                // Insert in mDirs
                mDirs = new String[files.size()];
                for(int i = files.size() - 1; i >= 0; i--){
                    mDirs[i] = files.get(i).getFileName();
                }
            }
        
            if (mDirs != null) {
                for (String s : mDirs)
                    mDirectories.add(s);
            } else {
                mDirectories.add(OCFile.PATH_SEPARATOR);
            }
               
            // Actionbar setup
            ActionBar action_bar = getSupportActionBar();
            action_bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            action_bar.setDisplayShowTitleEnabled(false);
            action_bar.setListNavigationCallbacks(mDirectories, this);
            if(mCurrentDir != null && mCurrentDir.getParentId() != 0){
                action_bar.setDisplayHomeAsUpEnabled(true);
            } else {
                action_bar.setDisplayHomeAsUpEnabled(false);
            }
        
            // List dir here
            mFileList.listDirectory(mCurrentDir);
        }
        Log.i(getClass().toString(), "onResume() end");
    }

    @Override
    protected void onPause() {
        Log.i(getClass().toString(), "onPause() start");
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
        
        getIntent().putExtra(FileDetailFragment.EXTRA_FILE, mCurrentDir);
        Log.i(getClass().toString(), "onPause() end");
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
        AlertDialog.Builder builder;
        switch (id) {
        case DIALOG_SETUP_ACCOUNT:
            builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.main_tit_accsetup);
            builder.setMessage(R.string.main_wrn_accsetup);
            builder.setCancelable(false);
            builder.setPositiveButton(android.R.string.ok, this);
            builder.setNegativeButton(android.R.string.cancel, this);
            dialog = builder.create();
            break;
        case DIALOG_ABOUT_APP: {
            builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.about_title));
            PackageInfo pkg;
            try {
                pkg = getPackageManager().getPackageInfo(getPackageName(), 0);
                builder.setMessage("ownCloud android client\n\nversion: " + pkg.versionName );
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
            final Account a = AccountUtils.getCurrentOwnCloudAccount(this);
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
                            Thread thread = new Thread(new DirectoryCreator(path, a, new Handler()));
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
        default:
            dialog = null;
        }
    
        return dialog;
    }

    
    /**
     * Responds to the "There are no ownCloud Accounts setup" dialog
     * TODO: Dialog is 100% useless -> Remove
     */
    @Override
    public void onClick(DialogInterface dialog, int which) {
        // In any case - we won't need it anymore
        dialog.dismiss();
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
            intent.putExtra("authorities",
                    new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
            startActivity(intent);
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            finish();
        }
    
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
        private AccountManager mAm;
        private Handler mHandler; 
    
        public DirectoryCreator(String targetPath, Account account, Handler handler) {
            mTargetPath = targetPath;
            mAccount = account;
            mAm = (AccountManager) getSystemService(ACCOUNT_SERVICE);
            mHandler = handler;
        }
    
        @Override
        public void run() {
            WebdavClient wdc = new WebdavClient(mAccount, getApplicationContext());
            
            String username = mAccount.name.substring(0,
                    mAccount.name.lastIndexOf('@'));
            String password = mAm.getPassword(mAccount);
    
            wdc.setCredentials(username, password);
            wdc.allowSelfsignedCertificates();
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
                        mFileList.listDirectory(mCurrentDir);
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
                    FileListFragment fileListFragment = (FileListFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.fileList);
                    if (fileListFragment != null) {
                        fileListFragment.listDirectory(mCurrentDir);  
                    }
                }
                
                setSupportProgressBarIndeterminateVisibility(inProgress);
                
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
            long parentDirId = intent.getLongExtra(FileUploader.EXTRA_PARENT_DIR_ID, -1);
            OCFile parentDir = mStorageManager.getFileById(parentDirId);
            String accountName = intent.getStringExtra(FileUploader.ACCOUNT_NAME);

            if (accountName.equals(AccountUtils.getCurrentOwnCloudAccount(context).name) &&
                    parentDir != null && 
                    (   (mCurrentDir == null && parentDir.getFileName().equals("/")) ||
                            parentDir.equals(mCurrentDir)
                    )
                ) {
                FileListFragment fileListFragment = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
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

            if (accountName.equals(AccountUtils.getCurrentOwnCloudAccount(context).name) &&
                     mCurrentDir != null && mCurrentDir.getFileId() == mStorageManager.getFileByPath(downloadedRemotePath).getParentId()) {
                FileListFragment fileListFragment = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
                if (fileListFragment != null) { 
                    fileListFragment.listDirectory();
                }
            }
        }
    }

    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.setup_account) {
            Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(android.provider.Settings.EXTRA_AUTHORITIES, new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
            startActivity(intent); 
            mForcedLoginToCreateFirstAccount = true;
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
            if (fileDetails != null) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.remove(fileDetails);
                transaction.add(R.id.file_details_container, new FileDetailFragment(null, null));
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
            showDetailsIntent.putExtra(FileDownloader.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
            startActivity(showDetailsIntent);
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileStateChanged() {
        FileListFragment fileListFragment = (FileListFragment) getSupportFragmentManager().findFragmentById(R.id.fileList);
        if (fileListFragment != null) { 
            fileListFragment.listDirectory();
        }
    }
    
    
    /**
     *  Operations in this method should be preferably performed in onCreate to have a lighter onResume method. 
     * 
     *  But we need to delay them to onResume for the first start of the application, when no account exists and the login activity must be shown; and 
     *  put instead the ugly view that shows the 'Setup' button to restart the login activity.   
     *  
     *  In other way, if the users cancels or presses BACK in the login page that first time (users can be cruel sometimes) would show a blank view (the 
     *  FragmentList view empty).
     *  
     *  This is temporal, until we found out how to get a result in this activity after launching the ADD_ACCOUNT Intent with startActivityForResult (not trivial)
     */
    private void initDelayedTilAccountAvailabe() {
        setContentView(mLayoutView);    
        mDualPane = (findViewById(R.id.file_details_container) != null);
        if (mDualPane && getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG) == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null)); // empty FileDetailFragment
            transaction.commit();
        }
        setSupportProgressBarIndeterminateVisibility(false);
    }
    

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

    
}
