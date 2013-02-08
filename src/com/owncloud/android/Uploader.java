/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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
package com.owncloud.android;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.network.OwnCloudClientUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavClient;

/**
 * This can be used to upload things to an ownCloud instance.
 * 
 * @author Bartek Przybylski
 * 
 */
public class Uploader extends ListActivity implements OnItemClickListener, android.view.View.OnClickListener {
    private static final String TAG = "ownCloudUploader";

    private Account mAccount;
    private AccountManager mAccountManager;
    private Stack<String> mParents;
    private ArrayList<Parcelable> mStreamsToUpload;
    private boolean mCreateDir;
    private String mUploadPath;
    private static final String[] CONTENT_PROJECTION = { Media.DATA, Media.DISPLAY_NAME, Media.MIME_TYPE, Media.SIZE };
    private DataStorageManager mStorageManager;
    private OCFile mFile;

    private final static int DIALOG_NO_ACCOUNT = 0;
    private final static int DIALOG_WAITING = 1;
    private final static int DIALOG_NO_STREAM = 2;
    private final static int DIALOG_MULTIPLE_ACCOUNT = 3;
    //private final static int DIALOG_GET_DIRNAME = 4;

    private final static int REQUEST_CODE_SETUP_ACCOUNT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        mParents = new Stack<String>();
        mParents.add("");
        /*if (getIntent().hasExtra(Intent.EXTRA_STREAM)) {
            prepareStreamsToUpload();*/
        if (prepareStreamsToUpload()) {
            mAccountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
            Account[] accounts = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
            if (accounts.length == 0) {
                Log.i(TAG, "No ownCloud account is available");
                showDialog(DIALOG_NO_ACCOUNT);
            } else if (accounts.length > 1) {
                Log.i(TAG, "More then one ownCloud is available");
                showDialog(DIALOG_MULTIPLE_ACCOUNT);
            } else {
                mAccount = accounts[0];
                mStorageManager = new FileDataStorageManager(mAccount, getContentResolver());
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
                        Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                        intent.putExtra("authorities", new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
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
        /*case DIALOG_GET_DIRNAME:
            final EditText dirName = new EditText(getBaseContext());
            builder.setView(dirName);
            builder.setTitle(R.string.uploader_info_dirname);
            String pathToUpload;
            if (mParents.empty()) {
                pathToUpload = "/";
            } else {
                mCursor = managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, mParents.peek()), null,
                        null, null, null);
                mCursor.moveToFirst();
                pathToUpload = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_PATH))
                        + mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_NAME)).replace(" ", "%20");   // TODO don't make this ; use WebdavUtils.encode in the right moment
            }
            a a = new a(pathToUpload, dirName);
            builder.setPositiveButton(R.string.common_ok, a);
            builder.setNegativeButton(R.string.common_cancel, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });
            return builder.create();*/
        case DIALOG_MULTIPLE_ACCOUNT:
            CharSequence ac[] = new CharSequence[mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE).length];
            for (int i = 0; i < ac.length; ++i) {
                ac[i] = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)[i].name;
            }
            builder.setTitle(R.string.common_choose_account);
            builder.setItems(ac, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAccount = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)[which];
                    mStorageManager = new FileDataStorageManager(mAccount, getContentResolver());
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
        Log.d(TAG, "on item click");
        Vector<OCFile> tmpfiles = mStorageManager.getDirectoryContent(mFile);
        if (tmpfiles.size() <= 0) return;
        // filter on dirtype
        Vector<OCFile> files = new Vector<OCFile>();
        for (OCFile f : tmpfiles)
            if (f.isDirectory())
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
            Log.d(TAG, "Uploading file to dir " + mUploadPath);

            uploadFiles();

            break;
        /*case android.R.id.button1: // dynamic action for create aditional dir
            showDialog(DIALOG_GET_DIRNAME);
            break;*/
        default:
            throw new IllegalArgumentException("Wrong element clicked");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "result received. req: " + requestCode + " res: " + resultCode);
        if (requestCode == REQUEST_CODE_SETUP_ACCOUNT) {
            dismissDialog(DIALOG_NO_ACCOUNT);
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            Account[] accounts = mAccountManager.getAccountsByType(AccountAuthenticator.AUTH_TOKEN_TYPE);
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

        String full_path = "";
        for (String a : mParents)
            full_path += a + "/";
        
        Log.d(TAG, "Populating view with content of : " + full_path);
        
        mFile = mStorageManager.getFileByPath(full_path);
        if (mFile != null) {
            Vector<OCFile> files = mStorageManager.getDirectoryContent(mFile);
            List<HashMap<String, Object>> data = new LinkedList<HashMap<String,Object>>();
            for (OCFile f : files) {
                HashMap<String, Object> h = new HashMap<String, Object>();
                if (f.isDirectory()) {
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
            WebdavClient wdc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getApplicationContext());

            // create last directory in path if necessary
            if (mCreateDir) {
                wdc.createDirectory(mUploadPath);
            }

            String[] local = new String[mStreamsToUpload.size()], remote = new String[mStreamsToUpload.size()];

            for (int i = 0; i < mStreamsToUpload.size(); ++i) {
                Uri uri = (Uri) mStreamsToUpload.get(i);
                if (uri.getScheme().equals("content")) {
                    Cursor c = getContentResolver().query((Uri) mStreamsToUpload.get(i),
                                                      CONTENT_PROJECTION,
                                                      null,
                                                      null,
                                                      null);

                    if (!c.moveToFirst())
                        continue;

                    final String display_name = c.getString(c.getColumnIndex(Media.DISPLAY_NAME)),
                                data = c.getString(c.getColumnIndex(Media.DATA));
                    local[i] = data;
                    remote[i] = mUploadPath + display_name;
                } else if (uri.getScheme().equals("file")) {
                    final File file = new File(Uri.decode(uri.toString()).replace(uri.getScheme() + "://", ""));
                    local[i] = file.getAbsolutePath();
                    remote[i] = mUploadPath + file.getName();
                }

            }
            Intent intent = new Intent(getApplicationContext(), FileUploader.class);
            intent.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_MULTIPLE_FILES);
            intent.putExtra(FileUploader.KEY_LOCAL_FILE, local);
            intent.putExtra(FileUploader.KEY_REMOTE_FILE, remote);
            intent.putExtra(FileUploader.KEY_ACCOUNT, mAccount);
            startService(intent);
            finish();
            
        } catch (SecurityException e) {
            String message = String.format(getString(R.string.uploader_error_forbidden_content), getString(R.string.app_name));
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();            
        }
    }

}
