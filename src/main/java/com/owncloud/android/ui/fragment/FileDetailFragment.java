/**
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
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
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
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.adapter.UserListAdapter;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.utils.AnalyticsUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;


/**
 * This Fragment is used to display the details about a file.
 */
public class FileDetailFragment extends FileFragment implements OnClickListener,
        CompoundButton.OnCheckedChangeListener {

    private int mLayout;
    private View mView;
    private Account mAccount;

    public ProgressListener mProgressListener;

    // to show share with users/groups info
    private ArrayList<OCShare> mShares;

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
        mAccount = null;
        mLayout = R.layout.file_details_empty;
        mProgressListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            AnalyticsUtils.setCurrentScreenName(getActivity(), SCREEN_NAME, TAG);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        setFile(getArguments().getParcelable(ARG_FILE));
        mAccount = getArguments().getParcelable(ARG_ACCOUNT);

        if (savedInstanceState != null) {
            setFile(savedInstanceState.getParcelable(FileActivity.EXTRA_FILE));
            mAccount = savedInstanceState.getParcelable(FileActivity.EXTRA_ACCOUNT);
        }

        if (getFile() != null && mAccount != null) {
            mLayout = R.layout.file_details_fragment;
        }

        mView = inflater.inflate(mLayout, null);
        
        if (mLayout == R.layout.file_details_fragment) {
            int accentColor = ThemeUtils.primaryAccentColor();
            SwitchCompat favoriteToggle = (SwitchCompat) mView.findViewById(R.id.fdFavorite);
            favoriteToggle.setOnCheckedChangeListener(this);
            ThemeUtils.tintSwitch(favoriteToggle, accentColor, false);
            ProgressBar progressBar = (ProgressBar)mView.findViewById(R.id.fdProgressBar);
            ThemeUtils.colorHorizontalProgressBar(progressBar, ThemeUtils.primaryAccentColor());
            mProgressListener = new ProgressListener(progressBar);
            mView.findViewById(R.id.fdCancelBtn).setOnClickListener(this);
            ((TextView)mView.findViewById(R.id.fdShareTitle)).setTextColor(accentColor);
            ((TextView)mView.findViewById(R.id.fdShareWithUsersTitle)).setTextColor(accentColor);
        }

        updateFileDetails(false, false);
        return mView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(FileActivity.EXTRA_FILE, getFile());
        outState.putParcelable(FileActivity.EXTRA_ACCOUNT, mAccount);
    }

    @Override
    public void onStart() {
        super.onStart();
        listenForTransferProgress();
    }

    @Override
    public void onStop() {
        leaveTransferProgress();
        super.onStop();
    }


    @Override
    public View getView() {
        return super.getView() == null ? mView : super.getView();
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
                getActivity(),
                false
            );
            mf.filter(menu, true);
        }

        // additional restriction for this fragment 
        MenuItem item = menu.findItem(R.id.action_see_details);
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

        // additional restriction for this fragment
        item = menu.findItem(R.id.action_search);
        if (item != null) {
            item.setVisible(false);
            item.setEnabled(false);
        }


        Boolean dualPane = getResources().getBoolean(R.bool.large_land_layout);

        item = menu.findItem(R.id.action_switch_view);
        if (item != null && !dualPane){
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_sync_account);
        if (item != null && !dualPane) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_sort);
        if (item != null && !dualPane) {
            item.setVisible(false);
            item.setEnabled(false);
        }

        item = menu.findItem(R.id.action_send_share_file);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        if(getFile().isSharedWithMe() && !getFile().canReshare()){
            // additional restriction for this fragment
            if(item != null){
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
            case R.id.action_send_share_file: {
                mContainerActivity.getFileOperationsHelper().sendShareFile(getFile(),
                        (FileDisplayActivity) mContainerActivity);
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
            default:
                Log_OC.e(TAG, "Incorrect view clicked!");
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        SwitchCompat favSwitch = (SwitchCompat) getView().findViewById(R.id.fdFavorite);
        mContainerActivity.getFileOperationsHelper().toggleOfflineFile(getFile(), favSwitch.isChecked());
    }

    /**
     * Check if the fragment was created with an empty layout. An empty fragment can't show file details, must be replaced.
     *
     * @return True when the fragment was created with the empty layout.
     */
    public boolean isEmpty() {
        return (mLayout == R.layout.file_details_empty || getFile() == null || mAccount == null);
    }


    /**
     * Use this method to signal this Activity that it shall update its view.
     *
     * @param file : An {@link OCFile}
     */
    public void updateFileDetails(OCFile file, Account ocAccount) {
        setFile(file);
        mAccount = ocAccount;
        updateFileDetails(false, false);
    }

    /**
     * Updates the view with all relevant details about that file.
     * <p/>
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
            if (refresh && storageManager != null) {
                setFile(storageManager.getFileByPath(getFile().getRemotePath()));
            }
            OCFile file = getFile();

            // set file details
            setFilename(file.getFileName());
            setFiletype(file);
            setFilesize(file.getFileLength());

            setTimeModified(file.getModificationTimestamp());
            
            SwitchCompat favSwitch = (SwitchCompat) getView().findViewById(R.id.fdFavorite);
            favSwitch.setChecked(file.isAvailableOffline());

            setShareByLinkInfo(file.isSharedViaLink());

            setShareWithUserInfo();

            // configure UI for depending upon local state of the file
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            if (transferring ||
                    (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, file)) ||
                    (uploaderBinder != null && uploaderBinder.isUploading(mAccount, file))
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
        getView().invalidate();
    }

    /**
     * Checks if the fragment is ready to show details of a OCFile
     *
     * @return 'True' when the fragment is ready to show details of a file
     */
    private boolean readyToShow() {
        return (getFile() != null && mAccount != null && mLayout == R.layout.file_details_fragment);
    }


    /**
     * Updates the filename in view
     *
     * @param filename to set
     */
    private void setFilename(String filename) {
        TextView tv = (TextView) getView().findViewById(R.id.fdFilename);
        if (tv != null) {
            tv.setText(filename);
        }
    }

    /**
     * Updates the MIME type in view
     * @param file : An {@link OCFile}
     */
    private void setFiletype(OCFile file) {
        ImageView iv = (ImageView) getView().findViewById(R.id.fdIcon);

        if (iv != null) {
            iv.setTag(file.getFileId());
            // Name of the file, to deduce the icon to use in case the MIME type is not precise enough
            iv.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimetype(), file.getFileName(), mAccount));

            Bitmap thumbnail;

            if (MimeTypeUtil.isImage(file)) {
                String tagId = String.valueOf(file.getRemoteId());
                thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(tagId);

                if (thumbnail != null && !file.needsUpdateThumbnail()) {
                    iv.setImageBitmap(thumbnail);
                } else {
                    // generate new Thumbnail
                    if (ThumbnailsCacheManager.cancelPotentialThumbnailWork(file, iv)) {
                        final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                        iv, mContainerActivity.getStorageManager(), mAccount
                                );
                        if (thumbnail == null) {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg;
                        }
                        final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                                new ThumbnailsCacheManager.AsyncThumbnailDrawable(
                                        MainApp.getAppContext().getResources(),
                                        thumbnail,
                                        task
                                );
                        iv.setImageDrawable(asyncDrawable);
                        task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, file.getRemoteId()));
                    }
                }
            } else {
                iv.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimetype(), file.getFileName(), mAccount));
			}
        }
    }

    /**
     * Updates the file size in view
     *
     * @param fileSize in bytes to set
     */
    private void setFilesize(long fileSize) {
        TextView tv = (TextView) getView().findViewById(R.id.fdSize);
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
        TextView tv = (TextView) getView().findViewById(R.id.fdModified);
        if (tv != null) {
            tv.setText(DisplayUtils.unixTimeToHumanReadable(milliseconds));
        }
    }

    /**
     * Updates Share by link data
     *
     * @param isShareByLink flag is share by link is enable
     */
    private void setShareByLinkInfo(boolean isShareByLink) {
        TextView tv = (TextView) getView().findViewById(R.id.fdSharebyLink);
        if (tv != null) {
            tv.setText(isShareByLink ? R.string.filedetails_share_link_enable :
                    R.string.filedetails_share_link_disable);
        }
        ImageView linkIcon = (ImageView) getView().findViewById(R.id.fdShareLinkIcon);
        if (linkIcon != null) {
            linkIcon.setVisibility(isShareByLink ? View.VISIBLE : View.GONE);
        }
    }

    /**
     * Update Share With data
     */
    private void setShareWithUserInfo(){
        // Get Users and Groups
        if (((FileActivity) getActivity()).getStorageManager() != null) {
            FileDataStorageManager fileDataStorageManager = ((FileActivity) getActivity()).getStorageManager();
            mShares = fileDataStorageManager.getSharesWithForAFile(
                    getFile().getRemotePath(),mAccount.name
            );

            // Update list of users/groups
            updateListOfUserGroups();
        }
    }

    private void updateListOfUserGroups() {
        // Update list of users/groups
        // TODO Refactoring: create a new {@link ShareUserListAdapter} instance with every call should not be needed
        UserListAdapter mUserGroupsAdapter = new UserListAdapter(
                getActivity().getApplicationContext(),
                R.layout.share_user_item, mShares
        );

        // Show data
        ListView usersList = (ListView) getView().findViewById(R.id.fdshareUsersList);

        // No data
        TextView noList = (TextView) getView().findViewById(R.id.fdShareNoUsers);

        if (mShares.size() > 0) {
            usersList.setVisibility(View.VISIBLE);
            usersList.setAdapter(mUserGroupsAdapter);
            noList.setVisibility(View.GONE);
            setListViewHeightBasedOnChildren(usersList);

        } else {
            usersList.setVisibility(View.GONE);
            noList.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Fix scroll in listview when the parent is a ScrollView
     */
    private static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }
        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.AT_MOST);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0) {
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
            }
            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    /**
     * Enables or disables buttons for a file being downloaded
     */
    private void setButtonsForTransferring() {
        if (!isEmpty()) {
            // let's protect the user from himself ;)
            getView().findViewById(R.id.fdFavorite).setEnabled(false);
            
            // show the progress bar for the transfer
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.VISIBLE);
            TextView progressText = (TextView) getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.VISIBLE);
            FileDownloaderBinder downloaderBinder = mContainerActivity.getFileDownloaderBinder();
            FileUploaderBinder uploaderBinder = mContainerActivity.getFileUploaderBinder();
            //if (getFile().isDownloading()) {
            if (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, getFile())) {
                progressText.setText(R.string.downloader_download_in_progress_ticker);
            }
            else {
                if (uploaderBinder != null && uploaderBinder.isUploading(mAccount, getFile())) {
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
            getView().findViewById(R.id.fdFavorite).setEnabled(true);
            
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = (TextView) getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }

    /**
     * Enables or disables buttons for a file not locally available
     */
    private void setButtonsForRemote() {
        if (!isEmpty()) {
            getView().findViewById(R.id.fdFavorite).setEnabled(true);
            
            // hides the progress bar
            getView().findViewById(R.id.fdProgressBlock).setVisibility(View.GONE);
            TextView progressText = (TextView) getView().findViewById(R.id.fdProgressText);
            progressText.setVisibility(View.GONE);
        }
    }


    public void listenForTransferProgress() {
        if (mProgressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().
                        addDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
            if (mContainerActivity.getFileUploaderBinder() != null) {
                mContainerActivity.getFileUploaderBinder().
                        addDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
        } else {
            Log_OC.d(TAG, "mProgressListener == null");
        }
    }


    public void leaveTransferProgress() {
        if (mProgressListener != null) {
            if (mContainerActivity.getFileDownloaderBinder() != null) {
                mContainerActivity.getFileDownloaderBinder().
                        removeDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
            if (mContainerActivity.getFileUploaderBinder() != null) {
                mContainerActivity.getFileUploaderBinder().
                        removeDatatransferProgressListener(mProgressListener, mAccount, getFile());
            }
        }
    }


    /**
     * Helper class responsible for updating the progress bar shown for file uploading or
     * downloading
     */
    private class ProgressListener implements OnDatatransferProgressListener {
        int mLastPercent = 0;
        WeakReference<ProgressBar> mProgressBar = null;

        ProgressListener(ProgressBar progressBar) {
            mProgressBar = new WeakReference<ProgressBar>(progressBar);
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
