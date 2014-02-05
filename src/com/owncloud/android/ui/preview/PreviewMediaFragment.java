/* ownCloud Android client application
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
package com.owncloud.android.ui.preview;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.media.MediaControlView;
import com.owncloud.android.media.MediaService;
import com.owncloud.android.media.MediaServiceBinder;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.OnRemoteOperationListener;
import com.owncloud.android.lib.operations.common.RemoteOperation;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.fragment.ConfirmationDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.Log_OC;


/**
 * This fragment shows a preview of a downloaded media file (audio or video).
 * 
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will produce an {@link IllegalStateException}.
 * 
 * By now, if the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on instantiation too.
 * 
 * @author David A. Velasco
 */
public class PreviewMediaFragment extends FileFragment implements
        OnTouchListener,  
        ConfirmationDialogFragment.ConfirmationDialogFragmentListener, OnRemoteOperationListener  {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String EXTRA_PLAY_POSITION = "PLAY_POSITION";
    private static final String EXTRA_PLAYING = "PLAYING";

    private View mView;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    private ImageView mImagePreview;
    private VideoView mVideoPreview;
    private int mSavedPlaybackPosition;
    
    private Handler mHandler;
    private RemoteOperation mLastRemoteOperation;
    
    private MediaServiceBinder mMediaServiceBinder = null;
    private MediaControlView mMediaController = null;
    private MediaServiceConnection mMediaServiceConnection = null;
    private VideoHelper mVideoHelper;
    private boolean mAutoplay;
    public boolean mPrepared;
    
    private static final String TAG = PreviewMediaFragment.class.getSimpleName();

    
    /**
     * Creates a fragment to preview a file.
     * 
     * When 'fileToDetail' or 'ocAccount' are null
     * 
     * @param fileToDetail      An {@link OCFile} to preview in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     */
    public PreviewMediaFragment(OCFile fileToDetail, Account ocAccount, int startPlaybackPosition, boolean autoplay) {
        super(fileToDetail);
        mAccount = ocAccount;
        mSavedPlaybackPosition = startPlaybackPosition;
        mStorageManager = null; // we need a context to init this; the container activity is not available yet at this moment 
        mAutoplay = autoplay;
    }
    
    
    /**
     *  Creates an empty fragment for previews.
     * 
     *  MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically (for instance, when the device is turned a aside).
     * 
     *  DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful construction 
     */
    public PreviewMediaFragment() {
        super();
        mAccount = null;
        mSavedPlaybackPosition = 0;
        mStorageManager = null;
        mAutoplay = true;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        setHasOptionsMenu(true);
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.e(TAG, "onCreateView");

        
        mView = inflater.inflate(R.layout.file_preview, container, false);
        
        mImagePreview = (ImageView)mView.findViewById(R.id.image_preview);
        mVideoPreview = (VideoView)mView.findViewById(R.id.video_preview);
        mVideoPreview.setOnTouchListener(this);
        
        mMediaController = (MediaControlView)mView.findViewById(R.id.media_controller);
        
        return mView;
    }
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Log_OC.e(TAG, "onAttach");
        
        if (!(activity instanceof FileFragment.ContainerActivity))
            throw new ClassCastException(activity.toString() + " must implement " + FileFragment.ContainerActivity.class.getSimpleName());
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.e(TAG, "onActivityCreated");

        mStorageManager = new FileDataStorageManager(mAccount, getActivity().getApplicationContext().getContentResolver());
        if (savedInstanceState != null) {
            setFile((OCFile)savedInstanceState.getParcelable(PreviewMediaFragment.EXTRA_FILE));
            mAccount = savedInstanceState.getParcelable(PreviewMediaFragment.EXTRA_ACCOUNT);
            mSavedPlaybackPosition = savedInstanceState.getInt(PreviewMediaFragment.EXTRA_PLAY_POSITION);
            mAutoplay = savedInstanceState.getBoolean(PreviewMediaFragment.EXTRA_PLAYING);
            
        }
        OCFile file = getFile();
        if (file == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        }
        if (mAccount == null) {
            throw new IllegalStateException("Instanced with a NULL ownCloud Account");
        }
        if (!file.isDown()) {
            throw new IllegalStateException("There is no local file to preview");
        }
        if (file.isVideo()) {
            mVideoPreview.setVisibility(View.VISIBLE);
            mImagePreview.setVisibility(View.GONE);
            prepareVideo();
            
        } else {
            mVideoPreview.setVisibility(View.GONE);
            mImagePreview.setVisibility(View.VISIBLE);
        }
        
    }
        

    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log_OC.e(TAG, "onSaveInstanceState");
        
        outState.putParcelable(PreviewMediaFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewMediaFragment.EXTRA_ACCOUNT, mAccount);
        
        if (getFile().isVideo()) {
            mSavedPlaybackPosition = mVideoPreview.getCurrentPosition();
            mAutoplay = mVideoPreview.isPlaying();
            outState.putInt(PreviewMediaFragment.EXTRA_PLAY_POSITION , mSavedPlaybackPosition);
            outState.putBoolean(PreviewMediaFragment.EXTRA_PLAYING , mAutoplay);
        } else {
            outState.putInt(PreviewMediaFragment.EXTRA_PLAY_POSITION , mMediaServiceBinder.getCurrentPosition());
            outState.putBoolean(PreviewMediaFragment.EXTRA_PLAYING , mMediaServiceBinder.isPlaying());
        }
    }
    

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.e(TAG, "onStart");

        OCFile file = getFile();
        if (file != null) {
           if (file.isAudio()) {
               bindMediaService();
               
           } else if (file.isVideo()) {
               stopAudio();
               playVideo(); 
           }
        }
    }
    
    
    private void stopAudio() {
        Intent i = new Intent(getSherlockActivity(), MediaService.class);
        i.setAction(MediaService.ACTION_STOP_ALL);
        getSherlockActivity().startService(i);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.file_actions_menu, menu);
        List<Integer> toHide = new ArrayList<Integer>();    
        
        MenuItem item = null;
        toHide.add(R.id.action_cancel_download);
        toHide.add(R.id.action_cancel_upload);
        toHide.add(R.id.action_download_file);
        toHide.add(R.id.action_sync_file);
        toHide.add(R.id.action_rename_file);    // by now

        for (int i : toHide) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
        
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                shareFileWithLink();
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                removeFile();
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            
            default:
                return false;
        }
    }

    
    private void shareFileWithLink() {
        stopPreview(false);
        FileActivity activity = (FileActivity)((FileFragment.ContainerActivity)getActivity());
        activity.getFileOperationsHelper().shareFileWithLink(getFile(), activity);
        
    }


    private void seeDetails() {
        stopPreview(false);
        ((FileFragment.ContainerActivity)getActivity()).showDetails(getFile());        
    }


    private void prepareVideo() {
        // create helper to get more control on the playback
        mVideoHelper = new VideoHelper();
        mVideoPreview.setOnPreparedListener(mVideoHelper);
        mVideoPreview.setOnCompletionListener(mVideoHelper);
        mVideoPreview.setOnErrorListener(mVideoHelper);
    }
    
    private void playVideo() {
        // create and prepare control panel for the user
        mMediaController.setMediaPlayer(mVideoPreview);
        
        // load the video file in the video player ; when done, VideoHelper#onPrepared() will be called
        mVideoPreview.setVideoPath(getFile().getStoragePath()); 
    }
    

    private class VideoHelper implements OnCompletionListener, OnPreparedListener, OnErrorListener {
        
        /** 
         * Called when the file is ready to be played.
         * 
         * Just starts the playback.
         * 
         * @param   mp    {@link MediaPlayer} instance performing the playback.
         */
        @Override
        public void onPrepared(MediaPlayer vp) {
            Log_OC.e(TAG, "onPrepared");
            mVideoPreview.seekTo(mSavedPlaybackPosition);
            if (mAutoplay) { 
                mVideoPreview.start();
            }
            mMediaController.setEnabled(true);
            mMediaController.updatePausePlay();
            mPrepared = true;
        }
        
        
        /**
         * Called when the file is finished playing.
         *  
         * Finishes the activity.
         * 
         * @param   mp    {@link MediaPlayer} instance performing the playback.
         */
        @Override
        public void onCompletion(MediaPlayer  mp) {
            Log_OC.e(TAG, "completed");
            if (mp != null) {
                mVideoPreview.seekTo(0);
                // next lines are necessary to work around undesired video loops
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD) {
                    mVideoPreview.pause();   
                    
                } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD_MR1) {
                    // mVideePreview.pause() is not enough
                    
                    mMediaController.setEnabled(false);
                    mVideoPreview.stopPlayback();
                    mAutoplay = false;
                    mSavedPlaybackPosition = 0;
                    mVideoPreview.setVideoPath(getFile().getStoragePath());
                }
            } // else : called from onError()
            mMediaController.updatePausePlay();
        }
        
        
        /**
         * Called when an error in playback occurs.
         * 
         * @param   mp      {@link MediaPlayer} instance performing the playback.
         * @param   what    Type of error
         * @param   extra   Extra code specific to the error
         */
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            if (mVideoPreview.getWindowToken() != null) {
                String message = MediaService.getMessageForMediaError(getActivity(), what, extra);
                new AlertDialog.Builder(getActivity())
                        .setMessage(message)
                        .setPositiveButton(android.R.string.VideoView_error_button,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                        VideoHelper.this.onCompletion(null);
                                    }
                                })
                        .setCancelable(false)
                        .show();
            }
            return true;
        }
        
    }

    
    @Override
    public void onPause() {
        super.onPause();
        Log_OC.e(TAG, "onPause");
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log_OC.e(TAG, "onResume");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log_OC.e(TAG, "onDestroy");
    }
    
    @Override
    public void onStop() {
        Log_OC.e(TAG, "onStop");
        super.onStop();

        mPrepared = false;
        if (mMediaServiceConnection != null) {
            Log_OC.d(TAG, "Unbinding from MediaService ...");
            if (mMediaServiceBinder != null && mMediaController != null) {
                mMediaServiceBinder.unregisterMediaController(mMediaController);
            }
            getActivity().unbindService(mMediaServiceConnection);
            mMediaServiceConnection = null;
            mMediaServiceBinder = null;
        }
    }
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && v == mVideoPreview) {
            startFullScreenVideo();
            return true;        
        }
        return false;
    }

    
    private void startFullScreenVideo() {
        Intent i = new Intent(getActivity(), PreviewVideoActivity.class);
        i.putExtra(FileActivity.EXTRA_ACCOUNT, mAccount);
        i.putExtra(FileActivity.EXTRA_FILE, getFile());
        i.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, mVideoPreview.isPlaying());
        mVideoPreview.pause();
        i.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, mVideoPreview.getCurrentPosition());
        startActivityForResult(i, 0);
    }

    @Override
    public void onConfigurationChanged (Configuration newConfig) {
        Log_OC.e(TAG, "onConfigurationChanged " + this);
    }
    
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        Log_OC.e(TAG, "onActivityResult " + this);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mSavedPlaybackPosition = data.getExtras().getInt(PreviewVideoActivity.EXTRA_START_POSITION);
            mAutoplay = data.getExtras().getBoolean(PreviewVideoActivity.EXTRA_AUTOPLAY); 
        }
    }
    

    private void playAudio() {
        OCFile file = getFile();
        if (!mMediaServiceBinder.isPlaying(file)) {
            Log_OC.d(TAG, "starting playback of " + file.getStoragePath());
            mMediaServiceBinder.start(mAccount, file, mAutoplay, mSavedPlaybackPosition);
            
        } else {
            if (!mMediaServiceBinder.isPlaying() && mAutoplay) {
                mMediaServiceBinder.start();
                mMediaController.updatePausePlay();
            }
        }
    }


    private void bindMediaService() {
        Log_OC.d(TAG, "Binding to MediaService...");
        if (mMediaServiceConnection == null) {
            mMediaServiceConnection = new MediaServiceConnection();
        }
        getActivity().bindService(  new Intent(getActivity(), 
                                    MediaService.class),
                                    mMediaServiceConnection, 
                                    Context.BIND_AUTO_CREATE);
            // follow the flow in MediaServiceConnection#onServiceConnected(...)
    }
    
    /** Defines callbacks for service binding, passed to bindService() */
    private class MediaServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (component.equals(new ComponentName(getActivity(), MediaService.class))) {
                Log_OC.d(TAG, "Media service connected");
                mMediaServiceBinder = (MediaServiceBinder) service;
                if (mMediaServiceBinder != null) {
                    prepareMediaController();
                    playAudio();    // do not wait for the touch of nobody to play audio
                    
                    Log_OC.d(TAG, "Successfully bound to MediaService, MediaController ready");
                    
                } else {
                    Log_OC.e(TAG, "Unexpected response from MediaService while binding");
                }
            }
        }

        private void prepareMediaController() {
            mMediaServiceBinder.registerMediaController(mMediaController);
            if (mMediaController != null) {
                mMediaController.setMediaPlayer(mMediaServiceBinder);
                mMediaController.setEnabled(true);
                mMediaController.updatePausePlay();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(getActivity(), MediaService.class))) {
                Log_OC.e(TAG, "Media service suddenly disconnected");
                if (mMediaController != null) {
                    mMediaController.setMediaPlayer(null);
                } else {
                    Toast.makeText(getActivity(), "No media controller to release when disconnected from media service", Toast.LENGTH_SHORT).show();
                }
                mMediaServiceBinder = null;
                mMediaServiceConnection = null;
            }
        }
    }    

    

    /**
     * Opens the previewed file with an external application.
     * 
     * TODO - improve this; instead of prioritize the actions available for the MIME type in the server, 
     * we should get a list of available apps for MIME tpye in the server and join it with the list of 
     * available apps for the MIME type known from the file extension, to let the user choose
     */
    private void openFile() {
        OCFile file = getFile();
        stopPreview(true);
        String storagePath = file.getStoragePath();
        String encodedStoragePath = WebdavUtils.encodePath(storagePath);
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), file.getMimetype());
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(i);
            
        } catch (Throwable t) {
            Log_OC.e(TAG, "Fail when trying to open with the mimeType provided from the ownCloud server: " + file.getMimetype());
            boolean toastIt = true; 
            String mimeType = "";
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                if (mimeType == null || !mimeType.equals(file.getMimetype())) {
                    if (mimeType != null) {
                        i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), mimeType);
                    } else {
                        // desperate try
                        i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), "*-/*");
                    }
                    i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivity(i);
                    toastIt = false;
                }
                
            } catch (IndexOutOfBoundsException e) {
                Log_OC.e(TAG, "Trying to find out MIME type of a file without extension: " + storagePath);
                
            } catch (ActivityNotFoundException e) {
                Log_OC.e(TAG, "No activity found to handle: " + storagePath + " with MIME type " + mimeType + " obtained from extension");
                
            } catch (Throwable th) {
                Log_OC.e(TAG, "Unexpected problem when opening: " + storagePath, th);
                
            } finally {
                if (toastIt) {
                    Toast.makeText(getActivity(), "There is no application to handle file " + file.getFileName(), Toast.LENGTH_SHORT).show();
                }
            }
            
        }
        finish();
    }
    
    /**
     * Starts a the removal of the previewed file.
     * 
     * Shows a confirmation dialog. The action continues in {@link #onConfirmation(String)} , {@link #onNeutral(String)} or {@link #onCancel(String)},
     * depending upon the user selection in the dialog. 
     */
    private void removeFile() {
        ConfirmationDialogFragment confDialog = ConfirmationDialogFragment.newInstance(
                R.string.confirmation_remove_alert,
                new String[]{getFile().getFileName()},
                R.string.confirmation_remove_remote_and_local,
                R.string.confirmation_remove_local,
                R.string.common_cancel);
        confDialog.setOnConfirmationListener(this);
        confDialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
    }

    
    /**
     * Performs the removal of the previewed file, both locally and in the server.
     */
    @Override
    public void onConfirmation(String callerTag) {
        OCFile file = getFile();
        if (mStorageManager.getFileById(file.getFileId()) != null) {   // check that the file is still there;
            stopPreview(true);
            mLastRemoteOperation = new RemoveFileOperation( file,      // TODO we need to review the interface with RemoteOperations, and use OCFile IDs instead of OCFile objects as parameters
                                                            true, 
                                                            mStorageManager);
            mLastRemoteOperation.execute(mAccount, getSherlockActivity(), this, mHandler, getSherlockActivity());
            
            ((FileDisplayActivity) getActivity()).showLoadingDialog();
        }
    }
    
    
    /**
     * Removes the file from local storage
     */
    @Override
    public void onNeutral(String callerTag) {
        OCFile file = getFile();
        stopPreview(true);
        mStorageManager.removeFile(file, false, true);    // TODO perform in background task / new thread
        finish();
    }
    
    /**
     * User cancelled the removal action.
     */
    @Override
    public void onCancel(String callerTag) {
        // nothing to do here
    }
    

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewMediaFragment} to be previewed.
     * 
     * @param file      File to test if can be previewed.
     * @return          'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && (file.isAudio() || file.isVideo()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation.equals(mLastRemoteOperation)) {
            if (operation instanceof RemoveFileOperation) {
                onRemoveFileOperationFinish((RemoveFileOperation)operation, result);
            }
        }
    }
    
    private void onRemoveFileOperationFinish(RemoveFileOperation operation, RemoteOperationResult result) {
        ((FileDisplayActivity) getActivity()).dismissLoadingDialog();
        if (result.isSuccess()) {
            Toast msg = Toast.makeText(getActivity().getApplicationContext(), R.string.remove_success_msg, Toast.LENGTH_LONG);
            msg.show();
            finish();
                
        } else {
            Toast msg = Toast.makeText(getActivity(), R.string.remove_fail_msg, Toast.LENGTH_LONG); 
            msg.show();
            if (result.isSslRecoverableException()) {
                // TODO show the SSL warning dialog
            }
        }
    }

    private void stopPreview(boolean stopAudio) {
        OCFile file = getFile();
        if (file.isAudio() && stopAudio) {
            mMediaServiceBinder.pause();
            
        } else if (file.isVideo()) {
            mVideoPreview.stopPlayback();
        }
    }



    /**
     * Finishes the preview
     */
    private void finish() {
        getActivity().onBackPressed();
    }


    public int getPosition() {
        if (mPrepared) {
            mSavedPlaybackPosition = mVideoPreview.getCurrentPosition();
        }
        Log_OC.e(TAG, "getting position: " + mSavedPlaybackPosition);
        return mSavedPlaybackPosition;
    }
    
    public boolean isPlaying() {
        if (mPrepared) {
            mAutoplay = mVideoPreview.isPlaying();
        }
        return mAutoplay;
    }
    
}
