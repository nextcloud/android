/* ownCloud Android client application
 *   Copyright (C) 2012-2013  ownCloud Inc.
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

import java.util.Vector;

import android.accounts.Account;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.datamodel.DataStorageManager;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileDownloadFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.ui.fragment.FilePreviewFragment;
import com.owncloud.android.ui.fragment.PreviewImageFragment;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;

/**
 *  Used as an utility to preview image files contained in an ownCloud account.
 *  
 *  @author David A. Velasco
 */
public class PreviewImageActivity extends SherlockFragmentActivity implements FileFragment.ContainerActivity, ViewPager.OnPageChangeListener {
    
    public static final int DIALOG_SHORT_WAIT = 0;

    public static final String TAG = PreviewImageActivity.class.getSimpleName();
    
    public static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    
    private OCFile mFile;
    private OCFile mParentFolder;  
    private Account mAccount;
    private DataStorageManager mStorageManager;
    
    private ViewPager mViewPager; 
    private PreviewImagePagerAdapter mPreviewImagePagerAdapter;    
    
    private FileDownloaderBinder mDownloaderBinder = null;
    private ServiceConnection mDownloadConnection, mUploadConnection = null;
    private FileUploaderBinder mUploaderBinder = null;
    private boolean mWaitingToPreview;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFile = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_FILE);
        mAccount = getIntent().getParcelableExtra(FileDetailFragment.EXTRA_ACCOUNT);
        if (mFile == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        }
        if (mAccount == null) {
            throw new IllegalStateException("Instanced with a NULL ownCloud Account");
        }
        if (!mFile.isImage()) {
            throw new IllegalArgumentException("Non-image file passed as argument");
        }
        
        setContentView(R.layout.preview_image_activity);
    
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(mFile.getFileName());
        
        mStorageManager = new FileDataStorageManager(mAccount, getContentResolver());
        mParentFolder = mStorageManager.getFileById(mFile.getParentId());
        if (mParentFolder == null) {
            // should not be necessary
            mParentFolder = mStorageManager.getFileByPath(OCFile.PATH_SEPARATOR);
        }

        createViewPager();

        if (savedInstanceState == null) {
            mWaitingToPreview = false;
        } else {
            mWaitingToPreview = savedInstanceState.getBoolean(KEY_WAITING_TO_PREVIEW);
        }
        
        mDownloadConnection = new DetailsServiceConnection();
        bindService(new Intent(this, FileDownloader.class), mDownloadConnection, Context.BIND_AUTO_CREATE);
        mUploadConnection = new DetailsServiceConnection();
        bindService(new Intent(this, FileUploader.class), mUploadConnection, Context.BIND_AUTO_CREATE);
        
    }

    private void createViewPager() {
        mPreviewImagePagerAdapter = new PreviewImagePagerAdapter(getSupportFragmentManager(), mParentFolder);
        mViewPager = (ViewPager) findViewById(R.id.fragmentPager);
        mViewPager.setAdapter(mPreviewImagePagerAdapter);        
        mViewPager.setOnPageChangeListener(this);
    }
    

    /**
     * Adapter class that provides Fragment instances  
     * 
     * @author David A. Velasco
     */
    public class PreviewImagePagerAdapter extends FragmentStatePagerAdapter {
        
        Vector<OCFile> mImageFiles; 
        
        public PreviewImagePagerAdapter(FragmentManager fm, OCFile parentFolder) {
            super(fm);
            mImageFiles = mStorageManager.getDirectoryImages(parentFolder);
        }
    
        @Override
        public Fragment getItem(int i) {
            Log.e(TAG, "GETTING PAGE " + i);
            OCFile file = mImageFiles.get(i);
            Fragment fragment = null;
            if (file.isDown()) {
                fragment = new PreviewImageFragment(file, mAccount);
                mWaitingToPreview = false;
            } else {
                fragment = new FileDownloadFragment(file, mAccount);    // TODO
                //mWaitingToPreview = true;
            }
            return fragment;
        }
    
        @Override
        public int getCount() {
            return mImageFiles.size();
        }
    
        @Override
        public CharSequence getPageTitle(int position) {
            return mImageFiles.get(position).getFileName();
        }
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
                
            if (component.equals(new ComponentName(PreviewImageActivity.this, FileDownloader.class))) {
                Log.d(TAG, "Download service connected");
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mWaitingToPreview) {
                    requestForDownload();
                }
                    
            } else if (component.equals(new ComponentName(PreviewImageActivity.this, FileUploader.class))) {
                Log.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
            FileDetailFragment detailsFragment = (fragment instanceof FileDetailFragment) ? (FileDetailFragment) fragment : null;
            if (detailsFragment != null) {
                detailsFragment.listenForTransferProgress();
                detailsFragment.updateFileDetails(mWaitingToPreview);   // let the fragment gets the mDownloadBinder through getDownloadBinder() (see FileDetailFragment#updateFileDetais())
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(PreviewImageActivity.this, FileDownloader.class))) {
                Log.d(TAG, "Download service disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(PreviewImageActivity.this, FileUploader.class))) {
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
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(FileDetailFragment.FTAG);
        if (fragment != null && fragment instanceof FileDetailFragment) {
            ((FileDetailFragment) fragment).updateFileDetails(false);
        }
    }
    

    private void backToDisplayActivity() {
        /*
        Intent intent = new Intent(this, FileDisplayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(FileDetailFragment.EXTRA_FILE, mFile);
        intent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        startActivity(intent);
        */
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
        Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, file);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
        showDetailsIntent.putExtra(FileDetailActivity.EXTRA_MODE, FileDetailActivity.MODE_DETAILS);
        startActivity(showDetailsIntent);
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
                transaction.replace(R.id.fragment, new FilePreviewFragment(file, mAccount), FileDetailFragment.FTAG); 
                transaction.commit();
                mWaitingToPreview = false;
            }
        }
    }

    
    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily complete.
     * 
     *  @param  Position        Position index of the new selected page
     */
    @Override
    public void onPageSelected(int position) {
        OCFile currentFile = ((FileFragment)mPreviewImagePagerAdapter.getItem(position)).getFile();
        getSupportActionBar().setTitle(currentFile.getFileName());
    }
    
    /**
     * Called when the scroll state changes. Useful for discovering when the user begins dragging, 
     * when the pager is automatically settling to the current page, or when it is fully stopped/idle.
     * 
     * @param   State       The new scroll state (SCROLL_STATE_IDLE, _DRAGGING, _SETTLING
     */
    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part of a programmatically 
     * initiated smooth scroll or a user initiated touch scroll.
     * 
     * @param   position                Position index of the first page currently being displayed. 
     *                                  Page position+1 will be visible if positionOffset is nonzero.
     *                                  
     * @param   positionOffset          Value from [0, 1) indicating the offset from the page at position.
     * @param   positionOffsetPixels    Value in pixels indicating the offset from position. 
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }
    
}
