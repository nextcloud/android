/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
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
package com.owncloud.android.ui.fragment;

import android.accounts.Account;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.lang.ref.WeakReference;


/**
 * This Fragment is used to display the details about a file.
 */
public class FileDetailFragment extends FileFragment implements OnClickListener {

    private int layout;
    private View view;
    private ImageView previewImage;
    private ProgressBar toolbarProgressBar;
    private boolean previewLoaded;
    private Account account;

    public ProgressListener progressListener;

    private static final String TAG = FileDetailFragment.class.getSimpleName();
    public static final String FTAG_CONFIRMATION = "REMOVE_CONFIRMATION_FRAGMENT";
    public static final String FTAG_RENAME_FILE = "RENAME_FILE_FRAGMENT";

    private static final String ARG_FILE = "FILE";
    private static final String ARG_ACCOUNT = "ACCOUNT";

    private static final String SCREEN_NAME = "File details";

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
     * Creates an empty details fragment.
     * 
     * It's necessary to keep a public constructor without parameters; the system uses it when tries
     * to reinstantiate a fragment automatically.
     */
    public FileDetailFragment() {
        super();
        account = null;
        layout = R.layout.file_details_empty;
        progressListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AnalyticsUtils.setCurrentScreenName(getActivity(), SCREEN_NAME, TAG);
        }

