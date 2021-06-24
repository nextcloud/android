/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author David A. Velasco
 *   @author Andy Scherzinger
 *   @author Chris Narkiewicz
 *
 *   Copyright (C) 2011 Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2018 Andy Scherzinger
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
 */
package com.owncloud.android.ui.fragment;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.google.android.material.tabs.TabLayout;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.DeviceInfo;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.FileDetailsFragmentBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.adapter.FileDetailTabAdapter;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.content.res.ResourcesCompat;

/**
 * This Fragment is used to display the details about a file.
 */
public class FileDetailFragment extends FileFragment implements OnClickListener, Injectable {
    private static final String TAG = FileDetailFragment.class.getSimpleName();
    private static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";
    static final String FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT";
    private static final String FTAG_SHARING = "SHARING_DETAILS_FRAGMENT";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_USER = "USER";
    private static final String ARG_ACTIVE_TAB = "TAB";
    private View view;
    private User user;
    private boolean previewLoaded;
    /**
     * variable to check if custom back icon on toolbar has to be shown
     */
    private boolean isCustomBackIcon;

    private FileDetailsFragmentBinding binding;
    private ProgressListener progressListener;
    private ToolbarActivity toolbarActivity;
    private int activeTab;

    @Inject AppPreferences preferences;
    @Inject ConnectivityService connectivityService;
    @Inject UserAccountManager accountManager;
    @Inject DeviceInfo deviceInfo;

    /**
     * Public factory method to create new FileDetailFragment instances.
     * <p>
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     *
     * @param fileToDetail An {@link OCFile} to show in the fragment
     * @param user         Currently active user
     * @return New fragment with arguments set
     */
    public static FileDetailFragment newInstance(OCFile fileToDetail, User user) {
        FileDetailFragment frag = new FileDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToDetail);
        args.putParcelable(ARG_USER, user);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Public factory method to create new FileDetailFragment instances.
     * <p>
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     *
     * @param fileToDetail An {@link OCFile} to show in the fragment
     * @param user         Currently active user
     * @param activeTab    to be active tab
     * @return New fragment with arguments set
     */
    public static FileDetailFragment newInstance(OCFile fileToDetail, User user, int activeTab) {
        FileDetailFragment frag = new FileDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToDetail);
        args.putParcelable(ARG_USER, user);
        args.putInt(ARG_ACTIVE_TAB, activeTab);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Creates an empty details fragment.
     * <p>
     * It's necessary to keep a public constructor without parameters; the system uses it when tries to reinstate a
     * fragment automatically.
     */
    public FileDetailFragment() {
        super();
        user = null;
        progressListener = null;
    }

    /**
     * return the reference to the file detail sharing fragment to communicate with it.
     *
     * @return reference to the {@link FileDetailSharingFragment}
     */
    public FileDetailSharingFragment getFileDetailSharingFragment() {
        return (FileDetailSharingFragment)getChildFragmentManager().findFragmentByTag(FTAG_SHARING);
    }

    /**
     * return the reference to the file detail activity fragment to communicate with it.
     *
     * @return reference to the {@link FileDetailActivitiesFragment}
     */
    public FileDetailActivitiesFragment getFileDetailActivitiesFragment() {
        //return ((FileDetailTabAdapter) binding.pager.getAdapter()).getFileDetailActivitiesFragment();
        return null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Bundle arguments = getArguments();

        if (arguments == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }

        setFile(arguments.getParcelable(ARG_FILE));
        user = arguments.getParcelable(ARG_USER);
        activeTab = arguments.getInt(ARG_ACTIVE_TAB, 0);

        if (savedInstanceState != null) {
            setFile(savedInstanceState.getParcelable(ARG_FILE));
            user = savedInstanceState.getParcelable(ARG_USER);
        }

        binding = FileDetailsFragmentBinding.inflate(inflater, container, false);
        view = binding.getRoot();

        if (getFile() == null || user == null) {
            showEmptyContent();
        } else {
            binding.emptyList.emptyListView.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getFile() != null && user != null) {
            ThemeUtils.colorHorizontalProgressBar(binding.progressBar, ThemeUtils.primaryAccentColor(getContext()));
            progressListener = new ProgressListener(binding.progressBar);
            binding.cancelBtn.setOnClickListener(this);
            binding.favorite.setOnClickListener(this);
            binding.overflowMenu.setOnClickListener(this);
            binding.lastModificationTimestamp.setOnClickListener(this);

            updateFileDetails(false, false);
        }
    }

