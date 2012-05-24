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
import java.net.URLDecoder;
import java.net.URLEncoder;

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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.datamodel.DataStorageManager;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.files.services.FileUploader;
import eu.alefzero.owncloud.syncadapter.FileSyncService;
import eu.alefzero.owncloud.ui.fragment.FileListFragment;
import eu.alefzero.webdav.WebdavClient;

/**
 * Displays, what files the user has available in his ownCloud.
 * 
 * @author Bartek Przybylski
 * 
 */

public class FileDisplayActivity extends SherlockFragmentActivity implements
        OnNavigationListener, OnClickListener {
    private ArrayAdapter<String> mDirectories;
    private DataStorageManager mStorageManager;
    private String[] mDirs = null;

    private SyncBroadcastReceiver syncBroadcastRevceiver;

    private static final String KEY_DIR = "DIR";
    
    private static final int DIALOG_SETUP_ACCOUNT = 0;
    private static final int DIALOG_CREATE_DIR = 1;
    private static final int ACTION_SELECT_FILE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!accountsAreSetup()) {
            showDialog(DIALOG_SETUP_ACCOUNT);
            return;
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(false);
        
        setContentView(R.layout.files);

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
    
            case android.R.id.home: {
                Intent i = new Intent(this, AccountSelectActivity.class);
                startActivity(i);
                finish();
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == ACTION_SELECT_FILE) {
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
                    remotepath += "/" + URLEncoder.encode(mDirectories.getItem(j));
                }
                if (!remotepath.endsWith("/"))
                    remotepath += "/";
                remotepath += URLEncoder.encode(new File(filepath).getName());
                Log.e("ASD", remotepath + "");
    
                i.putExtra(FileUploader.KEY_LOCAL_FILE, filepath);
                i.putExtra(FileUploader.KEY_REMOTE_FILE, remotepath);
                i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
                startService(i);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mDirectories.getCount() == 1) {
            finish();
            return;
        }
        popPath();
        ((FileListFragment) getSupportFragmentManager().findFragmentById(
                R.id.fileList)).onNavigateUp();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Check, if there are ownCloud accounts
        if (!accountsAreSetup()) {
            showDialog(DIALOG_SETUP_ACCOUNT);
        }
        mDirs = savedInstanceState.getStringArray(KEY_DIR);
        mDirectories = new CustomArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item);
        mDirectories.add("/");
        if (mDirs != null)
            for (String s : mDirs)
                mDirectories.insert(s, 0);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mDirectories != null){
            mDirs = new String[mDirectories.getCount()-1];
            for (int j = mDirectories.getCount() - 2, i = 0; j >= 0; --j, ++i) {
                mDirs[i] = mDirectories.getItem(j);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!accountsAreSetup()) {
            showDialog(DIALOG_SETUP_ACCOUNT);
            return;
        }

        IntentFilter f = new IntentFilter(FileSyncService.SYNC_MESSAGE);
        syncBroadcastRevceiver = new SyncBroadcastReceiver();
        registerReceiver(syncBroadcastRevceiver, f);

        mDirectories = new CustomArrayAdapter<String>(this, R.layout.sherlock_spinner_dropdown_item);
        mDirectories.add("/");
        if (mDirs != null) {
            for (String s : mDirs)
                mDirectories.insert(s, 0);
            FileListFragment fileListFramgent = (FileListFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.fileList);
            if (fileListFramgent != null) fileListFramgent.listDirectory();
        }

        mStorageManager = new FileDataStorageManager(
                AccountUtils.getCurrentOwnCloudAccount(this),
                getContentResolver());
        ActionBar action_bar = getSupportActionBar();
        action_bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        action_bar.setDisplayShowTitleEnabled(false);
        action_bar.setListNavigationCallbacks(mDirectories, this);
        action_bar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (syncBroadcastRevceiver != null) {
            unregisterReceiver(syncBroadcastRevceiver);
            syncBroadcastRevceiver = null;
        }
    
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
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
        case DIALOG_CREATE_DIR: {
            builder = new Builder(this);
            final EditText dirName = new EditText(getBaseContext());
            final Account a = AccountUtils.getCurrentOwnCloudAccount(this);
            builder.setView(dirName);
            builder.setTitle(R.string.uploader_info_dirname);
            int typed_color = getResources().getColor(R.color.setup_text_typed);
            dirName.setTextColor(typed_color);
    
            builder.setPositiveButton(android.R.string.ok,
                    new OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String s = dirName.getText().toString();
                            if (s.trim().length() == 0) {
                                dialog.cancel();
                                return;
                            }
    
                            String path = "";
                            for (int i = mDirectories.getCount() - 2; i >= 0; --i) {
                                path += "/" + mDirectories.getItem(i);
                            }
                            OCFile parent = mStorageManager.getFileByPath(path
                                    + "/");
                            path += s + "/";
                            Thread thread = new Thread(new DirectoryCreator(
                                    path, a));
                            thread.start();
    
                            OCFile new_file = new OCFile(path);
                            new_file.setMimetype("DIR");
                            new_file.setParentId(parent.getParentId());
                            mStorageManager.saveFile(new_file);
    
                            dialog.dismiss();
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

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    public void pushPath(String path) {
        mDirectories.insert(path, 0);
    }

    public boolean popPath() {
        mDirectories.remove(mDirectories.getItem(0));
        return !mDirectories.isEmpty();
    }

    /**
     * Checks, whether or not there are any ownCloud accounts setup.
     * 
     * @return true, if there is at least one account.
     */
    private boolean accountsAreSetup() {
        AccountManager accMan = AccountManager.get(this);
        Account[] accounts = accMan
                .getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
        return accounts.length > 0;
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
            wdc.allowUnsignedCertificates();
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
            setProgressBarIndeterminateVisibility(inProgress);
            if (!inProgress) {
                FileListFragment fileListFramgent = (FileListFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.fileList);
                if (fileListFramgent != null)
                    fileListFramgent.listDirectory();
            }
        }

    }

}
