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

import android.accounts.Account;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;

/**
 * This activity displays the details of a file like its name, its size and so
 * on.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class FileDetailActivity extends SherlockFragmentActivity implements FileFragment.ContainerActivity {
    
    public static final int DIALOG_SHORT_WAIT = 0;

    public static final String TAG = FileDetailActivity.class.getSimpleName();
    
    public static final String EXTRA_MODE = "MODE";
    public static final int MODE_DETAILS = 0;
    public static final int MODE_PREVIEW = 1;

    public static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    
    private boolean mConfigurationChangedToLandscape = false;
    private FileDownloaderBinder mDownloaderBinder = null;
    private ServiceConnection mDownloadConnection, mUploadConnection = null;
    private FileUploaderBinder mUploaderBinder = null;
    private boolean mWaitingToPreview;
    
    private OCFile mFile;
    private Account mAccount;

    private FileDataStorageManager mStorageManager;
    private DownloadFinishReceiver mDownloadFinishReceiver;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFile = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE);
        mAccount = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_ACCOUNT);
        mStorageManager = new FileDataStorageManager(mAccount, getContentResolver());
        
        // check if configuration changed to large-land ; for a tablet being changed from portrait to landscape when in FileDetailActivity 
        Configuration conf = getResources().getConfiguration();
        mConfigurationChangedToLandscape = (conf.orientation == Configuration.ORIENTATION_LANDSCAPE && 
                                                (conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
                                           );

        if (!mConfigurationChangedToLandscape) {
            setContentView(R.layout.file_activity_details);
        
            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);

            if (savedInstanceState == null) {
                mWaitingToPreview = false;
                createChildFragment();
            } else {
                mWaitingToPreview = savedInstanceState.getBoolean(KEY_WAITING_TO_PREVIEW);
            }
            
            mDownloadConnection = new DetailsServiceConnection();
            bindService(new Intent(this, FileDownloader.class), mDownloadConnection, Context.BIND_AUTO_CREATE);
            mUploadConnection = new DetailsServiceConnection();
            bindService(new Intent(this, FileUploader.class), mUploadConnection, Context.BIND_AUTO_CREATE);
            
            
        }  else {
            backToDisplayActivity(false);   // the 'back' won't be effective until this.onStart() and this.onResume() are completed;
        }
        
        
    }

    /**
     * Creates the proper fragment depending upon the state of the handled {@link OCFile} and
     * the requested {@link Intent}.
     */
    private void createChildFragment() {
        int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_PREVIEW); 
        
        Fragment newFragment = null;
        if (PreviewMediaFragment.canBePreviewed(mFile) && mode == MODE_PREVIEW) {
            if (mFile.isDown()) {
                newFragment = new PreviewMediaFragment(mFile, mAccount);
            
            } else {
                newFragment = new FileDetailFragment(mFile, mAccount);
                mWaitingToPreview = true;
            }
            
        } else {
            newFragment = new FileDetailFragment(mFile, mAccount);
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment, newFragment, FileDetailFragment.FTAG);
        ft.commit();
    }
    

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WAITING_TO_PREVIEW, mWaitingToPreview);
    }
    
    
    @Override
    public void onPause() {
        super.onPause();
        if (mDownloadFinishReceiver != null) {
            unregisterReceiver(mDownloadFinishReceiver);
            mDownloadFinishReceiver = null;
        }
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        if (!mConfigurationChangedToLandscape) {
            // TODO this is probably unnecessary
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fragment != null && fragment instanceof FileDetailFragment) {
                ((FileDetailFragment) fragment).updateFileDetails(false, false);
            }
        }
        // Listen for download messages
        IntentFilter downloadIntentFilter = new IntentFilter(FileDownloader.DOWNLOAD_ADDED_MESSAGE);
        downloadIntentFilter.addAction(FileDownloader.DOWNLOAD_FINISH_MESSAGE);
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        registerReceiver(mDownloadFinishReceiver, downloadIntentFilter);
    }
    
    
    /** Defines callbacks for service binding, passed to bindService() */
    private class DetailsServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
                
            if (component.equals(new ComponentName(FileDetailActivity.this, FileDownloader.class))) {
                Log.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mWaitingToPreview) {
                    requestForDownload();
                }
                    
            } else if (component.equals(new ComponentName(FileDetailActivity.this, FileUploader.class))) {
                Log.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            FileDetailFragment detailsFragment = (fragment instanceof FileDetailFragment) ? (FileDetailFragment) fragment : null;
            if (detailsFragment != null) {
                detailsFragment.listenForTransferProgress();
                detailsFragment.updateFileDetails(mWaitingToPreview, false);   // let the fragment gets the mDownloadBinder through getDownloadBinder() (see FileDetailFragment#updateFileDetais())
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(FileDetailActivity.this, FileDownloader.class))) {
                Log.d(TAG, "Download service disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(FileDetailActivity.this, FileUploader.class))) {
                Log.d(TAG, "Upload service disconnected");
                mUploaderBinder = null;
            }
        }
    };    
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDownloadConnection != null) {
            unbindService(mDownloadConnection);
            mDownloadConnection = null;
        }
        if (mUploadConnection != null) {
            unbindService(mUploadConnection);
            mUploadConnection = null;
        }
    }
    
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean returnValue = false;
        
        switch(item.getItemId()){
        case android.R.id.home:
            backToDisplayActivity(true);
            returnValue = true;
            break;
        default:
        	returnValue = super.onOptionsItemSelected(item);
        }
        
        return returnValue;
    }



    private void backToDisplayActivity(boolean moveToParent) {
        Intent intent = new Intent(this, FileDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        OCFile targetFile = null;
        if (mFile != null) {
            targetFile = moveToParent ? mStorageManager.getFileById(mFile.getParentId()) : mFile;
        }
        intent.putExtra(FileDetailFragment.EXTRA_FILE, targetFile);
        intent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        startActivity(intent);
        finish();
    }
    
    
    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog = null;
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
        default:
            dialog = null;
        }
        return dialog;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onFileStateChanged() {
        // nothing to do here!
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public FileDownloaderBinder getFileDownloaderBinder() {
        return mDownloaderBinder;
    }


    @Override
    public FileUploaderBinder getFileUploaderBinder() {
        return mUploaderBinder;
    }


    @Override
    public void showFragmentWithDetails(OCFile file) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment, new FileDetailFragment(file, mAccount), FileDetailFragment.FTAG); 
        transaction.commit();
    }

    
    private void requestForDownload() {
        if (!mDownloaderBinder.isDownloading(mAccount, mFile)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, mAccount);
            i.putExtra(FileDownloader.EXTRA_FILE, mFile);
            startService(i);
        }
    }

    
    /**
     * Class waiting for broadcast events from the {@link FielDownloader} service.
     * 
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * current file.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean sameAccount = isSameAccount(context, intent);
            String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
            boolean samePath = (mFile != null && mFile.getRemotePath().equals(downloadedRemotePath));
            
            if (sameAccount && samePath) {
                updateChildFragment(intent.getAction(), downloadedRemotePath, intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false));
            }
            
            removeStickyBroadcast(intent);
        }

        private boolean isSameAccount(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);
            return (accountName != null && accountName.equals(AccountUtils.getCurrentOwnCloudAccount(context).name));
        }
    }


    public void updateChildFragment(String downloadEvent, String downloadedRemotePath, boolean success) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
        if (fragment != null && fragment instanceof FileDetailFragment) {
            FileDetailFragment detailsFragment = (FileDetailFragment) fragment;
            OCFile fileInFragment = detailsFragment.getFile();
            if (fileInFragment != null && !downloadedRemotePath.equals(fileInFragment.getRemotePath())) {
                // this never should happen; fileInFragment should be always equals to mFile, that was compared to downloadedRemotePath in DownloadReceiver 
                mWaitingToPreview = false;
                
            } else if (downloadEvent.equals(FileDownloader.DOWNLOAD_ADDED_MESSAGE)) {
                // grants that the progress bar is updated
                detailsFragment.listenForTransferProgress();
                detailsFragment.updateFileDetails(true, false);
                
            } else if (downloadEvent.equals(FileDownloader.DOWNLOAD_FINISH_MESSAGE)) {
                //  refresh the details fragment 
                if (success && mWaitingToPreview) {
                    mFile = mStorageManager.getFileById(mFile.getFileId());   // update the file from database, for the local storage path
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment, new PreviewMediaFragment(mFile, mAccount), FileDetailFragment.FTAG);
                    transaction.commit();
                    mWaitingToPreview = false;
                    
                } else {
                    detailsFragment.updateFileDetails(false, (success));
                    // TODO error message if !success ¿?
                }
            }
        } // TODO else if (fragment != null && fragment )
        
        
    }

}
