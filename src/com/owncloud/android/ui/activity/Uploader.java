/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountAuthenticator;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Audio;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.utils.DisplayUtils;

/**
 * This can be used to upload things to an ownCloud instance.
 * 
 * @author Bartek Przybylski
 * 
 */
public class Uploader extends SherlockListActivity implements OnItemClickListener, android.view.View.OnClickListener {
    private static final String TAG = "ownCloudUploader";

    private Account mAccount;
    private AccountManager mAccountManager;
    private Stack<String> mParents;
    private ArrayList<Parcelable> mStreamsToUpload;
    private boolean mCreateDir;
    private String mUploadPath;
    private FileDataStorageManager mStorageManager;
    private OCFile mFile;

    private final static int DIALOG_NO_ACCOUNT = 0;
    private final static int DIALOG_WAITING = 1;
    private final static int DIALOG_NO_STREAM = 2;
    private final static int DIALOG_MULTIPLE_ACCOUNT = 3;

    private final static int REQUEST_CODE_SETUP_ACCOUNT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mParents = new Stack<String>();

        ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(DisplayUtils.getSeasonalIconId());

        if (prepareStreamsToUpload()) {
            mAccountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
            Account[] accounts = mAccountManager.getAccountsByType(MainApp.getAccountType());
            if (accounts.length == 0) {
                Log_OC.i(TAG, "No ownCloud account is available");
                showDialog(DIALOG_NO_ACCOUNT);
            } else if (accounts.length > 1) {
                Log_OC.i(TAG, "More then one ownCloud is available");
                showDialog(DIALOG_MULTIPLE_ACCOUNT);
            } else {
                mAccount = accounts[0];
                mStorageManager = new FileDataStorageManager(mAccount, getContentResolver());
                initTargetFolder();
                populateDirectoryList();
                
            }
            
        } else {
            showDialog(DIALOG_NO_STREAM);
        }
    }
    
    @Override
    protected Dialog onCreateDialog(final int id) {
        final AlertDialog.Builder builder = new Builder(this);
        switch (id) {
        case DIALOG_WAITING:
            ProgressDialog pDialog = new ProgressDialog(this);
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.setMessage(getResources().getString(R.string.uploader_info_uploading));
            return pDialog;
        case DIALOG_NO_ACCOUNT:
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(R.string.uploader_wrn_no_account_title);
            builder.setMessage(String.format(getString(R.string.uploader_wrn_no_account_text), getString(R.string.app_name)));
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.uploader_wrn_no_account_setup_btn_text, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.ECLAIR_MR1) {
                        // using string value since in API7 this
                        // constatn is not defined
                        // in API7 < this constatant is defined in
                        // Settings.ADD_ACCOUNT_SETTINGS
                        // and Settings.EXTRA_AUTHORITIES
                        Intent intent = new Intent(android.provider.Settings.ACTION_ADD_ACCOUNT);
                        intent.putExtra("authorities", new String[] { MainApp.getAuthTokenType() });
                        startActivityForResult(intent, REQUEST_CODE_SETUP_ACCOUNT);
                    } else {
                        // since in API7 there is no direct call for
                        // account setup, so we need to
                        // show our own AccountSetupAcricity, get
                        // desired results and setup
                        // everything for ourself
                        Intent intent = new Intent(getBaseContext(), AccountAuthenticator.class);
                        startActivityForResult(intent, REQUEST_CODE_SETUP_ACCOUNT);
                    }
                }
            });
            builder.setNegativeButton(R.string.uploader_wrn_no_account_quit_btn_text, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            return builder.create();
        case DIALOG_MULTIPLE_ACCOUNT:
            CharSequence ac[] = new CharSequence[mAccountManager.getAccountsByType(MainApp.getAccountType()).length];
            for (int i = 0; i < ac.length; ++i) {
                ac[i] = DisplayUtils.convertIdn(mAccountManager.getAccountsByType(MainApp.getAccountType())[i].name, false);
            }
            builder.setTitle(R.string.common_choose_account);
            builder.setItems(ac, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAccount = mAccountManager.getAccountsByType(MainApp.getAccountType())[which];
                    mStorageManager = new FileDataStorageManager(mAccount, getContentResolver());
                    initTargetFolder();
                    populateDirectoryList();
                }
            });
            builder.setCancelable(true);
            builder.setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    dialog.cancel();
                    finish();
                }
            });
            return builder.create();
        case DIALOG_NO_STREAM:
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setTitle(R.string.uploader_wrn_no_content_title);
            builder.setMessage(R.string.uploader_wrn_no_content_text);
            builder.setCancelable(false);
            builder.setNegativeButton(R.string.common_cancel, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            return builder.create();
        default:
            throw new IllegalArgumentException("Unknown dialog id: " + id);
        }
    }

    class a implements OnClickListener {
        String mPath;
        EditText mDirname;

        public a(String path, EditText dirname) {
            mPath = path; 
            mDirname = dirname;
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            Uploader.this.mUploadPath = mPath + mDirname.getText().toString();
            Uploader.this.mCreateDir = true;
            uploadFiles();
        }
    }

    @Override
    public void onBackPressed() {

        if (mParents.size() <= 1) {
            super.onBackPressed();
            return;
        } else {
            mParents.pop();
            populateDirectoryList();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // click on folder in the list
        Log_OC.d(TAG, "on item click");
        Vector<OCFile> tmpfiles = mStorageManager.getFolderContent(mFile);
        if (tmpfiles.size() <= 0) return;
        // filter on dirtype
        Vector<OCFile> files = new Vector<OCFile>();
        for (OCFile f : tmpfiles)
            if (f.isFolder())
                files.add(f);
        if (files.size() < position) {
            throw new IndexOutOfBoundsException("Incorrect item selected");
        }
        mParents.push(files.get(position).getFileName());
        populateDirectoryList();
    }

    @Override
    public void onClick(View v) {
        // click on button
        switch (v.getId()) {
        case R.id.uploader_choose_folder:
            mUploadPath = "";   // first element in mParents is root dir, represented by ""; init mUploadPath with "/" results in a "//" prefix
            for (String p : mParents)
                mUploadPath += p + OCFile.PATH_SEPARATOR;
            Log_OC.d(TAG, "Uploading file to dir " + mUploadPath);

            uploadFiles();

            break;
        default:
            throw new IllegalArgumentException("Wrong element clicked");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log_OC.i(TAG, "result received. req: " + requestCode + " res: " + resultCode);
        if (requestCode == REQUEST_CODE_SETUP_ACCOUNT) {
            dismissDialog(DIALOG_NO_ACCOUNT);
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            Account[] accounts = mAccountManager.getAccountsByType(MainApp.getAuthTokenType());
            if (accounts.length == 0) {
                showDialog(DIALOG_NO_ACCOUNT);
            } else {
                // there is no need for checking for is there more then one
                // account at this point
                // since account setup can set only one account at time
                mAccount = accounts[0];
                populateDirectoryList();
            }
        }
    }

    private void populateDirectoryList() {
        setContentView(R.layout.uploader_layout);

        String current_dir = mParents.peek();
        if(current_dir.equals("")){
            getSupportActionBar().setTitle(getString(R.string.default_display_name_for_root_folder));
        }
        else{
            getSupportActionBar().setTitle(current_dir);
        }
        boolean notRoot = (mParents.size() > 1);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(notRoot);
        actionBar.setHomeButtonEnabled(notRoot);

        String full_path = generatePath(mParents);
        
        Log_OC.d(TAG, "Populating view with content of : " + full_path);

        mFile = mStorageManager.getFileByPath(full_path);
        if (mFile != null) {
            Vector<OCFile> files = mStorageManager.getFolderContent(mFile);
            List<HashMap<String, Object>> data = new LinkedList<HashMap<String,Object>>();
            for (OCFile f : files) {
                HashMap<String, Object> h = new HashMap<String, Object>();
                if (f.isFolder()) {
                    h.put("dirname", f.getFileName());
                    data.add(h);
                }
            }
            SimpleAdapter sa = new SimpleAdapter(this,
                                                data,
                                                R.layout.uploader_list_item_layout,
                                                new String[] {"dirname"},
                                                new int[] {R.id.textView1});
            setListAdapter(sa);
            Button btn = (Button) findViewById(R.id.uploader_choose_folder);
            btn.setOnClickListener(this);
            getListView().setOnItemClickListener(this);
        }
    }

    private String generatePath(Stack<String> dirs) {
        String full_path = "";

        for (String a : dirs)
            full_path += a + "/";
        return full_path;
    }

    private boolean prepareStreamsToUpload() {
        if (getIntent().getAction().equals(Intent.ACTION_SEND)) {
            mStreamsToUpload = new ArrayList<Parcelable>();
            mStreamsToUpload.add(getIntent().getParcelableExtra(Intent.EXTRA_STREAM));
        } else if (getIntent().getAction().equals(Intent.ACTION_SEND_MULTIPLE)) {
            mStreamsToUpload = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }
        return (mStreamsToUpload != null && mStreamsToUpload.get(0) != null);
    }

    public void uploadFiles() {
        try {

            ArrayList<String> local = new ArrayList<String>();
            ArrayList<String> remote = new ArrayList<String>();
            
            // this checks the mimeType 
            for (Parcelable mStream : mStreamsToUpload) {
                
                Uri uri = (Uri) mStream;
                if (uri !=null) {
                    if (uri.getScheme().equals("content")) {
                        
                       String mimeType = getContentResolver().getType(uri);
                       
                       if (mimeType.contains("image")) {
                           String[] CONTENT_PROJECTION = { Images.Media.DATA, Images.Media.DISPLAY_NAME, Images.Media.MIME_TYPE, Images.Media.SIZE};
                           Cursor c = getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null);
                           c.moveToFirst();
                           int index = c.getColumnIndex(Images.Media.DATA);
                           String data = c.getString(index);
                           local.add(data);
                           remote.add(mUploadPath + c.getString(c.getColumnIndex(Images.Media.DISPLAY_NAME)));
                       
                       }
                       else if (mimeType.contains("video")) {
                           String[] CONTENT_PROJECTION = { Video.Media.DATA, Video.Media.DISPLAY_NAME, Video.Media.MIME_TYPE, Video.Media.SIZE, Video.Media.DATE_MODIFIED };
                           Cursor c = getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null);
                           c.moveToFirst();
                           int index = c.getColumnIndex(Video.Media.DATA);
                           String data = c.getString(index);
                           local.add(data);
                           remote.add(mUploadPath + c.getString(c.getColumnIndex(Video.Media.DISPLAY_NAME)));
                          
                       }
                       else if (mimeType.contains("audio")) {
                           String[] CONTENT_PROJECTION = { Audio.Media.DATA, Audio.Media.DISPLAY_NAME, Audio.Media.MIME_TYPE, Audio.Media.SIZE };
                           Cursor c = getContentResolver().query(uri, CONTENT_PROJECTION, null, null, null);
                           c.moveToFirst();
                           int index = c.getColumnIndex(Audio.Media.DATA);
                           String data = c.getString(index);
                           local.add(data);
                           remote.add(mUploadPath + c.getString(c.getColumnIndex(Audio.Media.DISPLAY_NAME)));
                        
                       }
                       else {
                           String filePath = Uri.decode(uri.toString()).replace(uri.getScheme() + "://", "");
                           // cut everything whats before mnt. It occured to me that sometimes apps send their name into the URI
                           if (filePath.contains("mnt")) {
                              String splitedFilePath[] = filePath.split("/mnt");
                              filePath = splitedFilePath[1];
                           }
                           final File file = new File(filePath);
                           local.add(file.getAbsolutePath());
                           remote.add(mUploadPath + file.getName());
                       }
                        
                    } else if (uri.getScheme().equals("file")) {
                        String filePath = Uri.decode(uri.toString()).replace(uri.getScheme() + "://", "");
                        if (filePath.contains("mnt")) {
                           String splitedFilePath[] = filePath.split("/mnt");
                           filePath = splitedFilePath[1];
                        }
                        final File file = new File(filePath);
                        local.add(file.getAbsolutePath());
                        remote.add(mUploadPath + file.getName());
                    }
                    else {
                        throw new SecurityException();
                    }
                }
                else {
                    throw new SecurityException();
                }
           
            Intent intent = new Intent(getApplicationContext(), FileUploader.class);
            intent.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_MULTIPLE_FILES);
            intent.putExtra(FileUploader.KEY_LOCAL_FILE, local.toArray(new String[local.size()]));
            intent.putExtra(FileUploader.KEY_REMOTE_FILE, remote.toArray(new String[remote.size()]));
            intent.putExtra(FileUploader.KEY_ACCOUNT, mAccount);
            startService(intent);

            //Save the path to shared preferences
            SharedPreferences.Editor appPrefs = PreferenceManager
                    .getDefaultSharedPreferences(getApplicationContext()).edit();
            appPrefs.putString("last_upload_path", mUploadPath);
            appPrefs.apply();

            finish();
            }
            
        } catch (SecurityException e) {
            String message = String.format(getString(R.string.uploader_error_forbidden_content), getString(R.string.app_name));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();            
        }
    }
    
    /**
     *  Loads the target folder initialize shown to the user.
     * 
     *  The target account has to be chosen before this method is called. 
     */
    private void initTargetFolder() {
        if (mStorageManager == null) {
            throw new IllegalStateException("Do not call this method before initializing mStorageManager");
        }
        
        SharedPreferences appPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        String last_path = appPreferences.getString("last_upload_path", "");
        // "/" equals root-directory
        if(last_path.equals("/")) {
            mParents.add("");
        }
        else{
            String[] dir_names = last_path.split("/");
            for (String dir : dir_names)
                mParents.add(dir);
        }
        //Make sure that path still exists, if it doesn't pop the stack and try the previous path
            while(!mStorageManager.fileExists(generatePath(mParents)) && mParents.size() > 1){
                mParents.pop();
            }
    }

    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
        case android.R.id.home: {
            if((mParents.size() > 1)) {                
                onBackPressed(); 
            }
            break;
        }
        default:
            retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    
}
