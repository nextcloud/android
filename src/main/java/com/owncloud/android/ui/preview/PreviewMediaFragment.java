/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author Chris Narkiewicz
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.media.ErrorFormat;
import com.nextcloud.client.media.PlayerServiceConnection;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.files.StreamMediaFileOperation;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.media.MediaControlView;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.MimeTypeUtil;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * This fragment shows a preview of a downloaded media file (audio or video).
 *
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link Account} values will
 * produce an {@link IllegalStateException}.
 *
 * By now, if the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is
 * generated on instantiation too.
 */
public class PreviewMediaFragment extends FileFragment implements OnTouchListener, Injectable {

    private static final String TAG = PreviewMediaFragment.class.getSimpleName();

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_USER = "USER";
    private static final String EXTRA_PLAY_POSITION = "PLAY_POSITION";
    private static final String EXTRA_PLAYING = "PLAYING";
    private static final double MIN_DENSITY_RATIO = 24.0;

    private static final String FILE = "FILE";
    private static final String USER = "USER";
    private static final String PLAYBACK_POSITION = "PLAYBACK_POSITION";
    private static final String AUTOPLAY = "AUTOPLAY";

    private User user;
    private ImageView mImagePreview;
    private VideoView mVideoPreview;
    private int mSavedPlaybackPosition;

    private RelativeLayout mPreviewContainer;

    private LinearLayout mMultiListContainer;
    private TextView mMultiListMessage;
    private TextView mMultiListHeadline;
    private ImageView mMultiListIcon;
    private ProgressBar mMultiListProgress;

    private MediaControlView mMediaController;
    private boolean mAutoplay;
    private boolean mPrepared;
    private PlayerServiceConnection mMediaPlayerServiceConnection;

    private Uri mVideoUri;
    @Inject ClientFactory clientFactory;
    @Inject UserAccountManager accountManager;
    @Inject DeviceInfo deviceInfo;

