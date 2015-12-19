/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015  ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hd that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.preview;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;

import com.ortiz.touch.ExtendedViewPager;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.fragment.FileFragment;


/**
 *  Holds a swiping galley where image files contained in an ownCloud directory are shown
 */
public class PreviewImageActivity extends FileActivity implements
        FileFragment.ContainerActivity,
        ViewPager.OnPageChangeListener, OnRemoteOperationListener {
    
    public static final int DIALOG_SHORT_WAIT = 0;

    public static final String TAG = PreviewImageActivity.class.getSimpleName();
    
    public static final String KEY_WAITING_TO_PREVIEW = "WAITING_TO_PREVIEW";
    private static final String KEY_WAITING_FOR_BINDER = "WAITING_FOR_BINDER";

    private static final int INITIAL_HIDE_DELAY = 0; // immediate hide

    private ExtendedViewPager mViewPager;
    private PreviewImagePagerAdapter mPreviewImagePagerAdapter;
    private int mSavedPosition = 0;
    private boolean mHasSavedPosition = false;
    
    private boolean mRequestWaitingForBinder;
    
    private DownloadFinishReceiver mDownloadFinishReceiver;
    
    private View mFullScreenAnchorView;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.preview_image_activity);

        // Navigation Drawer
        initDrawer();

        // ActionBar
        ActionBar actionBar = getSupportActionBar();
        updateActionBarTitleAndHomeButton(null);
        actionBar.hide();


        // Make sure we're running on Honeycomb or higher to use FullScreen and
        // Immersive Mode
        if (isHoneycombOrHigher()) {
        
            mFullScreenAnchorView = getWindow().getDecorView();
            // to keep our UI controls visibility in line with system bars
            // visibility
            mFullScreenAnchorView.setOnSystemUiVisibilityChangeListener
                    (new View.OnSystemUiVisibilityChangeListener() {
                @SuppressLint("InlinedApi")
                @Override
                public void onSystemUiVisibilityChange(int flags) {
                    boolean visible = (flags & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                    ActionBar actionBar = getSupportActionBar();
                    if (visible) {
                        actionBar.show();
                        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    } else {
                        actionBar.hide();
                        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    }
                }
            });

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.owncloud_blue_dark_transparent));
            }
        }
            
        if (savedInstanceState != null) {
            mRequestWaitingForBinder = savedInstanceState.getBoolean(KEY_WAITING_FOR_BINDER);
        } else {
            mRequestWaitingForBinder = false;
        }
        
    }

    private void initViewPager() {
        // get parent from path
        String parentPath = getFile().getRemotePath().substring(0,
                getFile().getRemotePath().lastIndexOf(getFile().getFileName()));
        OCFile parentFolder = getStorageManager().getFileByPath(parentPath);
        if (parentFolder == null) {
            // should not be necessary
            parentFolder = getStorageManager().getFileByPath(OCFile.ROOT_PATH);
        }

        // TODO Enable when "On Device" is recovered ?
        mPreviewImagePagerAdapter = new PreviewImagePagerAdapter(getSupportFragmentManager(),
                parentFolder, getAccount(), getStorageManager()/*, MainApp.getOnlyOnDevice()*/);

        mViewPager = (ExtendedViewPager) findViewById(R.id.fragmentPager);
        int position = mHasSavedPosition ? mSavedPosition :
                mPreviewImagePagerAdapter.getFilePosition(getFile());
        position = (position >= 0) ? position : 0;
        mViewPager.setAdapter(mPreviewImagePagerAdapter); 
        mViewPager.setOnPageChangeListener(this);
        mViewPager.setCurrentItem(position);
        if (position == 0 && !getFile().isDown()) {
            // this is necessary because mViewPager.setCurrentItem(0) just after setting the
            // adapter does not result in a call to #onPageSelected(0)
            mRequestWaitingForBinder = true;
        }
    }
    
    
    protected void onPostCreate(Bundle savedInstanceState)  {
        super.onPostCreate(savedInstanceState);
        
        // Trigger the initial hide() shortly after the activity has been 
        // created, to briefly hint to the user that UI controls 
        // are available
        delayedHide(INITIAL_HIDE_DELAY);
        
    }
    
    Handler mHideSystemUiHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (isHoneycombOrHigher()) {
                hideSystemUI(mFullScreenAnchorView);
            }
            getSupportActionBar().hide();
        }
    };
    
    private void delayedHide(int delayMillis)   {
        mHideSystemUiHandler.removeMessages(0);
        mHideSystemUiHandler.sendEmptyMessageDelayed(0, delayMillis);
    }
    
    
    /// handle Window Focus changes
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        
        // When the window loses focus (e.g. the action overflow is shown),
        // cancel any pending hide action.
        if (!hasFocus) {
            mHideSystemUiHandler.removeMessages(0);
        }
    }
    
    
    
    @Override
    public void onStart() {
        super.onStart();
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_WAITING_FOR_BINDER, mRequestWaitingForBinder);    
    }

    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        super.onRemoteOperationFinish(operation, result);
        
        if (operation instanceof RemoveFileOperation) {
            finish();
        } else if (operation instanceof SynchronizeFileOperation) {
            onSynchronizeFileOperationFinish((SynchronizeFileOperation) operation, result);

        }
    }
    
    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation,
                                                  RemoteOperationResult result) {
        if (result.isSuccess()) {
            invalidateOptionsMenu();
        }

    }

    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new PreviewImageServiceConnection();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private class PreviewImageServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
                
            if (component.equals(new ComponentName(PreviewImageActivity.this,
                    FileDownloader.class))) {
                mDownloaderBinder = (FileDownloaderBinder) service;
                if (mRequestWaitingForBinder) {
                    mRequestWaitingForBinder = false;
                    Log_OC.d(TAG, "Simulating reselection of current page after connection " +
                            "of download binder");
                    onPageSelected(mViewPager.getCurrentItem());
                }

            } else if (component.equals(new ComponentName(PreviewImageActivity.this,
                    FileUploader.class))) {
                Log_OC.d(TAG, "Upload service connected");
                mUploaderBinder = (FileUploaderBinder) service;
            } else {
                return;
            }
            
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(PreviewImageActivity.this,
                    FileDownloader.class))) {
                Log_OC.d(TAG, "Download service suddenly disconnected");
                mDownloaderBinder = null;
            } else if (component.equals(new ComponentName(PreviewImageActivity.this,
                    FileUploader.class))) {
                Log_OC.d(TAG, "Upload service suddenly disconnected");
                mUploaderBinder = null;
            }
        }
    };    
    
    
    @Override
    public void onStop() {
        super.onStop();
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
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawer(GravityCompat.START);
            } else {
                backToDisplayActivity();
            }
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

        mDownloadFinishReceiver = new DownloadFinishReceiver();
        
        IntentFilter filter = new IntentFilter(FileDownloader.getDownloadFinishMessage());
        filter.addAction(FileDownloader.getDownloadAddedMessage());
        registerReceiver(mDownloadFinishReceiver, filter);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }
    
    @Override
    public void onPause() {
        if (mDownloadFinishReceiver != null){
            unregisterReceiver(mDownloadFinishReceiver);
            mDownloadFinishReceiver = null;
        }
        
        super.onPause();
    }
    

    private void backToDisplayActivity() {
        finish();
    }
    
    @Override
    public void showDetails(OCFile file) {
        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        showDetailsIntent.setAction(FileDisplayActivity.ACTION_DETAILS);
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, file);
        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT,
                AccountUtils.getCurrentOwnCloudAccount(this));
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
     * This method will be invoked when a new page becomes selected. Animation is not necessarily
     * complete.
     * 
     *  @param  position        Position index of the new selected page
     */
    @Override
    public void onPageSelected(int position) {
        mSavedPosition = position;
        mHasSavedPosition = true;
        if (mDownloaderBinder == null) {
            mRequestWaitingForBinder = true;
            
        } else {
            OCFile currentFile = mPreviewImagePagerAdapter.getFileAt(position); 
            getSupportActionBar().setTitle(currentFile.getFileName());
            mDrawerToggle.setDrawerIndicatorEnabled(false);
            if (!currentFile.isDown()) {
                if (!mPreviewImagePagerAdapter.pendingErrorAt(position)) {
                    requestForDownload(currentFile);
                }
            }

            // Call to reset image zoom to initial state
            ((PreviewImagePagerAdapter) mViewPager.getAdapter()).resetZoom();
        }

    }
    
    /**
     * Called when the scroll state changes. Useful for discovering when the user begins dragging, 
     * when the pager is automatically settling to the current page. when it is fully stopped/idle.
     * 
     * @param   state       The new scroll state (SCROLL_STATE_IDLE, _DRAGGING, _SETTLING
     */
    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /**
     * This method will be invoked when the current page is scrolled, either as part of a
     * programmatically initiated smooth scroll or a user initiated touch scroll.
     * 
     * @param   position                Position index of the first page currently being displayed. 
     *                                  Page position+1 will be visible if positionOffset is
     *                                  nonzero.
     *                                  
     * @param   positionOffset          Value from [0, 1) indicating the offset from the page
     *                                  at position.
     * @param   positionOffsetPixels    Value in pixels indicating the offset from position. 
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }
    

    /**
     * Class waiting for broadcast events from the {@link FileDownloader} service.
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
                boolean downloadWasFine = intent.getBooleanExtra(
                        FileDownloader.EXTRA_DOWNLOAD_RESULT, false);
                //boolean isOffscreen =  Math.abs((mViewPager.getCurrentItem() - position))
                // <= mViewPager.getOffscreenPageLimit();
                
                if (position >= 0 &&
                        intent.getAction().equals(FileDownloader.getDownloadFinishMessage())) {
                    if (downloadWasFine) {
                        mPreviewImagePagerAdapter.updateFile(position, file);   
                        
                    } else {
                        mPreviewImagePagerAdapter.updateWithDownloadError(position);
                    }
                    mPreviewImagePagerAdapter.notifyDataSetChanged();   // will trigger the creation
                                                                        // of new fragments
                    
                } else {
                    Log_OC.d(TAG, "Download finished, but the fragment is offscreen");
                }
                
            }
            removeStickyBroadcast(intent);
        }

    }

    @SuppressLint("InlinedApi")
	public void toggleFullScreen() {

        if (isHoneycombOrHigher()) {
        
            boolean visible = (mFullScreenAnchorView.getSystemUiVisibility()
                    & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;

            if (visible) {
                hideSystemUI(mFullScreenAnchorView);
                // actionBar.hide(); // propagated through
                // OnSystemUiVisibilityChangeListener()
            } else {
                showSystemUI(mFullScreenAnchorView);
                // actionBar.show(); // propagated through
                // OnSystemUiVisibilityChangeListener()
            }

        } else {

            ActionBar actionBar = getSupportActionBar();
            if (!actionBar.isShowing()) {
                actionBar.show();
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);

            } else {
                actionBar.hide();
                mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

            }

        }
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

    @Override
    public void onBrowsedDownTo(OCFile folder) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onTransferStateChanged(OCFile file, boolean downloading, boolean uploading) {
        // TODO Auto-generated method stub
        
    }
    
    
    @SuppressLint("InlinedApi")
	private void hideSystemUI(View anchorView) {
        anchorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION         // hides NAVIGATION BAR; Android >= 4.0
            |   View.SYSTEM_UI_FLAG_FULLSCREEN              // hides STATUS BAR;     Android >= 4.1
            |   View.SYSTEM_UI_FLAG_IMMERSIVE               // stays interactive;    Android >= 4.4
            |   View.SYSTEM_UI_FLAG_LAYOUT_STABLE           // draw full window;     Android >= 4.1
            |   View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN       // draw full window;     Android >= 4.1
            |   View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  // draw full window;     Android >= 4.1
        );
    }
    
    @SuppressLint("InlinedApi")
    private void showSystemUI(View anchorView) {
        anchorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE           // draw full window;     Android >= 4.1
            |   View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN       // draw full window;     Android >= 4.1
            |   View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION  // draw full window;     Android >= 4.1
        );
    }

    /**
     * Checks if OS version is Honeycomb one or higher
     *
     * @return boolean
     */
    private boolean isHoneycombOrHigher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return true;
        }
        return false;
    }

    @Override
    public void allFilesOption(){
        backToDisplayActivity();
        super.allFilesOption();
    }
}
