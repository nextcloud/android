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

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.material.tabs.TabLayout;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
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
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * This Fragment is used to display the details about a file.
 */
public class FileDetailFragment extends FileFragment implements OnClickListener, Injectable {
    private static final String TAG = FileDetailFragment.class.getSimpleName();
    private static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";
    static final String FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";
    private static final String ARG_ACTIVE_TAB = "TAB";

    @BindView(R.id.detail_container)
    LinearLayout detailContainer;

    @BindView(R.id.progressBlock)
    View downloadProgressContainer;

    @BindView(R.id.cancelBtn)
    ImageButton cancelButton;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.progressText)
    TextView progressText;

    @BindView(R.id.filename)
    TextView fileName;

    @BindView(R.id.size)
    TextView fileSize;

    @BindView(R.id.last_modification_timestamp)
    TextView fileModifiedTimestamp;

    @BindView(R.id.favorite)
    ImageView favoriteIcon;

    @BindView(R.id.overflow_menu)
    ImageView overflowMenu;

    @BindView(R.id.tab_layout)
    TabLayout tabLayout;

    @BindView(R.id.pager)
    ViewPager viewPager;

    @BindView(R.id.empty_list_view)
    public LinearLayout emptyContentContainer;

    @BindView(R.id.empty_list_view_headline)
    public TextView emptyContentHeadline;

    @BindView(R.id.empty_list_icon)
    public ImageView emptyContentIcon;

    @BindView(R.id.empty_list_progress)
    public ProgressBar emptyContentProgressBar;

    private int layout;
    private View view;
    private boolean previewLoaded;
    private Account account;
    private Unbinder unbinder;

    private ProgressListener progressListener;
    private ToolbarActivity activity;
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
     * @param account           An ownCloud account; needed to start downloads
     * @return                  New fragment with arguments set
     */
    public static FileDetailFragment newInstance(OCFile fileToDetail, Account account) {
        FileDetailFragment frag = new FileDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToDetail);
        args.putParcelable(ARG_ACCOUNT, account);
        frag.setArguments(args);
        return frag;
    }

    /**
     * Public factory method to create new FileDetailFragment instances.
     *
     * When 'fileToDetail' or 'ocAccount' are null, creates a dummy layout (to use when a file wasn't tapped before).
     *
     * @param fileToDetail      An {@link OCFile} to show in the fragment
     * @param account           An ownCloud account; needed to start downloads
     *                          @param activeTab to be active tab
     * @return New fragment with arguments set
     */
    public static FileDetailFragment newInstance(OCFile fileToDetail, Account account, int activeTab) {
        FileDetailFragment frag = new FileDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_FILE, fileToDetail);
        args.putParcelable(ARG_ACCOUNT, account);
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
        account = null;
        layout = R.layout.file_details_fragment;
        progressListener = null;
    }

    /**
     * return the reference to the file detail sharing fragment to communicate with it.
     *
     * @return reference to the {@link FileDetailSharingFragment}
     */
    public FileDetailSharingFragment getFileDetailSharingFragment() {
        return ((FileDetailTabAdapter)viewPager.getAdapter()).getFileDetailSharingFragment();
    }

    /**
     * return the reference to the file detail activity fragment to communicate with it.
     *
     * @return reference to the {@link FileDetailActivitiesFragment}
     */
    public FileDetailActivitiesFragment getFileDetailActivitiesFragment() {
        return ((FileDetailTabAdapter) viewPager.getAdapter()).getFileDetailActivitiesFragment();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (previewLoaded && getFile() != null && MimeTypeUtil.isImage(getFile())) {
            activatePreviewImage();
        }
    }

    private void activatePreviewImage() {
        if (activity != null) {
            activity.setPreviewImageVisibility(View.VISIBLE);
            activity.setProgressBarVisibility(View.GONE);
            ThemeUtils.setStatusBarColor(activity, activity.getResources().getColor(R.color.background_color_inverse));
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle(null);
                activity.getSupportActionBar().setBackgroundDrawable(null);
            }
        }
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
        account = arguments.getParcelable(ARG_ACCOUNT);
        activeTab = arguments.getInt(ARG_ACTIVE_TAB, 0);

        if (savedInstanceState != null) {
            setFile(savedInstanceState.getParcelable(FileActivity.EXTRA_FILE));
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }

        view = inflater.inflate(layout, null);
        unbinder = ButterKnife.bind(this, view);

        if (getFile() == null || account == null) {
            showEmptyContent();
        } else {
            emptyContentContainer.setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getFile() != null && account != null) {
            ThemeUtils.colorHorizontalProgressBar(progressBar, ThemeUtils.primaryAccentColor(getContext()));
            progressListener = new ProgressListener(progressBar);
            cancelButton.setOnClickListener(this);
            favoriteIcon.setOnClickListener(this);
            overflowMenu.setOnClickListener(this);
            fileModifiedTimestamp.setOnClickListener(this);

            updateFileDetails(false, false);
        }
    }

    private void onOverflowIconClicked(View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.inflate(R.menu.file_details_actions_menu);
        prepareOptionsMenu(popup.getMenu());

        popup.setOnMenuItemClickListener(this::optionsItemSelected);
        popup.show();
    }

    private void setupViewPager() {
        tabLayout.removeAllTabs();

        tabLayout.addTab(tabLayout.newTab().setText(R.string.drawer_item_activities));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.share_dialog_title));

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setSelectedTabIndicatorColor(ThemeUtils.primaryAccentColor(getContext()));

        final FileDetailTabAdapter adapter = new FileDetailTabAdapter(getFragmentManager(), getFile(), account);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout) {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (activeTab == 0) {
                    getFileDetailActivitiesFragment().markCommentsAsRead();
                }

                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
            }
        });
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());

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

        tabLayout.getTabAt(activeTab).select();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, getFile());
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, account);
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForTransferProgress();
    }

    @Override
    public void onStop() {
        leaveTransferProgress();

        if(activity != null) {
            activity.setPreviewImageVisibility(View.GONE);
            activity.setProgressBarVisibility(View.VISIBLE);
            activity.setupToolbar();
        }

        super.onStop();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ToolbarActivity) {
            activity = (ToolbarActivity) context;
        } else {
            activity = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public View getView() {
        return super.getView() == null ? view : super.getView();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        FileMenuFilter.hideAll(menu);
    }

    private void prepareOptionsMenu(Menu menu) {
        if (containerActivity.getStorageManager() != null) {
            Account currentAccount = containerActivity.getStorageManager().getAccount();
            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                currentAccount,
                containerActivity,
                getActivity(),
                false
            );

            mf.filter(menu,
                      true,
                      accountManager.isMediaStreamingSupported(currentAccount));
        }

        if (getFile().isFolder()) {
            FileMenuFilter.hideMenuItems(menu.findItem(R.id.action_send_file));
            FileMenuFilter.hideMenuItems(menu.findItem(R.id.action_sync_file));
        }

        // dual pane restrictions
        if (!getResources().getBoolean(R.bool.large_land_layout)){
            FileMenuFilter.hideMenuItems(menu.findItem(R.id.action_sync_account));
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
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
     *
     * @return True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return layout == R.layout.empty_list || getFile() == null || account == null;
    }

    /**
     * Use this method to signal this Activity that it shall update its view.
     *
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, Account ocAccount) {
        setFile(file);
        account = ocAccount;
        updateFileDetails(false, false);
    }

    /**
     * Updates the view with all relevant details about that file.
     *
     * TODO Remove parameter when the transferring state of files is kept in database.
     *
     * @param transferring Flag signaling if the file should be considered as downloading or uploading,
     *                     although {@link FileDownloaderBinder#isDownloading(Account, OCFile)}  and
     *                     {@link FileUploaderBinder#isUploading(Account, OCFile)} return false.
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
                fileName.setText(file.getFileName());
            } else {
                fileName.setVisibility(View.GONE);
            }
            fileSize.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));

            boolean showDetailedTimestamp = preferences.isShowDetailedTimestampEnabled();
            setFileModificationTimestamp(file, showDetailedTimestamp);

            setFilePreview(file);
            setFavoriteIconStatus(file.isFavorite());

            // configure UI for depending upon local state of the file
            FileDownloaderBinder downloaderBinder = containerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = containerActivity.getFileUploaderBinder();
            if (transferring
                    || (downloaderBinder != null && downloaderBinder.isDownloading(account, file))
                    || (uploaderBinder != null && uploaderBinder.isUploading(account, file))) {
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
            fileModifiedTimestamp.setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
        } else {
            fileModifiedTimestamp.setText(DisplayUtils.getRelativeTimestamp(getContext(),
                                                                            file.getModificationTimestamp()));
        }
    }

    private void setFavoriteIconStatus(boolean isFavorite) {
        if (isFavorite) {
            favoriteIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_star));
        } else {
            favoriteIcon.setImageDrawable(getResources().getDrawable(R.drawable.ic_star_outline));
        }
    }

    /**
     * Checks if the fragment is ready to show details of a OCFile
     *
     * @return 'True' when the fragment is ready to show details of a file
     */
    private boolean readyToShow() {
        return getFile() != null && account != null && layout == R.layout.file_details_fragment;
    }

    /**
     * Updates the file preview if possible
     *
     * @param file a {@link OCFile} to be previewed
     */
    private void setFilePreview(OCFile file) {
        Bitmap resizedImage;

        if (activity != null && MimeTypeUtil.isImage(file)) {
            String tagId = String.valueOf(ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + getFile().getRemoteId());
            resizedImage = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId);

            if (resizedImage != null && !file.isUpdateThumbnailNeeded()) {
                activity.setPreviewImageBitmap(resizedImage);
                activatePreviewImage();
                previewLoaded = true;
            } else {
                // show thumbnail while loading resized image
                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                        String.valueOf(ThumbnailsCacheManager.PREFIX_THUMBNAIL + getFile().getRemoteId()));

                if (thumbnail != null) {
                    activity.setPreviewImageBitmap(thumbnail);
                } else {
                    thumbnail = ThumbnailsCacheManager.mDefaultImg;
                }

                // generate new resized image
                if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(getFile(), activity.getPreviewImageView()) &&
                        containerActivity.getStorageManager() != null) {
                    final ThumbnailsCacheManager.ResizedImageGenerationTask task =
                            new ThumbnailsCacheManager.ResizedImageGenerationTask(this,
                                    activity.getPreviewImageView(),
                                    containerActivity.getStorageManager(),
                                    connectivityService,
                                    containerActivity.getStorageManager().getAccount());

                    if (resizedImage == null) {
                        resizedImage = thumbnail;
                    }

                    final ThumbnailsCacheManager.AsyncResizedImageDrawable asyncDrawable =
                            new ThumbnailsCacheManager.AsyncResizedImageDrawable(
                                    MainApp.getAppContext().getResources(),
                                    resizedImage,
                                    task
                            );

                    activity.setPreviewImageDrawable(asyncDrawable);
                    activatePreviewImage();
                    previewLoaded = true;
                    task.execute(getFile());
                }
            }
        }
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            // show the progress bar for the transfer
            downloadProgressContainer.setVisibility(View.VISIBLE);
            progressText.setVisibility(View.VISIBLE);
            FileDownloaderBinder downloaderBinder = containerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = containerActivity.getFileUploaderBinder();
            //if (getFile().isDownloading()) {
            if (downloaderBinder != null && downloaderBinder.isDownloading(account, getFile())) {
                progressText.setText(R.string.downloader_download_in_progress_ticker);
            }
            else {
                if (uploaderBinder != null && uploaderBinder.isUploading(account, getFile())) {
                    progressText.setText(R.string.uploader_upload_in_progress_ticker);
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
            downloadProgressContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            // hides the progress bar
            downloadProgressContainer.setVisibility(View.GONE);
        }
    }

    public void listenForTransferProgress() {
        if (progressListener != null) {
            if (containerActivity.getFileDownloaderBinder() != null) {
                containerActivity.getFileDownloaderBinder().
                        addDatatransferProgressListener(progressListener, account, getFile());
            }
            if (containerActivity.getFileUploaderBinder() != null) {
                containerActivity.getFileUploaderBinder().
                        addDatatransferProgressListener(progressListener, account, getFile());
            }
        } else {
            Log_OC.d(TAG, "progressListener == null");
        }
    }

    private void leaveTransferProgress() {
        if (progressListener != null) {
            if (containerActivity.getFileDownloaderBinder() != null) {
                containerActivity.getFileDownloaderBinder().
                        removeDatatransferProgressListener(progressListener, account, getFile());
            }
            if (containerActivity.getFileUploaderBinder() != null) {
                containerActivity.getFileUploaderBinder().
                        removeDatatransferProgressListener(progressListener, account, getFile());
            }
        }
    }

    private void showEmptyContent() {
        if (emptyContentContainer != null) {
            emptyContentContainer.setVisibility(View.VISIBLE);
            detailContainer.setVisibility(View.GONE);

            emptyContentHeadline.setText(R.string.file_details_no_content);

            emptyContentProgressBar.setVisibility(View.GONE);
            emptyContentIcon.setImageResource(R.drawable.ic_list_empty_error);
            emptyContentIcon.setVisibility(View.VISIBLE);
        }
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
