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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ProgressBar;

import com.google.android.material.tabs.TabLayout;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
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
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.adapter.FileDetailTabAdapter;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ThemeBarUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;
import com.owncloud.android.utils.theme.ThemeLayoutUtils;

import java.lang.ref.WeakReference;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

/**
 * This Fragment is used to display the details about a file.
 */
public class FileDetailFragment extends FileFragment implements OnClickListener, Injectable {
    private static final String TAG = FileDetailFragment.class.getSimpleName();
    private static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";
    static final String FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_USER = "USER";
    private static final String ARG_ACTIVE_TAB = "TAB";
    private View view;
    private User user;
    private boolean previewLoaded;

    private FileDetailsFragmentBinding binding;
    private ProgressListener progressListener;
    private ToolbarActivity toolbarActivity;
    private int activeTab;

    @Inject AppPreferences preferences;
    @Inject ConnectivityService connectivityService;
    @Inject UserAccountManager accountManager;

    /**
     * Public factory method to create new FileDetailFragment instances.
     *
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     *
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param user              Currently active user
     * @return                  New fragment with arguments set
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
     *
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     *
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param user              Currently active user
     *                          @param activeTab to be active tab
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
     *
     * It's necessary to keep a public constructor without parameters; the system uses it when tries
     * to reinstate a fragment automatically.
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
        return ((FileDetailTabAdapter) binding.pager.getAdapter()).getFileDetailSharingFragment();
    }

    /**
     * return the reference to the file detail activity fragment to communicate with it.
     *
     * @return reference to the {@link FileDetailActivitiesFragment}
     */
    public FileDetailActivitiesFragment getFileDetailActivitiesFragment() {
        return ((FileDetailTabAdapter) binding.pager.getAdapter()).getFileDetailActivitiesFragment();
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

        binding = FileDetailsFragmentBinding.inflate(inflater,container,false);
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
            ThemeBarUtils.colorHorizontalProgressBar(binding.progressBar, ThemeColorUtils.primaryAccentColor(getContext()));
            progressListener = new ProgressListener(binding.progressBar);
            binding.cancelBtn.setOnClickListener(this);
            binding.favorite.setOnClickListener(this);
            binding.overflowMenu.setOnClickListener(this);
            binding.lastModificationTimestamp.setOnClickListener(this);

            updateFileDetails(false, false);
        }
    }

    private void onOverflowIconClicked(View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.inflate(R.menu.fragment_file_detail);
        prepareOptionsMenu(popup.getMenu());

        popup.setOnMenuItemClickListener(this::optionsItemSelected);
        popup.show();
    }

    private void setupViewPager() {
        binding.tabLayout.removeAllTabs();

        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.drawer_item_activities).setIcon(R.drawable.ic_activity));
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText(R.string.share_dialog_title).setIcon(R.drawable.shared_via_users));

        ThemeLayoutUtils.colorTabLayout(getContext().getApplicationContext(), binding.tabLayout);

        final FileDetailTabAdapter adapter = new FileDetailTabAdapter(getFragmentManager(), getFile(), user);
        binding.pager.setAdapter(adapter);
        binding.pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(binding.tabLayout) {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (activeTab == 0) {
                    getFileDetailActivitiesFragment().markCommentsAsRead();
                }

                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        });
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                binding.pager.setCurrentItem(tab.getPosition());

                if (tab.getPosition() == 0) {
                    FileDetailActivitiesFragment fragment = getFileDetailActivitiesFragment();

                    if (fragment != null) {
                        fragment.markCommentsAsRead();
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // unused at the moment
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // unused at the moment
            }
        });

        binding.tabLayout.getTabAt(activeTab).select();
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
        int itemId = item.getItemId();

        if (itemId == R.id.action_send_file) {
            containerActivity.getFileOperationsHelper().sendShareFile(getFile(), true);
            return true;
        } else if (itemId == R.id.action_open_file_with) {
            containerActivity.getFileOperationsHelper().openFile(getFile());
            return true;
        } else if (itemId == R.id.action_remove_file) {
            RemoveFilesDialogFragment dialog = RemoveFilesDialogFragment.newInstance(getFile());
            dialog.show(getFragmentManager(), FTAG_CONFIRMATION);
            return true;
        } else if (itemId == R.id.action_rename_file) {
            RenameFileDialogFragment dialog = RenameFileDialogFragment.newInstance(getFile());
            dialog.show(getFragmentManager(), FTAG_RENAME_FILE);
            return true;
        } else if (itemId == R.id.action_cancel_sync) {
            ((FileDisplayActivity) containerActivity).cancelTransference(getFile());
            return true;
        } else if (itemId == R.id.action_download_file || itemId == R.id.action_sync_file) {
            containerActivity.getFileOperationsHelper().syncFile(getFile());
            return true;
        } else if (itemId == R.id.action_set_as_wallpaper) {
            containerActivity.getFileOperationsHelper().setPictureAs(getFile(), getView());
            return true;
        } else if (itemId == R.id.action_encrypted) {// TODO implement or remove
            return true;
        } else if (itemId == R.id.action_unset_encrypted) {// TODO implement or remove
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.cancelBtn) {
            ((FileDisplayActivity) containerActivity).cancelTransference(getFile());
        } else if (id == R.id.favorite) {
            containerActivity.getFileOperationsHelper().toggleFavoriteFile(getFile(), !getFile().isFavorite());
            setFavoriteIconStatus(!getFile().isFavorite());
        } else if (id == R.id.overflow_menu) {
            onOverflowIconClicked(v);
        } else if (id == R.id.last_modification_timestamp) {
            boolean showDetailedTimestamp = !preferences.isShowDetailedTimestampEnabled();
            preferences.setShowDetailedTimestampEnabled(showDetailedTimestamp);
            setFileModificationTimestamp(getFile(), showDetailedTimestamp);

            Log_OC.e(TAG, "Incorrect view clicked!");
        } else {
            Log_OC.e(TAG, "Incorrect view clicked!");
        }
    }

    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
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
     *
     * TODO Remove parameter when the transferring state of files is kept in database.
     *
     * @param transferring Flag signaling if the file should be considered as downloading or uploading,
     *                     although {@link FileDownloaderBinder#isDownloading(User, OCFile)}  and
     *                     {@link FileUploaderBinder#isUploading(User, OCFile)} return false.
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
            if (MimeTypeUtil.isImage(file)) {
                binding.filename.setText(file.getFileName());
            } else {
                binding.filename.setVisibility(View.GONE);
            }
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

        setupViewPager();

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

        if (toolbarActivity != null && MimeTypeUtil.isImage(file)) {
            String tagId = String.valueOf(ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + getFile().getRemoteId());
            resizedImage = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId);

            if (resizedImage != null && !file.isUpdateThumbnailNeeded()) {
                toolbarActivity.setPreviewImageBitmap(resizedImage);
                previewLoaded = true;
            } else {
                // show thumbnail while loading resized image
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        String.valueOf(ThumbnailsCacheManager.PREFIX_THUMBNAIL + getFile().getRemoteId()));

                if (thumbnail != null) {
                    toolbarActivity.setPreviewImageBitmap(thumbnail);
                } else {
                    thumbnail = ThumbnailsCacheManager.mDefaultImg;
                }

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
                    previewLoaded = true;
                    task.execute(getFile());
                }
            }
        } else {
            previewLoaded = false;
        }
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
            }
            else {
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
            int percent = (int)(100.0*((double)totalTransferredSoFar)/((double)totalToTransfer));
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
