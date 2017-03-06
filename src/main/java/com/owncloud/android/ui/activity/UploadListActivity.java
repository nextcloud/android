/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   @author David A. Velasco
 *   @author masensio
 *   Copyright (C) 2016 ownCloud Inc.
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
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.CheckCurrentCredentialsOperation;
import com.owncloud.android.ui.fragment.UploadListFragment;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;

/**
 * Activity listing pending, active, and completed uploads. User can delete
 * completed uploads from view. Content of this list of coming from
 * {@link UploadsStorageManager}.
 *
 */
public class UploadListActivity extends FileActivity implements UploadListFragment.ContainerActivity {

    private static final String TAG = UploadListActivity.class.getSimpleName();

    private static final String TAG_UPLOAD_LIST_FRAGMENT = "UPLOAD_LIST_FRAGMENT";

    private static final String SCREEN_NAME = "Uploads";

    private UploadMessagesReceiver mUploadMessagesReceiver;

    @Override
    public void showFiles(boolean onDeviceOnly) {
        super.showFiles(onDeviceOnly);
        Intent i = new Intent(getApplicationContext(), FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.upload_list_layout);

        // this activity has no file really bound, it's for mulitple accounts at the same time; should no inherit
        // from FileActivity; moreover, some behaviours inherited from FileActivity should be delegated to Fragments;
        // but that's other story
        setFile(null);

        // setup toolbar
        setupToolbar();

        // setup drawer
        setupDrawer(R.id.nav_uploads);

        // Add fragment with a transaction for setting a tag
        if(savedInstanceState == null) {
            createUploadListFragment();
        } // else, the Fragment Manager makes the job on configuration changes

        getSupportActionBar().setTitle(getString(R.string.uploads_view_title));
    }

