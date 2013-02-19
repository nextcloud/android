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
package com.owncloud.android.ui.fragment;

import java.io.File;
import java.lang.ref.WeakReference;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.FileEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.AccountUtils;
import com.owncloud.android.DisplayUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileObserverService;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.media.MediaService;
import com.owncloud.android.media.MediaServiceBinder;
import com.owncloud.android.network.OwnCloudClientUtils;
import com.owncloud.android.operations.OnRemoteOperationListener;
import com.owncloud.android.operations.RemoteOperation;
import com.owncloud.android.operations.RemoteOperationResult;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.operations.RenameFileOperation;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.OnSwipeTouchListener;
import com.owncloud.android.ui.activity.TransferServiceGetter;
import com.owncloud.android.ui.activity.VideoActivity;
import com.owncloud.android.ui.dialog.EditNameDialog;
import com.owncloud.android.ui.dialog.EditNameDialog.EditNameDialogListener;
import com.owncloud.android.utils.OwnCloudVersion;

import com.owncloud.android.R;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * This fragment shows a preview of a downloaded file.
 * 
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will produce an {@link IllegalStateException}.
 * 
 * By now, if the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on instantiation too.
 * 
 * @author David A. Velasco
 */
public class FilePreviewFragment extends SherlockFragment implements
        /*OnClickListener,*/ OnTouchListener , FileFragment,  
        ConfirmationDialogFragment.ConfirmationDialogFragmentListener, OnRemoteOperationListener /*, EditNameDialogListener*/ {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String EXTRA_PLAY_POSITION = "PLAY_POSITION";

    private View mView;
    private OCFile mFile;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    private ImageView mImagePreview;
    public Bitmap mBitmap = null;
    private VideoView mVideoPreview;
    private int mSavedPlaybackPosition;
    
    //private DownloadFinishReceiver mDownloadFinishReceiver;
    //private UploadFinishReceiver mUploadFinishReceiver;
    
    private Handler mHandler;
    private RemoteOperation mLastRemoteOperation;
    
    private MediaServiceBinder mMediaServiceBinder = null;
    private MediaController mMediaController = null;
    private MediaServiceConnection mMediaServiceConnection = null;
    private VideoHelper mVideoHelper;
    
    private static final String TAG = FilePreviewFragment.class.getSimpleName();

    
    /**
     * Creates a fragment to preview a file.
     * 
     * When 'fileToDetail' or 'ocAccount' are null
     * 
     * @param fileToDetail      An {@link OCFile} to preview in the fragment
     * @param ocAccount         An ownCloud account; needed to start downloads
     */
    public FilePreviewFragment(OCFile fileToDetail, Account ocAccount) {
        mFile = fileToDetail;
        mAccount = ocAccount;
        mSavedPlaybackPosition = 0;
        mStorageManager = null; // we need a context to init this; the container activity is not available yet at this moment 
    }
    
    
    /**
     *  Creates an empty fragment for previews.
     * 
     *  MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically (for instance, when the device is turned a aside).
     * 
     *  DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful construction 
     */
    public FilePreviewFragment() {
        mFile = null;
        mAccount = null;
        mSavedPlaybackPosition = 0;
        mStorageManager = null;
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
        
        //mView.findViewById(R.id.fdKeepInSync).setOnClickListener(this);
        //mView.findViewById(R.id.fdRenameBtn).setOnClickListener(this);
        //mView.findViewById(R.id.fdDownloadBtn).setOnClickListener(this);
        //mView.findViewById(R.id.fdOpenBtn).setOnClickListener(this);
        //mView.findViewById(R.id.fdRemoveBtn).setOnClickListener(this);
        mImagePreview = (ImageView)mView.findViewById(R.id.image_preview);
        mImagePreview.setOnTouchListener(this);
        mVideoPreview = (VideoView)mView.findViewById(R.id.video_preview);
        
        //updateFileDetails(false);
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
            mFile = savedInstanceState.getParcelable(FilePreviewFragment.EXTRA_FILE);
            mAccount = savedInstanceState.getParcelable(FilePreviewFragment.EXTRA_ACCOUNT);
            mSavedPlaybackPosition = savedInstanceState.getInt(FilePreviewFragment.EXTRA_PLAY_POSITION);
            
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
        
        outState.putParcelable(FilePreviewFragment.EXTRA_FILE, mFile);
        outState.putParcelable(FilePreviewFragment.EXTRA_ACCOUNT, mAccount);
        
        if (mVideoPreview.isPlaying()) {
            outState.putInt(FilePreviewFragment.EXTRA_PLAY_POSITION , mVideoPreview.getCurrentPosition());
        }
    }
    

    @Override
    public void onStart() {
        super.onStart();

        if (mFile != null) {
           if (mFile.isAudio()) {
               bindMediaService();
               
           } else if (mFile.isImage()) {
               BitmapLoader bl = new BitmapLoader(mImagePreview);
               bl.execute(new String[]{mFile.getStoragePath()});
               
           } else if (mFile.isVideo()) {
               playVideo(); 
           }
        }
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
            
            /*
            case R.id.action_toggle_keep_in_sync: {
                CheckBox cb = (CheckBox) getView().findViewById(R.id.fdKeepInSync);
                mFile.setKeepInSync(cb.isChecked());
                mStorageManager.saveFile(mFile);
                
                /// register the OCFile instance in the observer service to monitor local updates;
                /// if necessary, the file is download 
                Intent intent = new Intent(getActivity().getApplicationContext(),
                                           FileObserverService.class);
                intent.putExtra(FileObserverService.KEY_FILE_CMD,
                           (cb.isChecked()?
                                   FileObserverService.CMD_ADD_OBSERVED_FILE:
                                   FileObserverService.CMD_DEL_OBSERVED_FILE));
                intent.putExtra(FileObserverService.KEY_CMD_ARG_FILE, mFile);
                intent.putExtra(FileObserverService.KEY_CMD_ARG_ACCOUNT, mAccount);
                Log.e(TAG, "starting observer service");
                getActivity().startService(intent);
                
                if (mFile.keepInSync()) {
                    onClick(getView().findViewById(R.id.fdDownloadBtn));    // force an immediate synchronization
                }
                break;
            }*/
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
        // load the video file in the video player ; when done, VideoHelper#onPrepared() will be called
        mVideoPreview.setVideoPath(mFile.getStoragePath()); 

        // create and prepare control panel for the user
        mMediaController = new MediaController(getActivity());
        mMediaController.setMediaPlayer(mVideoPreview);
        mMediaController.setAnchorView(mVideoPreview);
        mVideoPreview.setMediaController(mMediaController);
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
            mVideoPreview.seekTo(mSavedPlaybackPosition);
            mVideoPreview.start();
            mMediaController.show(MediaService.MEDIA_CONTROL_SHORT_LIFE);  
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
            // nothing, right now
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
            Log.e(TAG, "Error in video playback, what = " + what + ", extra = " + extra);
            
            if (mMediaController != null) {
                mMediaController.hide();
            }
            
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
    public void onResume() {
        super.onResume();
        /*
        mDownloadFinishReceiver = new DownloadFinishReceiver();
        IntentFilter filter = new IntentFilter(
                FileDownloader.DOWNLOAD_FINISH_MESSAGE);
        getActivity().registerReceiver(mDownloadFinishReceiver, filter);
        
        mUploadFinishReceiver = new UploadFinishReceiver();
        filter = new IntentFilter(FileUploader.UPLOAD_FINISH_MESSAGE);
        getActivity().registerReceiver(mUploadFinishReceiver, filter);
        */

    }


    @Override
    public void onPause() {
        super.onPause();
        /*
        if (mVideoPreview.getVisibility() == View.VISIBLE) {
            mSavedPlaybackPosition = mVideoPreview.getCurrentPosition();
        }*/
        /*
        getActivity().unregisterReceiver(mDownloadFinishReceiver);
        mDownloadFinishReceiver = null;
        
        getActivity().unregisterReceiver(mUploadFinishReceiver);
        mUploadFinishReceiver = null;
        */
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
            if (mMediaController != null) {
                mMediaController.hide();
                mMediaController = null;
            }
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBitmap != null) {
            mBitmap.recycle();
        }
    }
    
    /*
    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
    }
    */

    /*
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fdDownloadBtn: {
                FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
                FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
                if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, mFile)) {
                    downloaderBinder.cancel(mAccount, mFile);
                    if (mFile.isDown()) {
                        setButtonsForDown();
                    } else {
                        setButtonsForRemote();
                    }

                } else if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, mFile)) {
                    uploaderBinder.cancel(mAccount, mFile);
                    if (!mFile.fileExists()) {
                        // TODO make something better
                        if (getActivity() instanceof FileDisplayActivity) {
                            // double pane
                            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                            transaction.replace(R.id.file_details_container, new FilePreviewFragment(null, null), FTAG); // empty FileDetailFragment
                            transaction.commit();
                            mContainerActivity.onFileStateChanged();
                        } else {
                            getActivity().finish();
                        }
                        
                    } else if (mFile.isDown()) {
                        setButtonsForDown();
                    } else {
                        setButtonsForRemote();
                    }
                    
                } else {
                    mLastRemoteOperation = new SynchronizeFileOperation(mFile, null, mStorageManager, mAccount, true, false, getActivity());
                    WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getSherlockActivity().getApplicationContext());
                    mLastRemoteOperation.execute(wc, this, mHandler);
                
                    // update ui 
                    boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
                    getActivity().showDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
                    setButtonsForTransferring(); // disable button immediately, although the synchronization does not result in a file transference
                    
                }
                break;
            }
            case R.id.fdKeepInSync: {
                CheckBox cb = (CheckBox) getView().findViewById(R.id.fdKeepInSync);
                mFile.setKeepInSync(cb.isChecked());
                mStorageManager.saveFile(mFile);
                
                /// register the OCFile instance in the observer service to monitor local updates;
                /// if necessary, the file is download 
                Intent intent = new Intent(getActivity().getApplicationContext(),
                                           FileObserverService.class);
                intent.putExtra(FileObserverService.KEY_FILE_CMD,
                           (cb.isChecked()?
                                   FileObserverService.CMD_ADD_OBSERVED_FILE:
                                   FileObserverService.CMD_DEL_OBSERVED_FILE));
                intent.putExtra(FileObserverService.KEY_CMD_ARG_FILE, mFile);
                intent.putExtra(FileObserverService.KEY_CMD_ARG_ACCOUNT, mAccount);
                Log.e(TAG, "starting observer service");
                getActivity().startService(intent);
                
                if (mFile.keepInSync()) {
                    onClick(getView().findViewById(R.id.fdDownloadBtn));    // force an immediate synchronization
                }
                break;
            }
            case R.id.fdRenameBtn: {
                EditNameDialog dialog = EditNameDialog.newInstance(getString(R.string.rename_dialog_title), mFile.getFileName(), this);
                dialog.show(getFragmentManager(), "nameeditdialog");
                break;
            }   
            case R.id.fdRemoveBtn: {
                ConfirmationDialogFragment confDialog = ConfirmationDialogFragment.newInstance(
                        R.string.confirmation_remove_alert,
                        new String[]{mFile.getFileName()},
                        mFile.isDown() ? R.string.confirmation_remove_remote_and_local : R.string.confirmation_remove_remote,
                        mFile.isDown() ? R.string.confirmation_remove_local : -1,
                        R.string.common_cancel);
                confDialog.setOnConfirmationListener(this);
                mCurrentDialog = confDialog;
                mCurrentDialog.show(getFragmentManager(), FTAG_CONFIRMATION);
                break;
            }
            case R.id.fdOpenBtn: {
                openFile();
                break;
            }
            default:
                Log.e(TAG, "Incorrect view clicked!");
        }
        
    }
    */
    
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) { 
            if (v == mImagePreview &&
                    mMediaServiceBinder != null && mFile.isAudio() && mMediaServiceBinder.isPlaying(mFile)) {
                toggleMediaController(MediaService.MEDIA_CONTROL_PERMANENT);
                return true;
                
            } else if (v == mVideoPreview) {
                toggleMediaController(MediaService.MEDIA_CONTROL_SHORT_LIFE);
                return true;        
            }
        }
        return false;
    }

    
    private void toggleMediaController(int time) {
        if (mMediaController.isShowing()) {
            mMediaController.hide();
        } else {
            mMediaController.show(time);
        }
    }


    private void playAudio() {
        if (!mMediaServiceBinder.isPlaying(mFile)) {
            Log.d(TAG, "starting playback of " + mFile.getStoragePath());
            mMediaServiceBinder.start(mAccount, mFile);
            
        } else {
            if (!mMediaServiceBinder.isPlaying()) {
                mMediaServiceBinder.start();
            }
            if (!mMediaController.isShowing() && isVisible()) {
                mMediaController.show(MediaService.MEDIA_CONTROL_PERMANENT);
                // TODO - fix strange bug; steps to trigger :
                // 1. remove the "isVisible()" control
                // 2. start the app and preview an audio file
                // 3. exit from the app (home button, for instance) while the audio file is still being played 
                // 4. go to notification bar and click on the "ownCloud music app" notification
                // PUM!
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
                    if (mMediaController == null) {
                        mMediaController = new MediaController(getSherlockActivity());
                    }
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
            mMediaController.setMediaPlayer(mMediaServiceBinder);
            mMediaController.setAnchorView(getView());
            mMediaController.setEnabled(mMediaServiceBinder.isInPlaybackState());
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(getActivity(), MediaService.class))) {
                Log.e(TAG, "Media service suddenly disconnected");
                if (mMediaController != null) {
                    mMediaController.hide();
                    mMediaController.setMediaPlayer(null);
                    mMediaController = null;
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
     * Interface to implement by any Activity that includes some instance of FileDetailFragment
     * 
     * @author David A. Velasco
     */
    public interface ContainerActivity extends TransferServiceGetter {

        /**
         * Callback method invoked when the detail fragment wants to notice its container 
         * activity about a relevant state the file shown by the fragment.
         * 
         * Added to notify to FileDisplayActivity about the need of refresh the files list. 
         * 
         * Currently called when:
         *  - a download is started;
         *  - a rename is completed;
         *  - a deletion is completed;
         *  - the 'inSync' flag is changed;
         */
        public void onFileStateChanged();
        
    }

    /*
    public void onDismiss(EditNameDialog dialog) {
        if (dialog.getResult()) {
            String newFilename = dialog.getNewFilename();
            Log.d(TAG, "name edit dialog dismissed with new name " + newFilename);
            mLastRemoteOperation = new RenameFileOperation( mFile, 
                                                            mAccount, 
                                                            newFilename, 
                                                            new FileDataStorageManager(mAccount, getActivity().getContentResolver()));
            WebdavClient wc = OwnCloudClientUtils.createOwnCloudClient(mAccount, getSherlockActivity().getApplicationContext());
            mLastRemoteOperation.execute(wc, this, mHandler);
            boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
            getActivity().showDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
        }
    }
    */
    
    private class BitmapLoader extends AsyncTask<String, Void, Bitmap> {

        /**
         * Weak reference to the target {@link ImageView} where the bitmap will be loaded into.
         * 
         * Using a weak reference will avoid memory leaks if the target ImageView is retired from memory before the load finishes.
         */
        private final WeakReference<ImageView> mImageViewRef;
        
        
        /**
         * Constructor.
         * 
         * @param imageView     Target {@link ImageView} where the bitmap will be loaded into.
         */
        public BitmapLoader(ImageView imageView) {
            mImageViewRef = new WeakReference<ImageView>(imageView);
        }
        
        
        @SuppressWarnings("deprecation")
        @SuppressLint({ "NewApi", "NewApi", "NewApi" }) // to avoid Lint errors since Android SDK r20
		@Override
        protected Bitmap doInBackground(String... params) {
            Bitmap result = null;
            if (params.length != 1) return result;
            String storagePath = params[0];
            try {
                // set desired options that will affect the size of the bitmap
                BitmapFactory.Options options = new Options();
                options.inScaled = true;
                options.inPurgeable = true;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD_MR1) {
                    options.inPreferQualityOverSpeed = false;
                }
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                    options.inMutable = false;
                }
                // make a false load of the bitmap - just to be able to read outWidth, outHeight and outMimeType
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(storagePath, options);   
                
                int width = options.outWidth;
                int height = options.outHeight;
                int scale = 1;
                if (width >= 2048 || height >= 2048) {  
                    // try to scale down the image to save memory  
                    scale = (int) Math.ceil((Math.ceil(Math.max(height, width) / 2048.)));
                    options.inSampleSize = scale;
                }
                Display display = getActivity().getWindowManager().getDefaultDisplay();
                Point size = new Point();
                int screenwidth;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR2) {
                    display.getSize(size);
                    screenwidth = size.x;
                } else {
                    screenwidth = display.getWidth();
                }

                Log.d(TAG, "image width: " + width + ", screen width: " + screenwidth);

                if (width > screenwidth) {
                    // second try to scale down the image , this time depending upon the screen size; WTF... 
                    scale = (int) Math.ceil((float)width / screenwidth);
                    options.inSampleSize = scale;
                }

                // really load the bitmap
                options.inJustDecodeBounds = false; // the next decodeFile call will be real
                result = BitmapFactory.decodeFile(storagePath, options);
                Log.e(TAG, "loaded width: " + options.outWidth + ", loaded height: " + options.outHeight);

            } catch (OutOfMemoryError e) {
                result = null;
                Log.e(TAG, "Out of memory occured for file with size " + storagePath);
                
            } catch (NoSuchFieldError e) {
                result = null;
                Log.e(TAG, "Error from access to unexisting field despite protection " + storagePath);
                
            } catch (Throwable t) {
                result = null;
                Log.e(TAG, "Unexpected error while creating image preview " + storagePath, t);
            }
            return result;
        }
        
        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && mImageViewRef != null) {
                final ImageView imageView = mImageViewRef.get();
                imageView.setImageBitmap(result);
                mBitmap  = result;
            }
        }
        
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link FilePreviewFragment} to be previewed.
     * 
     * @param file      File to test if can be previewed.
     * @return          'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && (file.isAudio() || file.isVideo() || file.isImage()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation.equals(mLastRemoteOperation)) {
            if (operation instanceof RemoveFileOperation) {
                onRemoveFileOperationFinish((RemoveFileOperation)operation, result);

                /*
            } else if (operation instanceof RenameFileOperation) {
                onRenameFileOperationFinish((RenameFileOperation)operation, result);
                
            } else if (operation instanceof SynchronizeFileOperation) {
                onSynchronizeFileOperationFinish((SynchronizeFileOperation)operation, result);*/
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
        if (mMediaController != null) {
            mMediaController.hide();
        }
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
    
    /*
    private void onRenameFileOperationFinish(RenameFileOperation operation, RemoteOperationResult result) {
        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);
        
        if (result.isSuccess()) {
            updateFileDetails(((RenameFileOperation)operation).getFile(), mAccount);
            mContainerActivity.onFileStateChanged();
            
        } else {
            if (result.getCode().equals(ResultCode.INVALID_LOCAL_FILE_NAME)) {
                Toast msg = Toast.makeText(getActivity(), R.string.rename_local_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
                // TODO throw again the new rename dialog
            } else {
                Toast msg = Toast.makeText(getActivity(), R.string.rename_server_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
                if (result.isSslRecoverableException()) {
                    // TODO show the SSL warning dialog
                }
            }
        }
    }
    
    private void onSynchronizeFileOperationFinish(SynchronizeFileOperation operation, RemoteOperationResult result) {
        boolean inDisplayActivity = getActivity() instanceof FileDisplayActivity;
        getActivity().dismissDialog((inDisplayActivity)? FileDisplayActivity.DIALOG_SHORT_WAIT : FileDetailActivity.DIALOG_SHORT_WAIT);

        if (!result.isSuccess()) {
            if (result.getCode() == ResultCode.SYNC_CONFLICT) {
                Intent i = new Intent(getActivity(), ConflictsResolveActivity.class);
                i.putExtra(ConflictsResolveActivity.EXTRA_FILE, mFile);
                i.putExtra(ConflictsResolveActivity.EXTRA_ACCOUNT, mAccount);
                startActivity(i);
                
            } else {
                Toast msg = Toast.makeText(getActivity(), R.string.sync_file_fail_msg, Toast.LENGTH_LONG); 
                msg.show();
            }
            
            if (mFile.isDown()) {
                setButtonsForDown();
                
            } else {
                setButtonsForRemote();
            }
            
        } else {
            if (operation.transferWasRequested()) {
                mContainerActivity.onFileStateChanged();    // this is not working; FileDownloader won't do NOTHING at all until this method finishes, so 
                                                            // checking the service to see if the file is downloading results in FALSE
            } else {
                Toast msg = Toast.makeText(getActivity(), R.string.sync_file_nothing_to_do_msg, Toast.LENGTH_LONG); 
                msg.show();
                if (mFile.isDown()) {
                    setButtonsForDown();
                    
                } else {
                    setButtonsForRemote();
                }
            }
        }
    }
    */


}