    /**
     * Creates a fragment to preview a file.
     *
     * When 'fileToDetail' or 'ocAccount' are null
     *
     * @param fileToDetail An {@link OCFile} to preview in the fragment
     * @param user    Currently active user
     */
    public static PreviewMediaFragment newInstance(OCFile fileToDetail, User user, int startPlaybackPosition,
                                                   boolean autoplay) {
        PreviewMediaFragment previewMediaFragment = new PreviewMediaFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelable(FILE, fileToDetail);
        bundle.putParcelable(USER, user);
        bundle.putInt(PLAYBACK_POSITION, startPlaybackPosition);
        bundle.putBoolean(AUTOPLAY, autoplay);

        previewMediaFragment.setArguments(bundle);

        return previewMediaFragment;
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
        mSavedPlaybackPosition = 0;
        mAutoplay = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();

        setFile(bundle.getParcelable(FILE));
        user = bundle.getParcelable(USER);
        mSavedPlaybackPosition = bundle.getInt(PLAYBACK_POSITION);
        mAutoplay = bundle.getBoolean(AUTOPLAY);
        mMediaPlayerServiceConnection = new PlayerServiceConnection(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.v(TAG, "onCreateView");


        View view = inflater.inflate(R.layout.file_preview, container, false);

        mPreviewContainer = view.findViewById(R.id.file_preview_container);
        mImagePreview = view.findViewById(R.id.image_preview);
        mVideoPreview = view.findViewById(R.id.video_preview);
        mVideoPreview.setOnTouchListener(this);

        mMediaController = view.findViewById(R.id.media_controller);

        setupMultiView(view);
        setMultiListLoadingMessage();
        return view;
    }

    private void setupMultiView(View view) {
        mMultiListContainer = view.findViewById(R.id.empty_list_view);
        mMultiListMessage = view.findViewById(R.id.empty_list_view_text);
        mMultiListHeadline = view.findViewById(R.id.empty_list_view_headline);
        mMultiListIcon = view.findViewById(R.id.empty_list_icon);
        mMultiListProgress = view.findViewById(R.id.empty_list_progress);
    }

    private void setMultiListLoadingMessage() {
        if (mMultiListContainer != null) {
            mMultiListHeadline.setText(R.string.file_list_loading);
            mMultiListMessage.setText("");

            mMultiListIcon.setVisibility(View.GONE);
            mMultiListProgress.setVisibility(View.VISIBLE);
        }
    }

    private void setMessageForMultiList(String headline, @StringRes int message, @DrawableRes int icon) {
        if (mMultiListContainer != null && mMultiListMessage != null) {
            mMultiListHeadline.setText(headline);
            mMultiListMessage.setText(message);
            mMultiListIcon.setImageResource(icon);

            mMultiListMessage.setVisibility(View.VISIBLE);
            mMultiListIcon.setVisibility(View.VISIBLE);
            mMultiListProgress.setVisibility(View.GONE);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log_OC.v(TAG, "onActivityCreated");

        OCFile file = getFile();
        if (savedInstanceState == null) {
            if (file == null) {
                throw new IllegalStateException("Instanced with a NULL OCFile");
            }
            if (user == null) {
                throw new IllegalStateException("Instanced with a NULL ownCloud Account");
            }
        } else {
            file = savedInstanceState.getParcelable(EXTRA_FILE);
            setFile(file);
            user = savedInstanceState.getParcelable(EXTRA_USER);
            mSavedPlaybackPosition = savedInstanceState.getInt(EXTRA_PLAY_POSITION);
            mAutoplay = savedInstanceState.getBoolean(EXTRA_PLAYING);
        }

        if (file != null) {
            if (MimeTypeUtil.isVideo(file)) {
                mVideoPreview.setVisibility(View.VISIBLE);
                mImagePreview.setVisibility(View.GONE);
                prepareVideo();
            } else {
                mVideoPreview.setVisibility(View.GONE);
                mImagePreview.setVisibility(View.VISIBLE);
                extractAndSetCoverArt(file);
            }
        }
        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    /**
     * tries to read the cover art from the audio file and sets it as cover art.
     *
     * @param file audio file with potential cover art
     */
    private void extractAndSetCoverArt(OCFile file) {
        if (MimeTypeUtil.isAudio(file)) {
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Log_OC.v(TAG, "onSaveInstanceState");
        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        outState.putParcelable(EXTRA_FILE, getFile());
        outState.putParcelable(EXTRA_USER, user);

        if (MimeTypeUtil.isVideo(getFile())) {
            if (mVideoPreview != null) {
                mSavedPlaybackPosition = mVideoPreview.getCurrentPosition();
                mAutoplay = mVideoPreview.isPlaying();
                outState.putInt(EXTRA_PLAY_POSITION, mSavedPlaybackPosition);
                outState.putBoolean(EXTRA_PLAYING, mAutoplay);
            }
        } else if(mMediaPlayerServiceConnection.isConnected()) {
            outState.putInt(EXTRA_PLAY_POSITION, mMediaPlayerServiceConnection.getCurrentPosition());
            outState.putBoolean(EXTRA_PLAYING, mMediaPlayerServiceConnection.isPlaying());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.v(TAG, "onStart");
        OCFile file = getFile();
        if (file != null) {
            if (MimeTypeUtil.isAudio(file)) {
                mMediaController.setMediaPlayer(mMediaPlayerServiceConnection);
                mMediaPlayerServiceConnection.bind();
                mMediaPlayerServiceConnection.start(user, file, mAutoplay, mSavedPlaybackPosition);
                mMultiListContainer.setVisibility(View.GONE);
                mPreviewContainer.setVisibility(View.VISIBLE);
            } else if (MimeTypeUtil.isVideo(file)) {
                if (mMediaPlayerServiceConnection.isConnected() && mMediaPlayerServiceConnection.isPlaying()) {
                    stopAudio();
                }
                playVideo();
            }
        }
    }

    private void stopAudio() {
        mMediaPlayerServiceConnection.stop();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.removeItem(R.id.action_search);
        inflater.inflate(R.menu.item_file, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (containerActivity.getStorageManager() != null) {
            User currentUser = accountManager.getUser();
            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                containerActivity,
                getActivity(),
                false,
                deviceInfo,
                currentUser
            );

            mf.filter(menu, true);
        }

        // additional restriction for this fragment
        // TODO allow renaming in PreviewImageFragment
        MenuItem item = menu.findItem(R.id.action_rename_file);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_select_all);
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

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_favorite);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_unset_favorite);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        if(getFile().isSharedWithMe() && !getFile().canReshare()){
            // additional restriction for this fragment
            item = menu.findItem(R.id.action_send_share_file);
            if(item != null){
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send_share_file: {
                sendShareFile();
                return true;
            }
            case R.id.action_open_file_with: {
                openFile();
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_see_details: {
                seeDetails();
                return true;
            }
            case R.id.action_sync_file: {
                containerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
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

    private void seeDetails() {
        stopPreview(false);
        containerActivity.showDetails(getFile());
    }

    private void sendShareFile() {
        stopPreview(false);
        containerActivity.getFileOperationsHelper().sendShareFile(getFile());
    }

    private void prepareVideo() {
        // create helper to get more control on the playback
        VideoHelper videoHelper = new VideoHelper();
        mVideoPreview.setOnPreparedListener(videoHelper);
        mVideoPreview.setOnCompletionListener(videoHelper);
        mVideoPreview.setOnErrorListener(videoHelper);
    }

    private void playVideo() {
        // create and prepare control panel for the user
        mMediaController.setMediaPlayer(mVideoPreview);

        // load the video file in the video player
        // when done, VideoHelper#onPrepared() will be called
        if (getFile().isDown()) {
            mVideoPreview.setVideoURI(getFile().getStorageUri());
        } else {
            try {
                OwnCloudClient client = clientFactory.create(user);
                new LoadStreamUrl(this, client).execute(getFile().getLocalId());
            } catch (Exception e) {
                Log_OC.e(TAG, "Loading stream url not possible: " + e);
            }
        }
    }

    private static class LoadStreamUrl extends AsyncTask<String, Void, Uri> {

        private OwnCloudClient client;
        private WeakReference<PreviewMediaFragment> previewMediaFragmentWeakReference;

        public LoadStreamUrl(PreviewMediaFragment previewMediaFragment, OwnCloudClient client) {
            this.client = client;
            this.previewMediaFragmentWeakReference = new WeakReference<>(previewMediaFragment);
        }

        @Override
        protected Uri doInBackground(String... fileId) {
            StreamMediaFileOperation sfo = new StreamMediaFileOperation(fileId[0]);
            RemoteOperationResult result = sfo.execute(client);

            if (!result.isSuccess()) {
                return null;
            }

            return Uri.parse((String) result.getData().get(0));
        }

        @Override
        protected void onPostExecute(Uri uri) {
            final PreviewMediaFragment previewMediaFragment = previewMediaFragmentWeakReference.get();
            final Context context = previewMediaFragment != null ? previewMediaFragment.getContext() : null;
            if (previewMediaFragment != null && context != null) {
                if (uri != null) {
                    previewMediaFragment.mVideoUri = uri;
                    previewMediaFragment.mVideoPreview.setVideoURI(uri);
                } else {
                    previewMediaFragment.mMultiListContainer.setVisibility(View.VISIBLE);
                    previewMediaFragment.setMessageForMultiList(
                        previewMediaFragment.getString(R.string.stream_not_possible_headline),
                        R.string.stream_not_possible_message, R.drawable.file_movie);
                }
            } else {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!");
            }
        }
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
            mMultiListContainer.setVisibility(View.GONE);
            mPreviewContainer.setVisibility(View.VISIBLE);
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
            mPreviewContainer.setVisibility(View.GONE);
            final Context context = getActivity();
            if (mVideoPreview.getWindowToken() != null && context != null) {
                String message = ErrorFormat.toString(context, what, extra);
                mMultiListContainer.setVisibility(View.VISIBLE);
                setMessageForMultiList(message, R.string.preview_sorry, R.drawable.file_movie);
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
        mMediaPlayerServiceConnection.unbind();
        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_UNLOCKED);
        super.onStop();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && v.equals(mVideoPreview)) {
            // added a margin on the left to avoid interfering with gesture to open navigation drawer
            if (event.getX() / Resources.getSystem().getDisplayMetrics().density > MIN_DENSITY_RATIO) {
                startFullScreenVideo();
            }
            return true;
        }
        return false;
    }

    private void startFullScreenVideo() {
        Intent i = new Intent(getActivity(), PreviewVideoActivity.class);
        i.putExtra(FileActivity.EXTRA_ACCOUNT, user.toPlatformAccount());
        i.putExtra(FileActivity.EXTRA_FILE, getFile());
        i.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, mVideoPreview.isPlaying());
        i.putExtra(PreviewVideoActivity.EXTRA_STREAM_URL, mVideoUri);
        mVideoPreview.pause();
        i.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, mVideoPreview.getCurrentPosition());
        startActivityForResult(i, FileActivity.REQUEST_CODE__LAST_SHARED + 1);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log_OC.v(TAG, "onConfigurationChanged " + this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log_OC.v(TAG, "onActivityResult " + this);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            mSavedPlaybackPosition = data.getIntExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0);
            mAutoplay = data.getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, false);
        }
    }

    /**
     * Opens the previewed file with an external application.
     */
    private void openFile() {
        stopPreview(true);
        containerActivity.getFileOperationsHelper().openFile(getFile());
        finishPreview();
    }

    /**
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewMediaFragment}
     * to be previewed.
     *
     * @param file File to test if can be previewed.
     * @return 'True' if the file can be handled by the fragment.
     */
    public static boolean canBePreviewed(OCFile file) {
        return file != null && (MimeTypeUtil.isAudio(file) || MimeTypeUtil.isVideo(file));
    }

    public void stopPreview(boolean stopAudio) {
        OCFile file = getFile();
        if (MimeTypeUtil.isAudio(file) && stopAudio) {
            mMediaPlayerServiceConnection.pause();
        } else if (MimeTypeUtil.isVideo(file)) {
            mVideoPreview.stopPlayback();
        }
    }

    /**
     * Finishes the preview
     */
    private void finishPreview() {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.onBackPressed();
        }
    }

    public int getPosition() {
        if (mPrepared) {
            mSavedPlaybackPosition = mVideoPreview.getCurrentPosition();
        }
        Log_OC.v(TAG, "getting position: " + mSavedPlaybackPosition);
        return mSavedPlaybackPosition;
    }

    private void toggleDrawerLockMode(ContainerActivity containerActivity, int lockMode) {
        ((DrawerActivity) containerActivity).setDrawerLockMode(lockMode);
    }
}