    private void replaceSharingFragment() {
        getChildFragmentManager().beginTransaction()
            .replace(R.id.sharing_frame_container,
                     FileDetailSharingFragment.newInstance(getFile(), user),
                     FTAG_SHARING).commit();
    }

    public void replaceSharingProcessFragment(String shareeName, ShareType shareType){
        requireActivity().getSupportFragmentManager().beginTransaction().replace(R.id.sharing_frame_container,
                                                           FileDetailsSharingProcessFragment.newInstance(getFile(),
                                                                                                         shareeName,
                                                                                                         shareType),
                                                           FileDetailsSharingProcessFragment.TAG)
            .addToBackStack(null)
            .commit();
    }

    private void onOverflowIconClicked(View view) {
        ContextThemeWrapper ctw = new ContextThemeWrapper(getActivity(), R.style.CustomPopupTheme);
        PopupMenu popup = new PopupMenu(ctw, view);
        popup.inflate(R.menu.fragment_file_detail);
        prepareOptionsMenu(popup.getMenu());

        popup.setOnMenuItemClickListener(this::optionsItemSelected);
        popup.show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_FILE, getFile());
        outState.putParcelable(ARG_USER, user);
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForTransferProgress();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (toolbarActivity != null) {
            toolbarActivity.showSortListGroup(false);

            if (previewLoaded) {
                toolbarActivity.setPreviewImageVisibility(true);
            }
            toolbarActivity.showToolbarBackImage(isCustomBackIcon);
        }

    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        leaveTransferProgress();

        if (toolbarActivity != null) {
            toolbarActivity.hidePreviewImage();
            toolbarActivity.showToolbarBackImage(false);
        }

