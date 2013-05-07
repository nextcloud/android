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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.AccountUtils;
import com.owncloud.android.Log_OC;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewVideoActivity;

/**
 * This activity displays the details of a file like its name, its size and so
 * on.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class FileDetailActivity extends FileActivity implements FileFragment.ContainerActivity {
    
    public static final int DIALOG_SHORT_WAIT = 0;

    public static final String TAG = FileDetailActivity.class.getSimpleName();
    
    public static final String EXTRA_MODE = "MODE";
    public static final int MODE_DETAILS = 0;
    public static final int MODE_PREVIEW = 1;

    public static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    
    private FileDownloaderBinder mDownloaderBinder = null;
    private ServiceConnection mDownloadConnection, mUploadConnection = null;
    private FileUploaderBinder mUploaderBinder = null;
    private boolean mWaitingToPreview;
    
    private FileDataStorageManager mStorageManager;
    private DownloadFinishReceiver mDownloadFinishReceiver;

    private Configuration mNewConfigurationChangeToApplyOnStart;

    private boolean mStarted;

    private boolean mDualPane;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStarted = false;
        
        // check if configuration is proper for this activity; tablets in landscape should pass the torch to FileDisplayActivity 
        Configuration conf = getResources().getConfiguration();
        mDualPane = (conf.orientation == Configuration.ORIENTATION_LANDSCAPE && 
                        (conf.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
                    );

        if (mDualPane) {
            // only happens when notifications (downloads, uploads) are clicked at the notification bar
            backToDisplayActivity(false);
            
        } else {
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
        }
    }
    
    /**
     * Creates the proper fragment depending upon the state of the handled {@link OCFile} and
     * the requested {@link Intent}.
     */
    private void createChildFragment() {
        int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_PREVIEW); 
        
        Fragment newFragment = null;
        OCFile file = getFile();
        Account account = getAccount();
        if (PreviewMediaFragment.canBePreviewed(file) && mode == MODE_PREVIEW) {
            if (file.isDown()) {
                int startPlaybackPosition = getIntent().getIntExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0);
                boolean autoplay = getIntent().getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, true);
                newFragment = new PreviewMediaFragment(file, account, startPlaybackPosition, autoplay);
            
            } else {
                newFragment = new FileDetailFragment(file, account);
                mWaitingToPreview = true;
            }
            
        } else {
            newFragment = new FileDetailFragment(file, account);
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment, newFragment, FileDetailFragment.FTAG);
        ft.commit();
    }
    
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        Log_OC.e(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mStarted) {
            checkConfigurationChange(newConfig);
        } else {
            mNewConfigurationChangeToApplyOnStart = newConfig;
        }
    }
    
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WAITING_TO_PREVIEW, mWaitingToPreview);
    }
    
    
    @Override
    public void onStart() {
        super.onStart();
        Log_OC.e(TAG, "onStart");
        if (mNewConfigurationChangeToApplyOnStart != null && !isRedirectingToSetupAccount()) {
            checkConfigurationChange(mNewConfigurationChangeToApplyOnStart);
            mNewConfigurationChangeToApplyOnStart = null;
        }
        mStarted = true;
    }

    private void checkConfigurationChange(Configuration newConfig) {
        finish();
        Intent intent = null;
        OCFile file = getFile();
        Account account = getAccount();
        if ((newConfig.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE
                && newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            
            intent = new Intent(this, FileDisplayActivity.class);
            intent.putExtra(EXTRA_FILE, file);
            intent.putExtra(EXTRA_ACCOUNT, account);
            intent.putExtra(EXTRA_MODE, getIntent().getIntExtra(EXTRA_MODE, MODE_PREVIEW));
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fragment != null && file != null && fragment instanceof PreviewMediaFragment && file.isVideo()) {
                PreviewMediaFragment videoFragment = (PreviewMediaFragment)fragment;
                intent.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, videoFragment.getPosition());
                intent.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, videoFragment.isPlaying());
            }
        
        } else {
            intent = new Intent(this, FileDetailActivity.class);
            intent .putExtra(EXTRA_FILE, file);
            intent .putExtra(EXTRA_ACCOUNT, account);
            intent.putExtra(EXTRA_MODE, getIntent().getIntExtra(EXTRA_MODE, MODE_PREVIEW));
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fragment != null && file != null && fragment instanceof PreviewMediaFragment && file.isVideo()) {
                PreviewMediaFragment videoFragment = (PreviewMediaFragment)fragment;
                intent.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, videoFragment.getPosition());
                intent.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, videoFragment.isPlaying());
            }
            // and maybe 'waiting to preview' flag
        }
        startActivity(intent);
    }

    @Override
    public void onStop() {
        super.onStop();
        Log_OC.e(TAG, "onStop");
        mStarted = false;
    }
    @Override
    public void onPause() {
        super.onPause();
        Log_OC.e(TAG, "onPause");
        if (mDownloadFinishReceiver != null) {
            unregisterReceiver(mDownloadFinishReceiver);
            mDownloadFinishReceiver = null;
        }
    }

    
    @Override
    public void onResume() {
        super.onResume();
        Log_OC.e(TAG, "onResume");
        // TODO this is probably unnecessary
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
        if (fragment != null && fragment instanceof FileDetailFragment) {
            ((FileDetailFragment) fragment).updateFileDetails(false, false);
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
                Log_OC.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mWaitingToPreview) {
                    requestForDownload();
                }
                    
            } else if (component.equals(new ComponentName(FileDetailActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service connected");
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
                Log_OC.d(TAG, "Download service disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(FileDetailActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service disconnected");
                mUploaderBinder = null;
            }
        }
    };    
    
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log_OC.e(TAG,  "onDestroy");
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

    @Override
    public void onBackPressed() {
        backToDisplayActivity(true);
    }
    
    private void backToDisplayActivity(boolean moveToParent) {
        Intent intent = new Intent(this, FileDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        OCFile targetFile = null;
        OCFile file = getFile();
        if (file != null) {
            targetFile = moveToParent ? mStorageManager.getFileById(file.getParentId()) : file;
        }
        intent.putExtra(EXTRA_FILE, targetFile);
        intent.putExtra(EXTRA_ACCOUNT, getAccount());
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
        transaction.replace(R.id.fragment, new FileDetailFragment(file, getAccount()), FileDetailFragment.FTAG); 
        transaction.commit();
    }

    
    private void requestForDownload() {
        if (!mDownloaderBinder.isDownloading(getAccount(), getFile())) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, getAccount());
            i.putExtra(FileDownloader.EXTRA_FILE, getFile());
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
            boolean samePath = (getFile() != null && getFile().getRemotePath().equals(downloadedRemotePath));
            
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
                    setFile(mStorageManager.getFileById(getFile().getFileId()));   // update the file from database, for the local storage path
                    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragment, new PreviewMediaFragment(getFile(), getAccount(), 0, true), FileDetailFragment.FTAG);
                    transaction.commit();
                    mWaitingToPreview = false;
                    
                } else {
                    detailsFragment.updateFileDetails(false, (success));
                    // TODO error message if !success ¿?
                }
            }
        } // TODO else if (fragment != null && fragment )
        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onAccountChanged() {
        mStorageManager = new FileDataStorageManager(getAccount(), getContentResolver());
        
        FileFragment fragment = (FileFragment) getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
        if (fragment != null && mStorageManager.getFileById(fragment.getFile().getFileId()) == null) {
            /// the account was forced to be changed; probably was deleted from system settings
            backToDisplayActivity(false);
        }
    }

}
