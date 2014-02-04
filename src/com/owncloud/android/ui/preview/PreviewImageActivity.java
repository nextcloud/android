/* ownCloud Android client application
 *   Copyright (C) 2012-2013  ownCloud Inc.
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
package com.owncloud.android.ui.preview;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.PinCodeActivity;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.Log_OC;


/**
 *  Holds a swiping galley where image files contained in an ownCloud directory are shown
 *  
 *  @author David A. Velasco
 */
public class PreviewImageActivity extends FileActivity implements FileFragment.ContainerActivity, ViewPager.OnPageChangeListener, OnTouchListener {
    
    public static final int DIALOG_SHORT_WAIT = 0;

    public static final String TAG = PreviewImageActivity.class.getSimpleName();
    
    public static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    private static final String KEY_WAITING_FOR_BINDER = "WAITING_FOR_BINDER";
    
    private static final String DIALOG_WAIT_TAG = "DIALOG_WAIT";
    
    private ViewPager mViewPager; 
    private PreviewImagePagerAdapter mPreviewImagePagerAdapter;    
    
    private FileDownloaderBinder mDownloaderBinder = null;
    private ServiceConnection mDownloadConnection, mUploadConnection = null;
    private FileUploaderBinder mUploaderBinder = null;

    private boolean mRequestWaitingForBinder;
    
    private DownloadFinishReceiver mDownloadFinishReceiver;

