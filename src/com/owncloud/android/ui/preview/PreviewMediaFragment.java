/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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

import android.accounts.Account;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.support.v7.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.VideoView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.media.MediaControlView;
import com.owncloud.android.media.MediaService;
import com.owncloud.android.media.MediaServiceBinder;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFileDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;


/**
 * This fragment shows a preview of a downloaded media file (audio or video).
 *
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will
 * produce an {@link IllegalStateException}.
 * 
 * By now, if the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is
 * generated on instantiation too.
 */
public class PreviewMediaFragment extends FileFragment implements
        OnTouchListener {

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_ACCOUNT = "ACCOUNT";
    private static final String EXTRA_PLAY_POSITION = "PLAY_POSITION";
    private static final String EXTRA_PLAYING = "PLAYING";

    private View mView;
    private Account mAccount;
    private ImageView mImagePreview;
    private VideoView mVideoPreview;
    private int mSavedPlaybackPosition;

    private MediaServiceBinder mMediaServiceBinder = null;
    private MediaControlView mMediaController = null;
    private MediaServiceConnection mMediaServiceConnection = null;
    private VideoHelper mVideoHelper;
    private boolean mAutoplay;
    public boolean mPrepared;

    private static final String TAG = PreviewMediaFragment.class.getSimpleName();


    /**
     * Creates a fragment to preview a file.
     * <p/>
     * When 'fileToDetail' or 'ocAccount' are null
     *
     * @param fileToDetail An {@link OCFile} to preview in the fragment
     * @param ocAccount    An ownCloud account; needed to start downloads
     */
    public PreviewMediaFragment(
            OCFile fileToDetail,
            Account ocAccount,
            int startPlaybackPosition,
            boolean autoplay) {

        super(fileToDetail);
        mAccount = ocAccount;
        mSavedPlaybackPosition = startPlaybackPosition;
        mAutoplay = autoplay;
    }


    /**
     * Creates an empty fragment for previews.
     * <p/>
     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically
     * (for instance, when the device is turned a aside).
     * <p/>
     * DO NOT CALL IT: an {@link OCFile} and {@link Account} must be provided for a successful
     * construction
     */
    public PreviewMediaFragment() {
        super();
        mAccount = null;
        mSavedPlaybackPosition = 0;
        mAutoplay = true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.v(TAG, "onCreateView");


        mView = inflater.inflate(R.layout.file_preview, container, false);

        mImagePreview = (ImageView) mView.findViewById(R.id.image_preview);
        mVideoPreview = (VideoView) mView.findViewById(R.id.video_preview);
        mVideoPreview.setOnTouchListener(this);

        mMediaController = (MediaControlView) mView.findViewById(R.id.media_controller);

        return mView;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.v(TAG, "onActivityCreated");

        OCFile file = getFile();
        if (savedInstanceState == null) {
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (mAccount == null) {
                throw new IllegalStateException("Instanced with a NULL ownCloud Account");
            }
            if (!file.isDown()) {
                throw new IllegalStateException("There is no local file to preview");
            }

        }
        else {
            file = (OCFile) savedInstanceState.getParcelable(PreviewMediaFragment.EXTRA_FILE);
            setFile(file);
            mAccount = savedInstanceState.getParcelable(PreviewMediaFragment.EXTRA_ACCOUNT);
            mSavedPlaybackPosition =
                    savedInstanceState.getInt(PreviewMediaFragment.EXTRA_PLAY_POSITION);
            mAutoplay = savedInstanceState.getBoolean(PreviewMediaFragment.EXTRA_PLAYING);

        }
        if (file != null && file.isDown()) {
            if (file.isVideo()) {
                mVideoPreview.setVisibility(View.VISIBLE);
                mImagePreview.setVisibility(View.GONE);
                prepareVideo();

            }
            else {
                mVideoPreview.setVisibility(View.GONE);
                mImagePreview.setVisibility(View.VISIBLE);
                extractAndSetCoverArt(file);
            }
        }

    }

    /**
     * tries to read the cover art from the audio file and sets it as cover art.
     *
     * @param file audio file with potential cover art
     */
    private void extractAndSetCoverArt(OCFile file) {
        if (file.isAudio()) {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(file.getStoragePath());
                byte[] data = mmr.getEmbeddedPicture();
                if (data != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    mImagePreview.setImageBitmap(bitmap); //associated cover art in bitmap
                } else {
                    mImagePreview.setImageResource(R.drawable.logo);
                }
            } catch (Throwable t) {
                mImagePreview.setImageResource(R.drawable.logo);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log_OC.v(TAG, "onSaveInstanceState");

        outState.putParcelable(PreviewMediaFragment.EXTRA_FILE, getFile());
        outState.putParcelable(PreviewMediaFragment.EXTRA_ACCOUNT, mAccount);

        if (getFile().isVideo()) {
            mSavedPlaybackPosition = mVideoPreview.getCurrentPosition();
            mAutoplay = mVideoPreview.isPlaying();
            outState.putInt(PreviewMediaFragment.EXTRA_PLAY_POSITION, mSavedPlaybackPosition);
            outState.putBoolean(PreviewMediaFragment.EXTRA_PLAYING, mAutoplay);
        }
        else {
            outState.putInt(
                    PreviewMediaFragment.EXTRA_PLAY_POSITION,
                    mMediaServiceBinder.getCurrentPosition());
            outState.putBoolean(
                    PreviewMediaFragment.EXTRA_PLAYING, mMediaServiceBinder.isPlaying());
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        Log_OC.v(TAG, "onStart");

        OCFile file = getFile();
        if (file != null && file.isDown()) {
            if (file.isAudio()) {
                bindMediaService();

            }
            else {
                if (file.isVideo()) {
                    stopAudio();
                    playVideo();
                }
            }
        }
    }


    private void stopAudio() {
        Intent i = new Intent(getActivity(), MediaService.class);
        i.setAction(MediaService.ACTION_STOP_ALL);
        getActivity().startService(i);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.file_actions_menu, menu);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mContainerActivity.getStorageManager() != null) {
            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                mContainerActivity.getStorageManager().getAccount(),
                mContainerActivity,
                getActivity()
            );
            mf.filter(menu);
        }

        // additional restriction for this fragment 
        // TODO allow renaming in PreviewImageFragment
        MenuItem item = menu.findItem(R.id.action_rename_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_move);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_copy);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_share_file: {
                seeShareFile();
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFileDialogFragment dialog = RemoveFileDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_send_file: {
                sendFile();
                return true;
            }
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_favorite_file:{
                mContainerActivity.getFileOperationsHelper().toggleFavorite(getFile(), true);
                return true;
            }
            case R.id.action_unfavorite_file:{
                mContainerActivity.getFileOperationsHelper().toggleFavorite(getFile(), false);
                return true;
            }
            default:
                return false;
        }
    }


    /**
     * Update the file of the fragment with file value
     *
     * @param file      Replaces the held file with a new one
     */
    public void updateFile(OCFile file) {
        setFile(file);
    }

    private void sendFile() {
        stopPreview(false);
        mContainerActivity.getFileOperationsHelper().sendDownloadedFile(getFile());

    }

    private void seeDetails() {
        stopPreview(false);
        mContainerActivity.showDetails(getFile());
    }

    private void seeShareFile() {
        stopPreview(false);
        mContainerActivity.getFileOperationsHelper().showShareFile(getFile());
    }

    private void prepareVideo() {
        // create helper to get more control on the playback
        mVideoHelper = new VideoHelper();
        mVideoPreview.setOnPreparedListener(mVideoHelper);
        mVideoPreview.setOnCompletionListener(mVideoHelper);
        mVideoPreview.setOnErrorListener(mVideoHelper);
    }

    @SuppressWarnings("static-access")
    private void playVideo() {
        // create and prepare control panel for the user
        mMediaController.setMediaPlayer(mVideoPreview);

        // load the video file in the video player ; 
        // when done, VideoHelper#onPrepared() will be called
        mVideoPreview.setVideoURI(getFile().getStorageUri());
    }


    private class VideoHelper implements OnCompletionListener, OnPreparedListener, OnErrorListener {

        /**
         * Called when the file is ready to be played.
         * <p/>
         * Just starts the playback.
         *
         * @param   vp    {@link MediaPlayer} instance performing the playback.
         */
        @Override
        public void onPrepared(MediaPlayer vp) {
            Log_OC.v(TAG, "onPrepared");
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
         * <p/>
         * Finishes the activity.
         *
         * @param mp {@link MediaPlayer} instance performing the playback.
         */
        @Override
        public void onCompletion(MediaPlayer mp) {
            Log_OC.v(TAG, "completed");
            if (mp != null) {
                mVideoPreview.seekTo(0);
            } // else : called from onError()
            mMediaController.updatePausePlay();
        }


        /**
         * Called when an error in playback occurs.
         *
         * @param mp    {@link MediaPlayer} instance performing the playback.
         * @param what  Type of error
         * @param extra Extra code specific to the error
         */
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            Log_OC.e(TAG, "Error in video playback, what = " + what + ", extra = " + extra);
            if (mVideoPreview.getWindowToken() != null) {
                String message = MediaService.getMessageForMediaError(
                        getActivity(), what, extra);
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
        Log_OC.v(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log_OC.v(TAG, "onResume");
    }

    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onStop() {
        Log_OC.v(TAG, "onStop");

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

        super.onStop();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && v == mVideoPreview) {
            // added a margin on the left to avoid interfering with gesture to open navigation drawer
            if (event.getX() / Resources.getSystem().getDisplayMetrics().density > 24.0) {
                startFullScreenVideo();
            }
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
    public void onConfigurationChanged(Configuration newConfig) {
        Log_OC.v(TAG, "onConfigurationChanged " + this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log_OC.v(TAG, "onActivityResult " + this);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mSavedPlaybackPosition = data.getExtras().getInt(
                    PreviewVideoActivity.EXTRA_START_POSITION);
            mAutoplay = data.getExtras().getBoolean(PreviewVideoActivity.EXTRA_AUTOPLAY);
        }
    }


    private void playAudio() {
        OCFile file = getFile();
        if (!mMediaServiceBinder.isPlaying(file)) {
            Log_OC.d(TAG, "starting playback of " + file.getStoragePath());
            mMediaServiceBinder.start(mAccount, file, mAutoplay, mSavedPlaybackPosition);

        }
        else {
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
            if (getActivity() != null) {
                if (component.equals(
                        new ComponentName(getActivity(), MediaService.class))) {
                    Log_OC.d(TAG, "Media service connected");
                    mMediaServiceBinder = (MediaServiceBinder) service;
                    if (mMediaServiceBinder != null) {
                        prepareMediaController();
                        playAudio();    // do not wait for the touch of nobody to play audio

                        Log_OC.d(TAG, "Successfully bound to MediaService, MediaController ready");

                    }
                    else {
                        Log_OC.e(TAG, "Unexpected response from MediaService while binding");
                    }
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
                Log_OC.w(TAG, "Media service suddenly disconnected");
                if (mMediaController != null) {
                    mMediaController.setMediaPlayer(null);
                }
                else {
                    Toast.makeText(
                            getActivity(),
                            "No media controller to release when disconnected from media service", 
                            Toast.LENGTH_SHORT).show();
                }
                mMediaServiceBinder = null;
                mMediaServiceConnection = null;
            }
        }
    }


    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        stopPreview(true);
        mContainerActivity.getFileOperationsHelper().openFile(getFile());
        finish();
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewMediaFragment}
     * to be previewed.
     *
     * @param file File to test if can be previewed.
     * @return 'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return (file != null && (file.isAudio() || file.isVideo()));
    }


    public void stopPreview(boolean stopAudio) {
        OCFile file = getFile();
        if (file.isAudio() && stopAudio) {
            mMediaServiceBinder.pause();

        }
        else {
            if (file.isVideo()) {
                mVideoPreview.stopPlayback();
            }
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
        Log_OC.v(TAG, "getting position: " + mSavedPlaybackPosition);
        return mSavedPlaybackPosition;
    }

    public boolean isPlaying() {
        if (mPrepared) {
            mAutoplay = mVideoPreview.isPlaying();
        }
        return mAutoplay;
    }

}