        super.onStop();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ToolbarActivity) {
            toolbarActivity = (ToolbarActivity) context;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public View getView() {
        return super.getView() == null ? view : super.getView();
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);

        FileMenuFilter.hideAll(menu);
    }

    private void prepareOptionsMenu(Menu menu) {
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

        if (getFile().isFolder()) {
            FileMenuFilter.hideMenuItems(menu.findItem(R.id.action_send_file));
            FileMenuFilter.hideMenuItems(menu.findItem(R.id.action_sync_file));
        }
    }

    private boolean optionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send_file: {
                containerActivity.getFileOperationsHelper().sendShareFile(getFile(), true);
                return true;
            }
            case R.id.action_open_file_with: {
                containerActivity.getFileOperationsHelper().openFile(getFile());
                return true;
            }
            case R.id.action_remove_file: {
                RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), FTAG_CONFIRMATION);
                return true;
            }
            case R.id.action_rename_file: {
                RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(getFile());
                dialog.show(getFragmentManager(), FTAG_RENAME_FILE);
                return true;
            }
            case R.id.action_cancel_sync: {
                ((FileDisplayActivity) containerActivity).cancelTransference(getFile());
                return true;
            }
            case R.id.action_download_file:
            case R.id.action_sync_file: {
                containerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_set_as_wallpaper: {
                containerActivity.getFileOperationsHelper().setPictureAs(getFile(), getView());
                return true;
            }
            case R.id.action_encrypted: {
                // TODO implement or remove
                return true;
            }
            case R.id.action_unset_encrypted: {
                // TODO implement or remove
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancelBtn: {
                ((FileDisplayActivity) containerActivity).cancelTransference(getFile());
                break;
            }
            case R.id.favorite: {
                if (getFile().isFavorite()) {
                    containerActivity.getFileOperationsHelper().toggleFavoriteFile(getFile(), false);
                } else {
                    containerActivity.getFileOperationsHelper().toggleFavoriteFile(getFile(), true);
                }
                setFavoriteIconStatus(!getFile().isFavorite());
                break;
            }
            case R.id.overflow_menu: {
                onOverflowIconClicked(v);
                break;
            }
            case R.id.last_modification_timestamp: {
                boolean showDetailedTimestamp = !preferences.isShowDetailedTimestampEnabled();
                preferences.setShowDetailedTimestampEnabled(showDetailedTimestamp);
                setFileModificationTimestamp(getFile(), showDetailedTimestamp);
            }
            default:
                Log_OC.e(TAG, "Incorrect view clicked!");
                break;
        }
    }

    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be
     * replaced.
     *
     * @return True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return getFile() == null || user == null;
    }

    /**
     * Use this method to signal this Activity that it shall update its view.
     *
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, User user) {
        setFile(file);
        this.user = user;
        updateFileDetails(false, false);
    }

    /**
     * Updates the view with all relevant details about that file.
     * <p>
     * TODO Remove parameter when the transferring state of files is kept in database.
     *
     * @param transferring Flag signaling if the file should be considered as downloading or uploading, although {@link
     *                     FileDownloaderBinder#isDownloading(User, OCFile)}  and {@link FileUploaderBinder#isUploading(User,
     *                     OCFile)} return false.
     * @param refresh      If 'true', try to refresh the whole file from the database
     */
    public void updateFileDetails(boolean transferring, boolean refresh) {
        if (readyToShow()) {
            FileDataStorageManager storageManager = containerActivity.getStorageManager();

            if (storageManager == null) {
                return;
            }

            if (refresh) {
                setFile(storageManager.getFileByPath(getFile().getRemotePath()));
            }
            OCFile file = getFile();

            // set file details
            binding.filename.setText(file.getFileName());
            binding.size.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));

            boolean showDetailedTimestamp = preferences.isShowDetailedTimestampEnabled();
            setFileModificationTimestamp(file, showDetailedTimestamp);

            setFilePreview(file);
            setFavoriteIconStatus(file.isFavorite());

            // configure UI for depending upon local state of the file
            FileDownloaderBinder downloaderBinder = containerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = containerActivity.getFileUploaderBinder();
            if (transferring
                || (downloaderBinder != null && downloaderBinder.isDownloading(user, file))
                || (uploaderBinder != null && uploaderBinder.isUploading(user, file))) {
                setButtonsForTransferring();

            } else if (file.isDown()) {

                setButtonsForDown();

            } else {
                // TODO load default preview image; when the local file is removed, the preview
                // remains there
                setButtonsForRemote();
            }
        }
        replaceSharingFragment();
        getView().invalidate();
    }

    private void setFileModificationTimestamp(OCFile file, boolean showDetailedTimestamp) {
        if (showDetailedTimestamp) {
            binding.lastModificationTimestamp.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
        } else {
            binding.lastModificationTimestamp.setText(DisplayUtils.getRelativeTimestamp(getContext(),
                                                                                        file.getModificationTimestamp()));
        }
    }

    private void setFavoriteIconStatus(boolean isFavorite) {
        if (isFavorite) {
            binding.favorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_star, null));
        } else {
            binding.favorite.setImageDrawable(ResourcesCompat.getDrawable(getResources(),
                                                                          R.drawable.ic_star_outline,
                                                                          null));
        }
    }

    /**
     * Checks if the fragment is ready to show details of a OCFile
     *
     * @return 'True' when the fragment is ready to show details of a file
     */
    private boolean readyToShow() {
        return getFile() != null && user != null;
    }

    /**
     * Updates the file preview if possible
     *
     * @param file a {@link OCFile} to be previewed
     */
    private void setFilePreview(OCFile file) {
        Bitmap resizedImage;

        if (toolbarActivity != null) {
            if (file.isFolder()) {
                toolbarActivity.setPreviewImageDrawable(MimeTypeUtil
                                                            .getFolderTypeIcon(file.isSharedWithMe() || file.isSharedWithSharee(),
                                                                               file.isSharedViaLink(), file.isEncrypted(),
                                                                               file.getMountType(), requireContext()));
                int leftRightPadding = requireContext().getResources().getDimensionPixelSize(R.dimen.standard_padding);
                updatePreviewImageUI(leftRightPadding);

                previewLoaded = true;
                isCustomBackIcon = false;
            } else {
                if (file.getRemoteId() != null && file.isPreviewAvailable()) {
                    String tagId = ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + getFile().getRemoteId();
                    resizedImage = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId);

                    if (resizedImage != null && !file.isUpdateThumbnailNeeded()) {
                        toolbarActivity.setPreviewImageBitmap(resizedImage);
                        toolbarActivity.showToolbarBackImage(true);
                        previewLoaded = true;
                        isCustomBackIcon = true;
                    } else {
                        // show thumbnail while loading resized image
                        Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                            ThumbnailsCacheManager.PREFIX_THUMBNAIL + getFile().getRemoteId());

                        if (thumbnail != null) {
                            toolbarActivity.setPreviewImageBitmap(thumbnail);
                            toolbarActivity.showToolbarBackImage(true);
                            previewLoaded = true;
                            isCustomBackIcon = true;
                        } else {
                            Drawable drawable = MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                             file.getFileName(),
                                                                             user,
                                                                             requireContext());
                            if (drawable == null) {
                                thumbnail = ThumbnailsCacheManager.mDefaultImg;
                                toolbarActivity.setPreviewImageBitmap(thumbnail);
                            } else {
                                toolbarActivity.setPreviewImageDrawable(drawable);
                                previewLoaded = true;
                                isCustomBackIcon = false;
                            }
                            updatePreviewImageUIForFiles();
                        }

                        if (MimeTypeUtil.isImage(file)) {
                            // generate new resized image
                            if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(getFile(), toolbarActivity.getPreviewImageView()) &&
                                containerActivity.getStorageManager() != null) {
                                final ThumbnailsCacheManager.ResizedImageGenerationTask task =
                                    new ThumbnailsCacheManager.ResizedImageGenerationTask(this,
                                                                                          toolbarActivity.getPreviewImageView(),
                                                                                          toolbarActivity.getPreviewImageContainer(),
                                                                                          containerActivity.getStorageManager(),
                                                                                          connectivityService,
                                                                                          containerActivity.getStorageManager().getAccount(),
                                                                                          getResources().getColor(R.color.background_color_inverse)
                                    );

                                if (resizedImage == null) {
                                    resizedImage = thumbnail;
                                }

                                final ThumbnailsCacheManager.AsyncResizedImageDrawable asyncDrawable =
                                    new ThumbnailsCacheManager.AsyncResizedImageDrawable(
                                        MainApp.getAppContext().getResources(),
                                        resizedImage,
                                        task
                                    );

                                toolbarActivity.setPreviewImageDrawable(asyncDrawable);
                                toolbarActivity.showToolbarBackImage(true);
                                previewLoaded = true;
                                isCustomBackIcon = false;
                                task.execute(getFile());
                            }
                        }
                    }
                } else {
                    toolbarActivity.setPreviewImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimeType(),
                                                                                         file.getFileName(),
                                                                                         user,
                                                                                         requireContext()));
                    updatePreviewImageUIForFiles();
                    previewLoaded = true;
                    isCustomBackIcon = false;
                }
            }
        } else {
            previewLoaded = false;
            isCustomBackIcon = false;
        }
    }

    /**
     * update preview image for files we are taking different paddings for files and folders
     */
    private void updatePreviewImageUIForFiles() {
        int leftRightPadding = requireContext().getResources().getDimensionPixelSize(R.dimen.standard_half_padding);
        updatePreviewImageUI(leftRightPadding);
    }

    /**
     * change scale type and padding for folders and files without thumbnails
     */
    private void updatePreviewImageUI(int leftRightPadding) {
        toolbarActivity.getPreviewImageView().setScaleType(ImageView.ScaleType.FIT_START);
        int topPadding = requireContext().getResources().getDimensionPixelSize(R.dimen.activity_row_layout_height);
        int bottomPadding = requireContext().getResources().getDimensionPixelSize(R.dimen.standard_padding);
        toolbarActivity.getPreviewImageView().setPadding(leftRightPadding, topPadding, leftRightPadding, bottomPadding);
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            // show the progress bar for the transfer
            binding.progressBlock.setVisibility(View.VISIBLE);
            binding.progressText.setVisibility(View.VISIBLE);
            FileDownloaderBinder downloaderBinder = containerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = containerActivity.getFileUploaderBinder();
            //if (getFile().isDownloading()) {
            if (downloaderBinder != null && downloaderBinder.isDownloading(user, getFile())) {
                binding.progressText.setText(R.string.downloader_download_in_progress_ticker);
            } else {
                if (uploaderBinder != null && uploaderBinder.isUploading(user, getFile())) {
                    binding.progressText.setText(R.string.uploader_upload_in_progress_ticker);
                }
            }
        }
    }

    /**
     * Enables or disables buttons for a file locally available
     */
    private void setButtonsForDown() {
        if (!isEmpty()) {
            // hides the progress bar
            binding.progressBlock.setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            // hides the progress bar
            binding.progressBlock.setVisibility(View.GONE);
        }
    }

    public void listenForTransferProgress() {
        if (progressListener != null) {
            if (containerActivity.getFileDownloaderBinder() != null) {
                containerActivity.getFileDownloaderBinder().
                    addDatatransferProgressListener(progressListener, getFile());
            }
            if (containerActivity.getFileUploaderBinder() != null) {
                containerActivity.getFileUploaderBinder().
                    addDatatransferProgressListener(progressListener, user, getFile());
            }
        } else {
            Log_OC.d(TAG, "progressListener == null");
        }
    }

    private void leaveTransferProgress() {
        if (progressListener != null) {
            if (containerActivity.getFileDownloaderBinder() != null) {
                containerActivity.getFileDownloaderBinder().
                    removeDatatransferProgressListener(progressListener, getFile());
            }
            if (containerActivity.getFileUploaderBinder() != null) {
                containerActivity.getFileUploaderBinder().
                    removeDatatransferProgressListener(progressListener, user, getFile());
            }
        }
    }

    private void showEmptyContent() {
        binding.emptyList.emptyListView.setVisibility(View.VISIBLE);
        binding.detailContainer.setVisibility(View.GONE);

        binding.emptyList.emptyListViewHeadline.setText(R.string.file_details_no_content);

        binding.emptyList.emptyListIcon.setImageResource(R.drawable.ic_list_empty_error);
        binding.emptyList.emptyListIcon.setVisibility(View.VISIBLE);
    }

    /**
     * Helper class responsible for updating the progress bar shown for file downloading.
     */
    private class ProgressListener implements OnDatatransferProgressListener {
        private int lastPercent;
        private WeakReference<ProgressBar> progressBarReference;

        ProgressListener(ProgressBar progressBar) {
            progressBarReference = new WeakReference<>(progressBar);
        }

        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar,
                                       long totalToTransfer, String filename) {
            int percent = (int) (100.0 * ((double) totalTransferredSoFar) / ((double) totalToTransfer));
            if (percent != lastPercent) {
                ProgressBar pb = progressBarReference.get();
                if (pb != null) {
                    pb.setProgress(percent);
                    pb.postInvalidate();
                }
            }
            lastPercent = percent;
        }
    }
}
