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
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
        OnNavigationListener, OnClickListener, android.view.View.OnClickListener  {
    
    private ArrayAdapter<String> mDirectories;
    private OCFile mCurrentDir;
    private String[] mDirs = null;

    private DataStorageManager mStorageManager;
    private SyncBroadcastReceiver mSyncBroadcastReceiver;
    private UploadFinishReceiver mUploadFinishReceiver;
    
    private View mLayoutView = null;
    private FileListFragment mFileList;
    
    private static final String KEY_DIR_ARRAY = "DIR_ARRAY";
    private static final String KEY_CURRENT_DIR = "DIR";
    
    private static final int DIALOG_SETUP_ACCOUNT = 0;
    private static final int DIALOG_CREATE_DIR = 1;
    private static final int DIALOG_ABOUT_APP = 2;
    
    private static final int ACTION_SELECT_FILE = 1;
    //private static final int ACTION_CREATE_FIRST_ACCOUNT = 2; dvelasco: WIP

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(getClass().toString(), "onCreate() start");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setSupportProgressBarIndeterminateVisibility(false);

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(getApplicationContext()));

        if(savedInstanceState != null){
            mCurrentDir = (OCFile) savedInstanceState.getParcelable(KEY_CURRENT_DIR);   // this is never saved with this key :S
        }
        
        mLayoutView = getLayoutInflater().inflate(R.layout.files, null);  // always inflate this at onCreate() ; just once!
        
        if (AccountUtils.accountsAreSetup(this)) {
            setContentView(mLayoutView);    
            
        } else  {
            setContentView(R.layout.no_account_available);
            setProgressBarIndeterminateVisibility(false);
            getSupportActionBar().setNavigationMode(ActionBar.DISPLAY_SHOW_TITLE);
            findViewById(R.id.setup_account).setOnClickListener(this);
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
                        Intent.createChooser(action, "Upload file from..."),
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
                Uri selectedImageUri = data.getData();
    
                String filemanagerstring = selectedImageUri.getPath();
                String selectedImagePath = getPath(selectedImageUri);
                String filepath;
    
                if (selectedImagePath != null)
                    filepath = selectedImagePath;
                else
                    filepath = filemanagerstring;
    
                if (filepath == null) {
                    Log.e("FileDisplay", "Couldnt resolve path to file");
                    return;
                }
    
                Intent i = new Intent(this, FileUploader.class);
                i.putExtra(FileUploader.KEY_ACCOUNT,
                        AccountUtils.getCurrentOwnCloudAccount(this));
                String remotepath = new String();
                for (int j = mDirectories.getCount() - 2; j >= 0; --j) {
                    remotepath += "/" + mDirectories.getItem(j);
                }
                if (!remotepath.endsWith("/"))
                    remotepath += "/";
                remotepath += new File(filepath).getName();
                remotepath = Uri.encode(remotepath, "/");
    
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
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.i(getClass().toString(), "onRestoreInstanceState() start");
        super.onRestoreInstanceState(savedInstanceState);
        mDirs = savedInstanceState.getStringArray(KEY_DIR_ARRAY);
        mDirectories = new CustomArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item);
        mDirectories.add("/");
        if (mDirs != null)
            for (String s : mDirs)
                mDirectories.insert(s, 0);
        mCurrentDir = savedInstanceState.getParcelable(FileDetailFragment.EXTRA_FILE);
        Log.i(getClass().toString(), "onRestoreInstanceState() end");
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
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

        if (!AccountUtils.accountsAreSetup(this)) {
            /*Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra(android.provider.Settings.EXTRA_AUTHORITIES, new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
            //startActivity(intent);
            startActivityForResult(intent, ACTION_CREATE_FIRST_ACCOUNT);*/
            
        } else {    // at least an account exist: normal operation

            // set the layout only if it couldn't be set in onCreate
            if (findViewById(R.id.file_list_view) == null)
                setContentView(mLayoutView);

            // Listen for sync messages
            IntentFilter syncIntentFilter = new IntentFilter(FileSyncService.SYNC_MESSAGE);
            mSyncBroadcastReceiver = new SyncBroadcastReceiver();
            registerReceiver(mSyncBroadcastReceiver, syncIntentFilter);
            
            // Listen for upload messages
            IntentFilter uploadIntentFilter = new IntentFilter(FileUploader.UPLOAD_FINISH_MESSAGE);
            mUploadFinishReceiver = new UploadFinishReceiver();
            registerReceiver(mUploadFinishReceiver, uploadIntentFilter);
        
            // Storage manager initialization
            mStorageManager = new FileDataStorageManager(
                    AccountUtils.getCurrentOwnCloudAccount(this),
                    getContentResolver());
        
            // File list
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
                mDirectories.add("/");
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
            builder.setTitle("About");
            PackageInfo pkg;
            try {
                pkg = getPackageManager().getPackageInfo(getPackageName(), 0);
                builder.setMessage("ownCloud android client\n\nversion: " + pkg.versionName );
                builder.setIcon(android.R.drawable.ic_menu_info_details);
                dialog = builder.create();
            } catch (NameNotFoundException e) {
                builder = null;
                dialog = null;
                e.printStackTrace();
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
                                if (!mStorageManager.fileExists("/")) {
                                    OCFile file = new OCFile("/");
                                    mStorageManager.saveFile(file);
                                }
                                mCurrentDir = mStorageManager.getFileByPath("/");
                            }
                            path = FileDisplayActivity.this.mCurrentDir.getRemotePath();
                            
                            // Create directory
                            path += Uri.encode(directoryName) + "/";
                            Thread thread = new Thread(new DirectoryCreator(path, a));
                            thread.start();
    
                            // Save new directory in local database
                            OCFile newDir = new OCFile(path);
                            newDir.setMimetype("DIR");
                            newDir.setParentId(mCurrentDir.getFileId());
                            mStorageManager.saveFile(newDir);
    
                            // Display the new folder right away
                            dialog.dismiss();
                            mFileList.listDirectory(mCurrentDir);
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
    
        public DirectoryCreator(String targetPath, Account account) {
            mTargetPath = targetPath;
            mAccount = account;
            mAm = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        }
    
        @Override
        public void run() {
            WebdavClient wdc = new WebdavClient(Uri.parse(mAm.getUserData(
                    mAccount, AccountAuthenticator.KEY_OC_URL)));
    
            String username = mAccount.name.substring(0,
                    mAccount.name.lastIndexOf('@'));
            String password = mAm.getPassword(mAccount);
    
            wdc.setCredentials(username, password);
            wdc.allowSelfsignedCertificates();
            wdc.createDirectory(mTargetPath);
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
            String account_name = intent
                    .getStringExtra(FileSyncService.ACCOUNT_NAME);
            Log.d("FileDisplay", "sync of account " + account_name
                    + " is in_progress: " + inProgress);
            setSupportProgressBarIndeterminateVisibility(inProgress);
            if (!inProgress) {
                FileListFragment fileListFragment = (FileListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fileList);
                if (fileListFragment != null)
                    fileListFragment.listDirectory();
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
            
            if (parentDir != null && (
                    (mCurrentDir == null && parentDir.getFileName().equals("/")) ||
                     parentDir.equals(mCurrentDir))
                ) {
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
            Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
            intent.putExtra("authorities", new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
            startActivity(intent);
        }
    }

    public DataStorageManager getStorageManager() {
        return mStorageManager;
    }    
}
