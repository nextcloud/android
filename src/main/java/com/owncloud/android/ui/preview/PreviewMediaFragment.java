/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author Chris Narkiewicz
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *   Copyright (C) 2020 Andy Scherzinger
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
 */
package com.owncloud.android.ui.preview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
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
import android.widget.LinearLayout;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.StyledPlayerControlView;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.media.PlayerServiceConnection;
import com.nextcloud.client.network.ClientFactory;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FragmentPreviewMediaBinding;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.files.StreamMediaFileOperation;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.fragment.FileFragment;
import com.owncloud.android.utils.MimeTypeUtil;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * This fragment shows a preview of a downloaded media file (audio or video).
 * <p>
 * Trying to get an instance with NULL {@link OCFile} or ownCloud {@link User} values will produce an {@link
 * IllegalStateException}.
 * <p>
 * By now, if the {@link OCFile} passed is not downloaded, an {@link IllegalStateException} is generated on
 * instantiation too.
 */
public class PreviewMediaFragment extends FileFragment implements OnTouchListener,
    Injectable, StyledPlayerControlView.OnFullScreenModeChangedListener {

    private static final String TAG = PreviewMediaFragment.class.getSimpleName();

    public static final String EXTRA_FILE = "FILE";
    public static final String EXTRA_USER = "USER";
    private static final String EXTRA_PLAY_POSITION = "PLAY_POSITION";
    private static final String EXTRA_PLAYING = "PLAYING";
    private static final double MIN_DENSITY_RATIO = 24.0;
    private static final int MENU_FULLSCREEN_ID = 3344;

    private static final String FILE = "FILE";
    private static final String USER = "USER";
    private static final String PLAYBACK_POSITION = "PLAYBACK_POSITION";
    private static final String AUTOPLAY = "AUTOPLAY";

    private User user;
    private long savedPlaybackPosition;

    private boolean autoplay;
    private boolean prepared;
    private PlayerServiceConnection mediaPlayerServiceConnection;

    private Uri videoUri;
    @Inject ClientFactory clientFactory;
    @Inject UserAccountManager accountManager;
    FragmentPreviewMediaBinding binding;
    LinearLayout emptyListView;
    private SimpleExoPlayer exoPlayer;

    /**
     * Creates a fragment to preview a file.
     * <p>
     * When 'fileToDetail' or 'user' are null
     *
     * @param fileToDetail An {@link OCFile} to preview in the fragment
     * @param user         Currently active user
     */
    public static PreviewMediaFragment newInstance(OCFile fileToDetail,
                                                   User user,
                                                   long startPlaybackPosition,
                                                   boolean autoplay) {
        PreviewMediaFragment previewMediaFragment = new PreviewMediaFragment();

        Bundle bundle = new Bundle();
        bundle.putParcelable(FILE, fileToDetail);
        bundle.putParcelable(USER, user);
        bundle.putLong(PLAYBACK_POSITION, startPlaybackPosition);
        bundle.putBoolean(AUTOPLAY, autoplay);

        previewMediaFragment.setArguments(bundle);

        return previewMediaFragment;
    }

    /**
     * Creates an empty fragment for previews.
     * <p/>
     * MUST BE KEPT: the system uses it when tries to reinstantiate a fragment automatically (for instance, when the
     * device is turned a aside).
     * <p/>
     * DO NOT CALL IT: an {@link OCFile} and {@link User} must be provided for a successful construction
     */
    public PreviewMediaFragment() {
        super();
        savedPlaybackPosition = 0;
        autoplay = true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        Bundle bundle = getArguments();

        setFile(bundle.getParcelable(FILE));
        user = bundle.getParcelable(USER);
        savedPlaybackPosition = bundle.getLong(PLAYBACK_POSITION);
        autoplay = bundle.getBoolean(AUTOPLAY);
        mediaPlayerServiceConnection = new PlayerServiceConnection(getContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Log_OC.v(TAG, "onCreateView");

        binding = FragmentPreviewMediaBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        emptyListView = binding.emptyView.emptyListView;

        setLoadingView();
        return view;
    }

    private void setLoadingView() {
        binding.progress.setVisibility(View.VISIBLE);
        binding.emptyView.emptyListView.setVisibility(View.GONE);
    }

    private void setVideoErrorMessage(String headline, @StringRes int message) {
        binding.emptyView.emptyListViewHeadline.setText(headline);
        binding.emptyView.emptyListViewText.setText(message);
        binding.emptyView.emptyListIcon.setImageResource(R.drawable.file_movie);
        binding.emptyView.emptyListViewText.setVisibility(View.VISIBLE);
        binding.emptyView.emptyListIcon.setVisibility(View.VISIBLE);
        binding.progress.setVisibility(View.GONE);
        binding.emptyView.emptyListView.setVisibility(View.VISIBLE);
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
            savedPlaybackPosition = savedInstanceState.getInt(EXTRA_PLAY_POSITION);
            autoplay = savedInstanceState.getBoolean(EXTRA_PLAYING);
        }

        if (file != null) {
            if (MimeTypeUtil.isVideo(file)) {
                binding.exoplayerView.setVisibility(View.VISIBLE);
                binding.imagePreview.setVisibility(View.GONE);
            } else {
                binding.exoplayerView.setVisibility(View.GONE);
                binding.imagePreview.setVisibility(View.VISIBLE);
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
            if (file.getStoragePath() == null) {
                setThumbnailForAudio(file);
            } else {
                try {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(file.getStoragePath());
                    byte[] data = mmr.getEmbeddedPicture();
                    if (data != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        binding.imagePreview.setImageBitmap(bitmap); //associated cover art in bitmap
                    } else {
                        setThumbnailForAudio(file);
                    }
                } catch (Throwable t) {
                    binding.imagePreview.setImageResource(R.drawable.logo);
                }
            }
        }
    }

    private void setThumbnailForAudio(OCFile file) {
        Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
            ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId());

        if (thumbnail != null) {
            binding.imagePreview.setImageBitmap(thumbnail);
        } else {
            binding.imagePreview.setImageResource(R.drawable.logo);
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
            savedPlaybackPosition = exoPlayer.getCurrentPosition();
            autoplay = exoPlayer.isPlaying();
            outState.putLong(EXTRA_PLAY_POSITION, savedPlaybackPosition);
            outState.putBoolean(EXTRA_PLAYING, autoplay);
        } else if (mediaPlayerServiceConnection.isConnected()) {
            outState.putInt(EXTRA_PLAY_POSITION, mediaPlayerServiceConnection.getCurrentPosition());
            outState.putBoolean(EXTRA_PLAYING, mediaPlayerServiceConnection.isPlaying());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log_OC.v(TAG, "onStart");
        OCFile file = getFile();
        if (file != null) {
            // bind to any existing player
            mediaPlayerServiceConnection.bind();

            exoPlayer = new SimpleExoPlayer.Builder(getContext()).build();
            binding.exoplayerView.setPlayer(exoPlayer);

            if (MimeTypeUtil.isAudio(file)) {
                binding.mediaController.setMediaPlayer(mediaPlayerServiceConnection);
                binding.mediaController.setVisibility(View.VISIBLE);
                mediaPlayerServiceConnection.start(user, file, autoplay, savedPlaybackPosition);
                binding.emptyView.emptyListView.setVisibility(View.GONE);
                binding.progress.setVisibility(View.GONE);
            } else if (MimeTypeUtil.isVideo(file)) {
                if (mediaPlayerServiceConnection.isConnected()) {
                    // always stop player
                    stopAudio();
                }
                playVideo();
            }
        }
    }

    private void stopAudio() {
        mediaPlayerServiceConnection.stop();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.removeItem(R.id.action_search);
        menu.add(Menu.NONE, MENU_FULLSCREEN_ID, 99, R.string.fullscreen);
        inflater.inflate(R.menu.item_file, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (containerActivity.getStorageManager() != null) {
            User currentUser = accountManager.getUser();
            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                containerActivity,
                getActivity(),
                false,
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

        if (getFile().isSharedWithMe() && !getFile().canReshare()) {
            // additional restriction for this fragment
            item = menu.findItem(R.id.action_send_share_file);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_send_share_file) {
            sendShareFile();
            return true;
        } else if (itemId == R.id.action_open_file_with) {
            openFile();
            return true;
        } else if (itemId == R.id.action_remove_file) {
            RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
            dialog.show(getFragmentManager(), ConfirmationDialogFragment.FTAG_CONFIRMATION);
            return true;
        } else if (itemId == R.id.action_see_details) {
            seeDetails();
            return true;
        } else if (itemId == R.id.action_sync_file) {
            containerActivity.getFileOperationsHelper().syncFile(getFile());
            return true;
        } else if (itemId == MENU_FULLSCREEN_ID) {
            startFullScreenVideo();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Update the file of the fragment with file value
     *
     * @param file Replaces the held file with a new one
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

    private void playVideo() {
        // load the video file in the video player
        // when done, VideoHelper#onPrepared() will be called
        if (getFile().isDown()) {
            binding.progress.setVisibility(View.GONE);

            exoPlayer.addMediaItem(MediaItem.fromUri(getFile().getStorageUri()));
            exoPlayer.prepare();

            if (savedPlaybackPosition >= 0) {
                exoPlayer.seekTo(savedPlaybackPosition);
            }
            exoPlayer.play();
        } else {
            try {
                new LoadStreamUrl(this, user, clientFactory).execute(getFile().getLocalId());
            } catch (Exception e) {
                Log_OC.e(TAG, "Loading stream url not possible: " + e);
            }
        }
    }

    @Override
    public void onFullScreenModeChanged(boolean isFullScreen) {
        Log_OC.e(TAG, "Fullscreen: " + isFullScreen);
    }

    private static class LoadStreamUrl extends AsyncTask<String, Void, Uri> {

        private final ClientFactory clientFactory;
        private final User user;
        private final WeakReference<PreviewMediaFragment> previewMediaFragmentWeakReference;

        public LoadStreamUrl(PreviewMediaFragment previewMediaFragment, User user, ClientFactory clientFactory) {
            this.previewMediaFragmentWeakReference = new WeakReference<>(previewMediaFragment);
            this.user = user;
            this.clientFactory = clientFactory;
        }

        @Override
        protected Uri doInBackground(String... fileId) {
            OwnCloudClient client;
            try {
                client = clientFactory.create(user);
            } catch (ClientFactory.CreationException e) {
                Log_OC.e(TAG, "Loading stream url not possible: " + e);
                return null;
            }

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
            if (previewMediaFragment != null && previewMediaFragment.binding != null && context != null) {
                if (uri != null) {
                    previewMediaFragment.videoUri = uri;

                    previewMediaFragment.binding.progress.setVisibility(View.GONE);

                    previewMediaFragment.exoPlayer.addMediaItem(MediaItem.fromUri(uri));
                    previewMediaFragment.exoPlayer.prepare();
                    previewMediaFragment.exoPlayer.play();
                } else {
                    previewMediaFragment.emptyListView.setVisibility(View.VISIBLE);
                    previewMediaFragment.setVideoErrorMessage(
                        previewMediaFragment.getString(R.string.stream_not_possible_headline),
                        R.string.stream_not_possible_message);
                }
            } else {
                Log_OC.e(TAG, "Error streaming file: no previewMediaFragment!");
            }
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
        autoplay = false;
        Log_OC.v(TAG, "onResume");
    }

    @Override
    public void onDestroy() {
        Log_OC.v(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        Log_OC.v(TAG, "onDestroyView");
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStop() {
        Log_OC.v(TAG, "onStop");
        if (MimeTypeUtil.isAudio(getFile()) && !mediaPlayerServiceConnection.isPlaying()) {
            stopAudio();
        }
        
        mediaPlayerServiceConnection.unbind();
        toggleDrawerLockMode(containerActivity, DrawerLayout.LOCK_MODE_UNLOCKED);
        super.onStop();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && v.equals(binding.exoplayerView)) {
            // added a margin on the left to avoid interfering with gesture to open navigation drawer
            if (event.getX() / Resources.getSystem().getDisplayMetrics().density > MIN_DENSITY_RATIO) {
                startFullScreenVideo();
            }
            return true;
        }
        return false;
    }

    private void startFullScreenVideo() {
        Intent intent = new Intent(getActivity(), PreviewVideoActivity.class);
        intent.putExtra(FileActivity.EXTRA_ACCOUNT, user.toPlatformAccount());
        intent.putExtra(FileActivity.EXTRA_FILE, getFile());
        intent.putExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, exoPlayer.isPlaying());
        intent.putExtra(PreviewVideoActivity.EXTRA_STREAM_URL, videoUri);
        exoPlayer.pause();
        intent.putExtra(PreviewVideoActivity.EXTRA_START_POSITION, exoPlayer.getCurrentPosition());
        startActivityForResult(intent, FileActivity.REQUEST_CODE__LAST_SHARED + 1);
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
            savedPlaybackPosition = data.getLongExtra(PreviewVideoActivity.EXTRA_START_POSITION, 0);
            autoplay = data.getBooleanExtra(PreviewVideoActivity.EXTRA_AUTOPLAY, false);
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
     * Helper method to test if an {@link OCFile} can be passed to a {@link PreviewMediaFragment} to be previewed.
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
            mediaPlayerServiceConnection.pause();
        } else if (MimeTypeUtil.isVideo(file)) {
            exoPlayer.stop(true);
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

    public long getPosition() {
        if (prepared) {
            savedPlaybackPosition = exoPlayer.getCurrentPosition();
        }
        Log_OC.v(TAG, "getting position: " + savedPlaybackPosition);
        return savedPlaybackPosition;
    }

    private void toggleDrawerLockMode(ContainerActivity containerActivity, int lockMode) {
        ((DrawerActivity) containerActivity).setDrawerLockMode(lockMode);
    }

    @Override
    public void onDetach() {
      
        exoPlayer.stop();
        exoPlayer.release();

        super.onDetach();
    }
}
