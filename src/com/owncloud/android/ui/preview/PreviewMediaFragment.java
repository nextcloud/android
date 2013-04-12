/* ownCloud Android client application
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
package com.owncloud.android.ui.preview;

import java.io.File;
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
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.media.MediaControlView;
import com.owncloud.android.media.MediaService;
import com.owncloud.android.media.MediaServiceBinder;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.OnRemoteOperationListener;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.fragment.ConfirmationDialogFragment;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileFragment;

import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * This fragment shows a preview of a downloaded media file (audio or video).
 * 
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will produce an {@link IllegalStateException}.
 * 
 * By now, if the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on instantiation too.
 * 
 * @author David A. Velasco
 */
public class PreviewMediaFragment extends SherlockFragment implements
        OnTouchListener , FileFragment,  
        ConfirmationDialogFragment.ConfirmationDialogFragmentListener, OnRemoteOperationListener  {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String EXTRA_PLAY_POSITION = "PLAY_POSITION";
    private static final String EXTRA_PLAYING = "PLAYING";

    private View mView;
    private OCFile mFile;
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
    
    private static final String TAG = PreviewMediaFragment.class.getSimpleName();

    
    /**
     * Creates a fragment to preview a file.
     * 
     * When 'fileToDetail' or 'ocAccount' are null
     * 
     * @param fileToDetail      An {@link OCFile} to preview in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     */
    public PreviewMediaFragment(OCFile fileToDetail, Account ocAccount) {
        mFile = fileToDetail;
        mAccount = ocAccount;
        mSavedPlaybackPosition = 0;
        mStorageManager = null; // we need a context to init this; the container activity is not available yet at this moment 
        mAutoplay = true;
    }
    
    
    /**
     *  Creates an empty fragment for previews.
     * 
     *  MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically (for instance, when the device is turned a aside).
     * 
     *  DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful construction 
     */
    public PreviewMediaFragment() {
        mFile = null;
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
        if (!(activity instanceof FileFragment.ContainerActivity))
            throw new ClassCastException(activity.toString() + " must implement " + FileFragment.ContainerActivity.class.getSimpleName());
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mStorageManager = new FileDataStorageManager(mAccount, getActivity().getApplicationContext().getContentResolver());
        if (savedInstanceState != null) {
            mFile = savedInstanceState.getParcelable(PreviewMediaFragment.EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(PreviewMediaFragment.EXTRA_ACCOUNT);
            mSavedPlaybackPosition = savedInstanceState.getInt(PreviewMediaFragment.EXTRA_PLAY_POSITION);
            mAutoplay = savedInstanceState.getBoolean(PreviewMediaFragment.EXTRA_PLAYING);
            
        }
        if (mFile == null) {
            throw new IllegalStateException("Instanced with a NULL OCFile");
        }
        if (mAccount == null) {
            throw new IllegalStateException("Instanced with a NULL ownCloud Account");
        }
        if (!mFile.isDown()) {
            throw new IllegalStateException("There is no local file to preview");
        }
        if (mFile.isVideo()) {
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
        outState.putParcelable(PreviewMediaFragment.EXTRA_FILE, mFile);
        outState.putParcelable(PreviewMediaFragment.EXTRA_ACCOUNT, mAccount);
        
        if (mFile.isVideo()) {
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

        if (mFile != null) {
           if (mFile.isAudio()) {
               bindMediaService();
               
           } else if (mFile.isVideo()) {
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

    
    private void seeDetails() {
        stopPreview(false);
        ((FileFragment.ContainerActivity)getActivity()).showFragmentWithDetails(mFile);        
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
        mVideoPreview.setVideoPath(mFile.getStoragePath()); 
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
            Log.e(TAG, "onPrepared");
            mVideoPreview.seekTo(mSavedPlaybackPosition);
            if (mAutoplay) { 
                mVideoPreview.start();
            }
            mMediaController.setEnabled(true);
            mMediaController.updatePausePlay();
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
            Log.e(TAG, "completed");
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
                    mVideoPreview.setVideoPath(mFile.getStoragePath());
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
    public void onStop() {
        super.onStop();
        
        if (mMediaServiceConnection != null) {
            Log.d(TAG, "Unbinding from MediaService ...");
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
        i.putExtra(PreviewVideoActivity.EXTRA_ACCOUNT, mAccount);
        i.putExtra(PreviewVideoActivity.EXTRA_FILE, mFile);
        i.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, mVideoPreview.isPlaying());
        mVideoPreview.pause();
        i.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, mVideoPreview.getCurrentPosition());
        startActivityForResult(i, 0);
    }

    
    @Override
    public void onActivityResult (int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mSavedPlaybackPosition = data.getExtras().getInt(PreviewVideoActivity.EXTRA_START_POSITION);
            mAutoplay = data.getExtras().getBoolean(PreviewVideoActivity.EXTRA_AUTOPLAY); 
        }
    }
    

    private void playAudio() {
        if (!mMediaServiceBinder.isPlaying(mFile)) {
            Log.d(TAG, "starting playback of " + mFile.getStoragePath());
            mMediaServiceBinder.start(mAccount, mFile, mAutoplay, mSavedPlaybackPosition);
            
        } else {
            if (!mMediaServiceBinder.isPlaying() && mAutoplay) {
                mMediaServiceBinder.start();
                mMediaController.updatePausePlay();
            }
        }
    }


    private void bindMediaService() {
        Log.d(TAG, "Binding to MediaService...");
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
                Log.d(TAG, "Media service connected");
                mMediaServiceBinder = (MediaServiceBinder) service;
                if (mMediaServiceBinder != null) {
                    prepareMediaController();
                    playAudio();    // do not wait for the touch of nobody to play audio
                    
                    Log.d(TAG, "Successfully bound to MediaService, MediaController ready");
                    
                } else {
                    Log.e(TAG, "Unexpected response from MediaService while binding");
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
                Log.e(TAG, "Media service suddenly disconnected");
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
        stopPreview(true);
        String storagePath = mFile.getStoragePath();
        String encodedStoragePath = WebdavUtils.encodePath(storagePath);
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Uri.parse("file://"+ encodedStoragePath), mFile.getMimetype());
            i.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivity(i);
            
        } catch (Throwable t) {
            Log.e(TAG, "Fail when trying to open with the mimeType provided from the ownCloud server: " + mFile.getMimetype());
            boolean toastIt = true; 
            String mimeType = "";
            try {
                Intent i = new Intent(Intent.ACTION_VIEW);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                if (mimeType == null || !mimeType.equals(mFile.getMimetype())) {
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
                Log.e(TAG, "Trying to find out MIME type of a file without extension: " + storagePath);
                
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "No activity found to handle: " + storagePath + " with MIME type " + mimeType + " obtained from extension");
                
            } catch (Throwable th) {
                Log.e(TAG, "Unexpected problem when opening: " + storagePath, th);
                
            } finally {
                if (toastIt) {
                    Toast.makeText(getActivity(), "There is no application to handle file " + mFile.getFileName(), Toast.LENGTH_SHORT).show();
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
                new String[]{mFile.getFileName()},
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
        if (mStorageManager.getFileById(mFile.getFileId()) != null) {   // check that the file is still there;
            stopPreview(true);
            mLastRemoteOperation = new RemoveFileOperation( mFile,      // TODO we need to review the interface with RemoteOperations, and use OCFile IDs instead of OCFile objects as parameters
                                                            true, 
                                                            mStorageManager);
            WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getSherlockActivity().getApplicationContext());
            mLastRemoteOperation.execute(wc, this, mHandler);
            
            boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
            getActivity().showDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
        }
    }
    
    
    /**
     * Removes the file from local storage
     */
    @Override
    public void onNeutral(String callerTag) {
        // TODO this code should be made in a secondary thread,
        if (mFile.isDown()) {   // checks it is still there
            stopPreview(true);
            File f = new File(mFile.getStoragePath());
            f.delete();
            mFile.setStoragePath(null);
            mStorageManager.saveFile(mFile);
            finish();
        }
    }
    
    /**
     * User cancelled the removal action.
     */
    @Override
    public void onCancel(String callerTag) {
        // nothing to do here
    }
    

    /**
     * {@inheritDoc}
     */
    public OCFile getFile(){
        return mFile;
    }
    
    /*
    /**
     * Use this method to signal this Activity that it shall update its view.
     * 
     * @param file : An {@link OCFile}
     *-/
    public void updateFileDetails(OCFile file, Account ocAccount) {
        mFile = file;
        if (ocAccount != null && ( 
                mStorageManager == null || 
                (mAccount != null && !mAccount.equals(ocAccount))
           )) {
            mStorageManager = new FileDataStorageManager(ocAccount, getActivity().getApplicationContext().getContentResolver());
        }
        mAccount = ocAccount;
        updateFileDetails(false);
    }
    */
    

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
        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
        
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
        if (mFile.isAudio() && stopAudio) {
            mMediaServiceBinder.pause();
            
        } else if (mFile.isVideo()) {
            mVideoPreview.stopPlayback();
        }
    }



    /**
     * Finishes the preview
     */
    private void finish() {
        Activity container = getActivity();
        if (container instanceof FileDisplayActivity) {
            // double pane
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.file_details_container, new FileDetailFragment(null, null), FileDetailFragment.FTAG); // empty FileDetailFragment
            transaction.commit();
            ((FileFragment.ContainerActivity)container).onFileStateChanged();
        } else {
            container.finish();
        }
    }
    
}