        if (previewImage != null && MimeTypeUtil.isImage(getFile()) && previewLoaded) {
            activatePreviewImage();
        }
    }

    private void activatePreviewImage() {
        previewImage.setVisibility(View.VISIBLE);
        toolbarProgressBar.setVisibility(View.GONE);
        ((ToolbarActivity) getActivity()).getSupportActionBar().setTitle(null);
        ((ToolbarActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(null);
        makeStatusBarBlack();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        setFile(getArguments().getParcelable(ARG_FILE));
        account = getArguments().getParcelable(ARG_ACCOUNT);

        if (savedInstanceState != null) {
            setFile(savedInstanceState.getParcelable(FileActivity.EXTRA_FILE));
            account = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }

        toolbarProgressBar = getActivity().findViewById(R.id.progressBar);

        if (getFile() != null && account != null) {
            layout = R.layout.file_details_fragment;
        }

        view = inflater.inflate(layout, null);
        
        if (layout == R.layout.file_details_fragment) {
            ProgressBar progressBar = view.findViewById(R.id.fdProgressBar);
            ThemeUtils.colorHorizontalProgressBar(progressBar, ThemeUtils.primaryAccentColor(getContext()));
            progressListener = new ProgressListener(progressBar);
            view.findViewById(R.id.fdCancelBtn).setOnClickListener(this);
            view.findViewById(R.id.fdFavorite).setOnClickListener(this);
            view.findViewById(R.id.overflow_menu).setOnClickListener(this);
            previewImage = getActivity().findViewById(R.id.preview_image);
            // TODO use whenever we switch to use glide for preview images
            /*
            if (getFile() != null && account != null && MimeTypeUtil.isImage(getFile())) {
                setHeaderImage();
            }
             */
        }

        updateFileDetails(false, false);
        return view;
    }
    // TODO use whenever we switch to use glide for preview images
    /*
    private void setHeaderImage() {
        if (mContainerActivity.getStorageManager().getCapability(account.name)
                .getServerBackground() != null && previewImage != null) {

            String background = mContainerActivity.getStorageManager().getCapability(account.name).getServerBackground();

            int primaryColor = ThemeUtils.primaryColor(account, getContext());

            if (URLUtil.isValidUrl(background)) {
                // background image
                SimpleTarget target = new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(Drawable resource, GlideAnimation glideAnimation) {
                        Drawable[] drawables = {new ColorDrawable(primaryColor), resource};
                        LayerDrawable layerDrawable = new LayerDrawable(drawables);
                        previewImage.setImageDrawable(layerDrawable);
                        previewImage.setVisibility(View.VISIBLE);
                        ((ToolbarActivity) getActivity()).getSupportActionBar().setTitle(null);
                        ((ToolbarActivity) getActivity()).getSupportActionBar().setBackgroundDrawable(null);
                        toolbarProgressBar.setVisibility(View.GONE);
                        previewLoaded = true;
                    }

                    @Override
                    public void onLoadFailed(Exception e, Drawable errorDrawable) {
                        previewImage.setVisibility(View.GONE);
                        toolbarProgressBar.setVisibility(View.VISIBLE);
                    }
                };

                Glide.with(this)
                        .load(background)
                        .centerCrop()
                        .placeholder(R.drawable.background)
                        .error(R.drawable.background)
                        .crossFade()
                        .into(target);
            } else {
                // hide image
                previewImage.setVisibility(View.GONE);
                toolbarProgressBar.setVisibility(View.VISIBLE);
            }
        }
    }
    */

    public void onOverflowIconClicked(View view) {
        PopupMenu popup = new PopupMenu(getActivity(), view);
        popup.inflate(R.menu.file_actions_menu);
        prepareOptionsMenu(popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            return optionsItemSelected(item);
        });
        popup.show();
    }

    private void setupViewPager(View view) {
        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        tabLayout.removeAllTabs();

        tabLayout.addTab(tabLayout.newTab().setText(R.string.drawer_item_activities));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.share_dialog_title));

        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
        tabLayout.setSelectedTabIndicatorColor(ThemeUtils.primaryAccentColor(getContext()));

        final ViewPager viewPager = view.findViewById(R.id.pager);
        final FileDetailTabAdapter adapter = new FileDetailTabAdapter
                (getFragmentManager(), getFile(), account);
        viewPager.setAdapter(adapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
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
        if (previewImage != null) {
            previewImage.setVisibility(View.GONE);
            toolbarProgressBar.setVisibility(View.VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getActivity().getWindow().setStatusBarColor(ThemeUtils.primaryDarkColor(getContext()));
            }
        }

        super.onStop();
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
        if (mContainerActivity.getStorageManager() != null) {
            FileMenuFilter mf = new FileMenuFilter(
                getFile(),
                mContainerActivity.getStorageManager().getAccount(),
                mContainerActivity,
                getActivity(),
                false
            );
            mf.filter(menu, true);
        }

        // restriction for this fragment
        FileMenuFilter.hideMenuItems(
                menu.findItem(R.id.action_see_details),
                menu.findItem(R.id.action_select_all),
                menu.findItem(R.id.action_move),
                menu.findItem(R.id.action_copy),
                menu.findItem(R.id.action_favorite),
                menu.findItem(R.id.action_unset_favorite),
                menu.findItem(R.id.action_search)
        );

        // dual pane restrictions
        if (!getResources().getBoolean(R.bool.large_land_layout)){
            FileMenuFilter.hideMenuItems(
                    menu.findItem(R.id.action_switch_view),
                    menu.findItem(R.id.action_sync_account),
                    menu.findItem(R.id.action_sort)
            );
        }

        // share restrictions
        if (getFile().isSharedWithMe() && !getFile().canReshare()) {
            FileMenuFilter.hideMenuItems(menu.findItem(R.id.action_send_share_file));
        }
    }

    public boolean optionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_send_share_file: {
                if(getFile().isSharedWithMe() && !getFile().canReshare()){
                    Snackbar.make(getView(),
                            R.string.resharing_is_not_allowed,
                            Snackbar.LENGTH_LONG
                    )
                            .show();
                } else {
                    mContainerActivity.getFileOperationsHelper().sendShareFile(getFile());
                }
                return true;
            }
            case R.id.action_open_file_with: {
                mContainerActivity.getFileOperationsHelper().openFile(getFile());
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
                ((FileDisplayActivity)mContainerActivity).cancelTransference(getFile());
                return true;
            }
            case R.id.action_download_file:
            case R.id.action_sync_file: {
                mContainerActivity.getFileOperationsHelper().syncFile(getFile());
                return true;
            }
            case R.id.action_keep_files_offline: {
                mContainerActivity.getFileOperationsHelper().toggleOfflineFile(getFile(), true);
                return true;
            }
            case R.id.action_unset_keep_files_offline: {
                mContainerActivity.getFileOperationsHelper().toggleOfflineFile(getFile(), false);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fdCancelBtn: {
                ((FileDisplayActivity) mContainerActivity).cancelTransference(getFile());
                break;
            }
            case R.id.fdFavorite: {
                if (getFile().getIsFavorite()) {
                    ((ImageView)getView().findViewById(R.id.fdFavorite)).
                            setImageDrawable(getResources()
                                    .getDrawable(R.drawable.ic_star_outline));
                    mContainerActivity.getFileOperationsHelper().toggleFavoriteFile(getFile(), false);
                } else {
                    ((ImageView)getView().findViewById(R.id.fdFavorite))
                            .setImageDrawable(getResources()
                                    .getDrawable(R.drawable.ic_star));
                    mContainerActivity.getFileOperationsHelper().toggleFavoriteFile(getFile(), true);
                }
                break;
            }
            case R.id.overflow_menu: {
                onOverflowIconClicked(v);
                break;
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
        return (layout == R.layout.file_details_empty || getFile() == null || account == null);
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
            FileDataStorageManager storageManager = mContainerActivity.getStorageManager();
            
            if (storageManager == null) {
                return;
            }
            
            if (refresh) {
                setFile(storageManager.getFileByPath(getFile().getRemotePath()));
            }
            OCFile file = getFile();

            // set file details
            setFilename(file.getFileName());
            setFiletype(file);
            setFilesize(file.getFileLength());
            setTimeModified(file.getModificationTimestamp());

            if (file.getIsFavorite()) {
                ((ImageView)getView().findViewById(R.id.fdFavorite)).setImageDrawable(getResources().getDrawable(R.drawable.ic_star));
            } else {
                ((ImageView)getView().findViewById(R.id.fdFavorite)).setImageDrawable(getResources().getDrawable(R.drawable.ic_star_outline));
            }

            // configure UI for depending upon local state of the file
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            if (transferring ||
                    (downloaderBinder != null && downloaderBinder.isDownloading(account, file)) ||
                    (uploaderBinder != null && uploaderBinder.isUploading(account, file))
                    ) {
                setButtonsForTransferring();

            } else if (file.isDown()) {
                
                setButtonsForDown();
                
            } else {
                // TODO load default preview image; when the local file is removed, the preview
                // remains there
                setButtonsForRemote();
            }
        }

        setupViewPager(getView());

        getView().invalidate();
    }

    /**
     * Checks if the fragment is ready to show details of a OCFile
     *
     * @return 'True' when the fragment is ready to show details of a file
     */
    private boolean readyToShow() {
        return (getFile() != null && account != null && layout == R.layout.file_details_fragment);
    }

    /**
     * Updates the filename in view
     *
     * @param filename to set
     */
    private void setFilename(String filename) {
        TextView tv = getView().findViewById(R.id.fdFilename);
        if (tv != null) {
            tv.setText(filename);
        }
    }

    /**
     * Updates the MIME type in view
     * @param file : An {@link OCFile}
     */
    private void setFiletype(OCFile file) {
        ImageView iv = getView().findViewById(R.id.fdIcon);
        View v = getView().findViewById(R.id.fdIcon_divider);

        if (iv != null) {
            iv.setTag(file.getFileId());
            // Name of the file, to deduce the icon to use in case the MIME type is not precise enough
            iv.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimetype(), file.getFileName(), account,
                    getContext()));

            Bitmap resizedImage;

            if (MimeTypeUtil.isImage(file)) {
                String tagId = String.valueOf(ThumbnailsCacheManager.PREFIX_RESIZED_IMAGE + getFile().getRemoteId());
                resizedImage = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId);

                if (resizedImage != null && !file.needsUpdateThumbnail()) {
                    iv.setVisibility(View.GONE);
                    v.setVisibility(View.GONE);
                    previewImage.setImageBitmap(resizedImage);
                    activatePreviewImage();
                    previewLoaded = true;
                } else {
                    // show thumbnail while loading resized image
                    Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                            String.valueOf(ThumbnailsCacheManager.PREFIX_THUMBNAIL + getFile().getRemoteId()));

                    if (thumbnail != null) {
                        iv.setImageBitmap(thumbnail);
                    } else {
                        thumbnail = ThumbnailsCacheManager.mDefaultImg;
                    }

                    // generate new resized image
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(getFile(), iv) &&
                            mContainerActivity.getStorageManager() != null) {
                        final ThumbnailsCacheManager.ResizedImageGenerationTask task =
                                new ThumbnailsCacheManager.ResizedImageGenerationTask(FileDetailFragment.this,
                                        iv,
                                        mContainerActivity.getStorageManager(),
                                        mContainerActivity.getStorageManager().getAccount());
                        if (resizedImage == null) {
                            resizedImage = thumbnail;
                        }
                        final ThumbnailsCacheManager.AsyncResizedImageDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncResizedImageDrawable(
                                        MainApp.getAppContext().getResources(),
                                        resizedImage,
                                        task
                                );
                        iv.setVisibility(View.GONE);
                        v.setVisibility(View.GONE);
                        previewImage.setImageDrawable(asyncDrawable);
                        activatePreviewImage();
                        previewLoaded = true;
                        task.execute(getFile());
                    }
                }
            } else {
                iv.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimetype(), file.getFileName(), account,
                        getContext()));
            }
        }
    }

    private void makeStatusBarBlack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getActivity().getWindow().setStatusBarColor(getResources().getColor(R.color.black));
        }
    }

    /**
     * Updates the file size in view
     *
     * @param fileSize in bytes to set
     */
    private void setFilesize(long fileSize) {
        TextView tv = getView().findViewById(R.id.fdSize);
        if (tv != null) {
            tv.setText(DisplayUtils.bytesToHumanReadable(fileSize));
        }
    }

    /**
     * Updates the time that the file was last modified
     *
     * @param milliseconds Unix time to set
     */
    private void setTimeModified(long milliseconds) {
        TextView tv = getView().findViewById(R.id.fdModified);
        if (tv != null) {
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
        }
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            // show the progress bar for the transfer
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.VISIBLE);
            TextView progressText = getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.VISIBLE);
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
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
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }

    public void listenForTransferProgress() {
        if (progressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().
                        addDatatransferProgressListener(progressListener, account, getFile());
            }
            if (mContainerActivity.getFileUploaderBinder() != null) {
                mContainerActivity.getFileUploaderBinder().
                        addDatatransferProgressListener(progressListener, account, getFile());
            }
        } else {
            Log_OC.d(TAG, "progressListener == null");
        }
    }

    public void leaveTransferProgress() {
        if (progressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().
                        removeDatatransferProgressListener(progressListener, account, getFile());
            }
            if (mContainerActivity.getFileUploaderBinder() != null) {
                mContainerActivity.getFileUploaderBinder().
                        removeDatatransferProgressListener(progressListener, account, getFile());
            }
        }
    }

    /**
     * Helper class responsible for updating the progress bar shown for file downloading.
     */
    private class ProgressListener implements OnDatatransferProgressListener {
        private int mLastPercent = 0;
        private WeakReference<ProgressBar> mProgressBar;

        ProgressListener(ProgressBar progressBar) {
            mProgressBar = new WeakReference<>(progressBar);
        }

        @Override
        public void onTransferProgress(long progressRate, long totalTransferredSoFar,
                                       long totalToTransfer, String filename) {
            int percent = (int)(100.0*((double)totalTransferredSoFar)/((double)totalToTransfer));
            if (percent != mLastPercent) {
                ProgressBar pb = mProgressBar.get();
                if (pb != null) {
                    pb.setProgress(percent);
                    pb.postInvalidate();
                }
            }
            mLastPercent = percent;
        }
    }
}