    private boolean mFullScreen;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.preview_image_activity);
        
        ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(DisplayUtils.getSeasonalIconId());
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.hide();
        
        // PIN CODE request
        if (getIntent().getExtras() != null && savedInstanceState == null && fromNotification()) {
            requestPinCode();
        }         
        
        mFullScreen = true;
        if (savedInstanceState != null) {
            mRequestWaitingForBinder = savedInstanceState.getBoolean(KEY_WAITING_FOR_BINDER);
        } else {
            mRequestWaitingForBinder = false;
        }
        
    }

    private void initViewPager() {
        // get parent from path
        String parentPath = getFile().getRemotePath().substring(0, getFile().getRemotePath().lastIndexOf(getFile().getFileName()));
        OCFile parentFolder = getStorageManager().getFileByPath(parentPath);
        if (parentFolder == null) {
            // should not be necessary
            parentFolder = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
        }
        mPreviewImagePagerAdapter = new PreviewImagePagerAdapter(getSupportFragmentManager(), parentFolder, getAccount(), getStorageManager());
        mViewPager = (ViewPager) findViewById(R.id.fragmentPager);
        int position = mPreviewImagePagerAdapter.getFilePosition(getFile());
        position = (position >= 0) ? position : 0;
        mViewPager.setAdapter(mPreviewImagePagerAdapter); 
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setCurrentItem(position);
        if (position == 0 && !getFile().isDown()) {
            // this is necessary because mViewPager.setCurrentItem(0) just after setting the adapter does not result in a call to #onPageSelected(0) 
            mRequestWaitingForBinder = true;
        }
    }
    
    
    @Override
    public void onStart() {
        super.onStart();
        mDownloadConnection = new PreviewImageServiceConnection();
        bindService(new Intent(this, FileDownloader.class), mDownloadConnection, Context.BIND_AUTO_CREATE);
        mUploadConnection = new PreviewImageServiceConnection();
        bindService(new Intent(this, FileUploader.class), mUploadConnection, Context.BIND_AUTO_CREATE);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WAITING_FOR_BINDER, mRequestWaitingForBinder);    
    }


    /** Defines callbacks for service binding, passed to bindService() */
    private class PreviewImageServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
                
            if (component.equals(new ComponentName(PreviewImageActivity.this, FileDownloader.class))) {
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mRequestWaitingForBinder) {
                    mRequestWaitingForBinder = false;
                    Log_OC.d(TAG, "Simulating reselection of current page after connection of download binder");
                    onPageSelected(mViewPager.getCurrentItem());
                }

            } else if (component.equals(new ComponentName(PreviewImageActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(PreviewImageActivity.this, FileDownloader.class))) {
                Log_OC.d(TAG, "Download service suddenly disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(PreviewImageActivity.this, FileUploader.class))) {
                Log_OC.d(TAG, "Upload service suddenly disconnected");
                mUploaderBinder = null;
            }
        }
    };    
    
    
    @Override
    public void onStop() {
        super.onStop();
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
    public void onDestroy() {
        super.onDestroy();
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
        //Log.e(TAG, "ACTIVITY, ONRESUME");
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        
        IntentFilter filter = new IntentFilter(FileDownloader.getDownloadFinishMessage());
        filter.addAction(FileDownloader.getDownloadAddedMessage());
        registerReceiver(mDownloadFinishReceiver, filter);
    }

    @Override
    protected void onPostResume() {
        //Log.e(TAG, "ACTIVITY, ONPOSTRESUME");
        super.onPostResume();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mDownloadFinishReceiver);
        mDownloadFinishReceiver = null;
    }
    

    private void backToDisplayActivity() {
        finish();
    }
    
    /**
     * Show loading dialog 
     */
    public void showLoadingDialog() {
        // Construct dialog
        LoadingDialog loading = new LoadingDialog(getResources().getString(R.string.wait_a_moment));
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        loading.show(ft, DIALOG_WAIT_TAG);
        
    }
    
    /**
     * Dismiss loading dialog
     */
    public void dismissLoadingDialog(){
        Fragment frag = getSupportFragmentManager().findFragmentByTag(DIALOG_WAIT_TAG);
      if (frag != null) {
          LoadingDialog loading = (LoadingDialog) frag;
            loading.dismiss();
        }
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
    public void showDetails(OCFile file) {
        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        showDetailsIntent.setAction(FileDisplayActivity.ACTION_DETAILS);
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, file);
        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, AccountUtils.getCurrentOwnCloudAccount(this));
        startActivity(showDetailsIntent);
        int pos = mPreviewImagePagerAdapter.getFilePosition(file);
        file = mPreviewImagePagerAdapter.getFileAt(pos);
        
    }

    
    private void requestForDownload(OCFile file) {
        if (mDownloaderBinder == null) {
            Log_OC.d(TAG, "requestForDownload called without binder to download service");
            
        } else if (!mDownloaderBinder.isDownloading(getAccount(), file)) {
            Intent i = new Intent(this, FileDownloader.class);
            i.putExtra(FileDownloader.EXTRA_ACCOUNT, getAccount());
            i.putExtra(FileDownloader.EXTRA_FILE, file);
            startService(i);
        }
    }

    /**
     * This method will be invoked when a new page becomes selected. Animation is not necessarily complete.
     * 
     *  @param  Position        Position index of the new selected page
     */
    @Override
    public void onPageSelected(int position) {
        if (mDownloaderBinder == null) {
            mRequestWaitingForBinder = true;
            
        } else {
            OCFile currentFile = mPreviewImagePagerAdapter.getFileAt(position); 
            getSupportActionBar().setTitle(currentFile.getFileName());
            if (!currentFile.isDown()) {
                if (!mPreviewImagePagerAdapter.pendingErrorAt(position)) {
                    requestForDownload(currentFile);
                }
            }
        }
    }
    
    /**
     * Called when the scroll state changes. Useful for discovering when the user begins dragging, 
     * when the pager is automatically settling to the current page. when it is fully stopped/idle.
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
    

    /**
     * Class waiting for broadcast events from the {@link FielDownloader} service.
     * 
     * Updates the UI when a download is started or finished, provided that it is relevant for the
     * folder displayed in the gallery.
     */
    private class DownloadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String accountName = intent.getStringExtra(FileDownloader.ACCOUNT_NAME);
            String downloadedRemotePath = intent.getStringExtra(FileDownloader.EXTRA_REMOTE_PATH);
            if (getAccount().name.equals(accountName) && 
                    downloadedRemotePath != null) {

                OCFile file = getStorageManager().getFileByPath(downloadedRemotePath);
                int position = mPreviewImagePagerAdapter.getFilePosition(file);
                boolean downloadWasFine = intent.getBooleanExtra(FileDownloader.EXTRA_DOWNLOAD_RESULT, false);
                //boolean isOffscreen =  Math.abs((mViewPager.getCurrentItem() - position)) <= mViewPager.getOffscreenPageLimit();
                
                if (position >= 0 && intent.getAction().equals(FileDownloader.getDownloadFinishMessage())) {
                    if (downloadWasFine) {
                        mPreviewImagePagerAdapter.updateFile(position, file);   
                        
                    } else {
                        mPreviewImagePagerAdapter.updateWithDownloadError(position);
                    }
                    mPreviewImagePagerAdapter.notifyDataSetChanged();   // will trigger the creation of new fragments
                    
                } else {
                    Log_OC.d(TAG, "Download finished, but the fragment is offscreen");
                }
                
            }
            removeStickyBroadcast(intent);
        }

    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
           toggleFullScreen();
        }
        return true;
    }

    
    private void toggleFullScreen() {
        ActionBar actionBar = getSupportActionBar();
        if (mFullScreen) {
            actionBar.show();
            
        } else {
            actionBar.hide();
            
        }
        mFullScreen = !mFullScreen;
    }

    @Override
    protected void onAccountSet(boolean stateWasRecovered) {
        super.onAccountSet(stateWasRecovered);
        if (getAccount() != null) {
            OCFile file = getFile();
            /// Validate handled file  (first image to preview)
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (!file.isImage()) {
                throw new IllegalArgumentException("Non-image file passed as argument");
            }
            
            // Update file according to DB file, if it is possible
            if (file.getFileId() > FileDataStorageManager.ROOT_PARENT_ID)            
                file = getStorageManager().getFileById(file.getFileId());
            
            if (file != null) {
                /// Refresh the activity according to the Account and OCFile set
                setFile(file);  // reset after getting it fresh from storageManager
                getSupportActionBar().setTitle(getFile().getFileName());
                //if (!stateWasRecovered) {
                    initViewPager();
                //}

            } else {
                // handled file not in the current Account
                finish();
            }
        }
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
            i.putExtra(PinCodeActivity.EXTRA_ACTIVITY, "PreviewImageActivity");
            startActivity(i);
        }
    }

}