    private void createUploadListFragment(){
        UploadListFragment uploadList = new UploadListFragment();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.upload_list_fragment, uploadList, TAG_UPLOAD_LIST_FRAGMENT);
        transaction.commit();
    }


    @Override
    protected void onResume() {
        Log_OC.v(TAG, "onResume() start");
        super.onResume();

        MainApp.getFirebaseAnalyticsInstance().setCurrentScreen(this, SCREEN_NAME, TAG);

        // Listen for upload messages
        mUploadMessagesReceiver = new UploadMessagesReceiver();
        IntentFilter uploadIntentFilter = new IntentFilter();
        uploadIntentFilter.addAction(FileUploader.getUploadsAddedMessage());
        uploadIntentFilter.addAction(FileUploader.getUploadStartMessage());
        uploadIntentFilter.addAction(FileUploader.getUploadFinishMessage());
        registerReceiver(mUploadMessagesReceiver, uploadIntentFilter);

        Log_OC.v(TAG, "onResume() end");

    }

    @Override
    protected void onPause() {
        Log_OC.v(TAG, "onPause() start");
        if (mUploadMessagesReceiver != null) {
            unregisterReceiver(mUploadMessagesReceiver);
            mUploadMessagesReceiver = null;
        }
        super.onPause();
        Log_OC.v(TAG, "onPause() end");
    }

    // ////////////////////////////////////////
    // UploadListFragment.ContainerActivity
    // ////////////////////////////////////////
    @Override
    public boolean onUploadItemClick(OCUpload file) {
        /// TODO is this path still active?
        File f = new File(file.getLocalPath());
        if(!f.exists()) {
            Toast.makeText(this, "Cannot open. Local file does not exist.",
                    Toast.LENGTH_SHORT).show();
        } else {
            openFileWithDefault(file.getLocalPath());
        }
        return true;
    }

    /**
     * Open file with app associates with its MIME type. If MIME type unknown, show list with all apps.
     */
    private void openFileWithDefault(String localPath) {
        Intent myIntent = new Intent(Intent.ACTION_VIEW);
        File file = new File(localPath);
        String mimetype = MimeTypeUtil.getBestMimeTypeByFilename(localPath);
        if ("application/octet-stream".equals(mimetype)) {
            mimetype = "*/*";
        }
        myIntent.setDataAndType(Uri.fromFile(file), mimetype);
        try {
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Found no app to open this file.", Toast.LENGTH_LONG).show();
            Log_OC.i(TAG, "Could not find app for sending log history.");

        }        
    }
    
    /**
     * Same as openFileWithDefault() but user cannot save default app.
     * @param ocFile
     */
    private void openFileWithDefaultNoDefault(OCFile ocFile) {
        getFileOperationsHelper().openFile(ocFile);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        UploadsStorageManager storageManager = null;
        UploadListFragment uploadListFragment =
                (UploadListFragment) getSupportFragmentManager().findFragmentByTag(TAG_UPLOAD_LIST_FRAGMENT);
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isDrawerOpen()) {
                    closeDrawer();
                } else {
                    openDrawer();
                }
            case R.id.action_retry_uploads:
                FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
                requester.retryFailedUploads(this, null, null);
                break;

            case R.id.action_clear_failed_uploads:
                storageManager = new UploadsStorageManager(getContentResolver(), getApplicationContext());
                storageManager.clearFailedButNotDelayedUploads();
                uploadListFragment.updateUploads();
                break;

            case R.id.action_clear_successfull_uploads:
                storageManager = new UploadsStorageManager(getContentResolver(), getApplicationContext());
                storageManager.clearSuccessfulUploads();
                uploadListFragment.updateUploads();
                break;

            case R.id.action_clear_finished_uploads:
                storageManager = new UploadsStorageManager(getContentResolver(), getApplicationContext());
                storageManager.clearAllFinishedButNotDelayedUploads();
                uploadListFragment.updateUploads();
                break;

            default:
                retval = super.onOptionsItemSelected(item);
        }

        return retval;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.upload_list_menu, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FileActivity.REQUEST_CODE__UPDATE_CREDENTIALS && resultCode == RESULT_OK) {
            // Retry uploads of the updated account
            Account account = AccountUtils.getOwnCloudAccountByName(
                this,
                data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            );
            FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
            requester.retryFailedUploads(
                this,
                account,
                UploadResult.CREDENTIAL_ERROR
            );
        }
    }

    /**
     *
     * @param operation     Operation performed.
     * @param result        Result of the removal.
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof CheckCurrentCredentialsOperation) {
            // Do not call super in this case; more refactoring needed around onRemoteOeprationFinish :'(
            getFileOperationsHelper().setOpIdWaitingFor(Long.MAX_VALUE);
            dismissLoadingDialog();
            Account account = (Account) result.getData().get(0);
            if (!result.isSuccess()) {
                requestCredentialsUpdate(this, account);

            } else {
                // already updated -> just retry!
                FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
                requester.retryFailedUploads(this, account, UploadResult.CREDENTIAL_ERROR);
            }

        } else {
            super.onRemoteOperationFinish(operation, result);
        }
    }


    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new UploadListServiceConnection();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private class UploadListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (service instanceof FileUploaderBinder) {
                if(mUploaderBinder == null)
                {
                    mUploaderBinder = (FileUploaderBinder) service;
                    Log_OC.d(TAG, "UploadListActivity connected to Upload service. component: " +
                            component + " service: "
                            + service);
                    // Say to UploadListFragment that the Binder is READY in the Activity
                    UploadListFragment uploadListFragment =
                            (UploadListFragment) getSupportFragmentManager().findFragmentByTag(TAG_UPLOAD_LIST_FRAGMENT);
                    if (uploadListFragment != null) {
                        uploadListFragment.binderReady();
                    }
                } else {
                    Log_OC.d(TAG, "mUploaderBinder already set. mUploaderBinder: " +
                            mUploaderBinder + " service:" + service);
                }
            } else {
                Log_OC.d(TAG, "UploadListActivity not connected to Upload service. component: " +
                        component + " service: " + service);
                return;
            }            
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(UploadListActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "UploadListActivity suddenly disconnected from Upload service");
                mUploaderBinder = null;
            }
        }
    };

    /**
     * Once the file upload has changed its status -> update uploads list view
     */
    private class UploadMessagesReceiver extends BroadcastReceiver {
        /**
         * {@link BroadcastReceiver} to enable syncing feedback in UI
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                UploadListFragment uploadListFragment =
                    (UploadListFragment) getSupportFragmentManager().findFragmentByTag(TAG_UPLOAD_LIST_FRAGMENT);

                uploadListFragment.updateUploads();
            } finally {
                if (intent != null) {
                    removeStickyBroadcast(intent);
                }
            }

        }
    }

    protected String getDefaultTitle() {
        return getString(R.string.uploads_view_title);
    }


    /**
     * Called when the ownCloud {@link Account} associated to the Activity was just updated.
     */
    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        getSupportActionBar().setTitle(getString(R.string.uploads_view_title));
        if (mAccountWasSet) {
            setAccountInDrawer(getAccount());
        }
    }

}
