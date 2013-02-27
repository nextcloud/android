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
package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
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
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;

import com.owncloud.android.R;

/**
 * This activity displays the details of a file like its name, its size and so
 * on.
 * 
 * @author Bartek Przybylski
 * @author David A. Velasco
 */
public class FileDetailActivity extends SherlockFragmentActivity implements FileDetailFragment.ContainerActivity {
    
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
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFile = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE);
        mAccount = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_ACCOUNT);
        
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
            backToDisplayActivity();   // the 'back' won't be effective until this.onStart() and this.onResume() are completed;
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
            backToDisplayActivity();
            returnValue = true;
            break;
        default:
        	returnValue = super.onOptionsItemSelected(item);
        }
        
        return returnValue;
    }



    @Override
    protected void onResume() {
        
        super.onResume();
        if (!mConfigurationChangedToLandscape) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            if (fragment != null && fragment instanceof FileDetailFragment) {
                ((FileDetailFragment) fragment).updateFileDetails(false, false);
            }
        }
    }
    

    private void backToDisplayActivity() {
        Intent intent = new Intent(this, FileDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(FileDetailFragment.EXTRA_FILE, mFile);
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

    @Override
    public void notifySuccessfulDownload(OCFile file, Intent intent, boolean success) {
        if (success) {
            if (mWaitingToPreview) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment, new PreviewMediaFragment(file, mAccount), FileDetailFragment.FTAG); 
                transaction.commit();
                mWaitingToPreview = false;
            }
        }
    }
    
}
