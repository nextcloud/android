/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.accounts.Account;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.ConnectivityService;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.OCUploadComparator;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.Arrays;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This Adapter populates a ListView with following types of uploads: pending,active, completed. Filtering possible.
 */
public class UploadListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {
    private static final String TAG = UploadListAdapter.class.getSimpleName();

    private ProgressListener progressListener;
    private FileActivity parentActivity;
    private UploadsStorageManager uploadsStorageManager;
    private ConnectivityService connectivityService;
    private PowerManagementService powerManagementService;
    private UserAccountManager accountManager;
    private UploadGroup[] uploadGroups;
    private boolean showUser;

    @Override
    public int getSectionCount() {
        return uploadGroups.length;
    }

    @Override
    public int getItemCount(int section) {
        return uploadGroups[section].getItems().length;
    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder holder, int section, boolean expanded) {
        HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;

        UploadGroup group = uploadGroups[section];

        headerViewHolder.title.setText(String.format(parentActivity.getString(R.string.uploads_view_group_header),
                                                     group.getGroupName(), group.getGroupItemCount()));
        headerViewHolder.title.setTextColor(ThemeUtils.primaryAccentColor(parentActivity));

        headerViewHolder.title.setOnClickListener(v -> toggleSectionExpanded(section));

        switch (group.type) {
            case CURRENT:
                headerViewHolder.action.setImageResource(R.drawable.ic_close);
                break;
            case FINISHED:
                headerViewHolder.action.setImageResource(R.drawable.ic_close);
                break;
            case FAILED:
                headerViewHolder.action.setImageResource(R.drawable.ic_sync);
                break;
        }

        headerViewHolder.action.setOnClickListener(v -> {
            switch (group.type) {
                case CURRENT:
                    FileUploader.FileUploaderBinder uploaderBinder = parentActivity.getFileUploaderBinder();

                    if (uploaderBinder != null) {
                        for (OCUpload upload : group.getItems()) {
                            uploaderBinder.cancel(upload);
                        }
                    }
                    break;
                case FINISHED:
                    uploadsStorageManager.clearSuccessfulUploads();
                    break;
                case FAILED:
                    new Thread(() -> new FileUploader.UploadRequester()
                        .retryFailedUploads(
                            parentActivity,
                            null,
                            uploadsStorageManager,
                            connectivityService,
                            accountManager,
                            powerManagementService,
                            null))
                        .start();
                    break;

                default:
                    // do nothing
                    break;
            }

            loadUploadItemsFromDb();
        });
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {
        // not needed
    }

    public UploadListAdapter(final FileActivity fileActivity,
                             final UploadsStorageManager uploadsStorageManager,
                             final UserAccountManager accountManager,
                             final ConnectivityService connectivityService,
                             final PowerManagementService powerManagementService) {
        Log_OC.d(TAG, "UploadListAdapter");
        this.parentActivity = fileActivity;
        this.uploadsStorageManager = uploadsStorageManager;
        this.accountManager = accountManager;
        this.connectivityService = connectivityService;
        this.powerManagementService = powerManagementService;
        uploadGroups = new UploadGroup[3];

        shouldShowHeadersForEmptySections(false);

        uploadGroups[0] = new UploadGroup(Type.CURRENT,
                                          parentActivity.getString(R.string.uploads_view_group_current_uploads)) {
            @Override
            public void refresh() {
                fixAndSortItems(uploadsStorageManager.getCurrentAndPendingUploadsForCurrentAccount());
            }
        };

        uploadGroups[1] = new UploadGroup(Type.FAILED,
                                          parentActivity.getString(R.string.uploads_view_group_failed_uploads)) {
            @Override
            public void refresh() {
                fixAndSortItems(uploadsStorageManager.getFailedButNotDelayedUploadsForCurrentAccount());
            }
        };

        uploadGroups[2] = new UploadGroup(Type.FINISHED,
                                          parentActivity.getString(R.string.uploads_view_group_finished_uploads)) {
            @Override
            public void refresh() {
                fixAndSortItems(uploadsStorageManager.getFinishedUploadsForCurrentAccount());
            }
        };

        showUser = accountManager.getAccounts().length > 1;

        loadUploadItemsFromDb();
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {
        ItemViewHolder itemViewHolder = (ItemViewHolder) holder;

        OCUpload item = uploadGroups[section].getItem(relativePosition);

        itemViewHolder.name.setText(item.getLocalPath());

        // local file name
        File remoteFile = new File(item.getRemotePath());
        String fileName = remoteFile.getName();
        if (fileName.length() == 0) {
            fileName = File.separator;
        }
        itemViewHolder.name.setText(fileName);

        // remote path to parent folder
        itemViewHolder.remotePath.setText(new File(item.getRemotePath()).getParent());

        // file size
        if (item.getFileSize() != 0) {
            itemViewHolder.fileSize.setText(String.format("%s, ",
                    DisplayUtils.bytesToHumanReadable(item.getFileSize())));
        } else {
            itemViewHolder.fileSize.setText("");
        }

        // upload date
        long updateTime = item.getUploadEndTimestamp();
        CharSequence dateString = DisplayUtils.getRelativeDateTimeString(parentActivity,
                                                                         updateTime,
                                                                         DateUtils.SECOND_IN_MILLIS,
                                                                         DateUtils.WEEK_IN_MILLIS, 0);
        itemViewHolder.date.setText(dateString);

        // account
        Account account = accountManager.getAccountByName(item.getAccountName());
        if (showUser) {
            itemViewHolder.account.setVisibility(View.VISIBLE);
            if (account != null) {
                itemViewHolder.account.setText(DisplayUtils.getAccountNameDisplayText(parentActivity,
                                                                                      account,
                                                                                      account.name,
                                                                                      item.getAccountName()));
            } else {
                itemViewHolder.account.setText(item.getAccountName());
            }
        } else {
            itemViewHolder.account.setVisibility(View.GONE);
        }

        // Reset fields visibility
        itemViewHolder.date.setVisibility(View.VISIBLE);
        itemViewHolder.remotePath.setVisibility(View.VISIBLE);
        itemViewHolder.fileSize.setVisibility(View.VISIBLE);
        itemViewHolder.status.setVisibility(View.VISIBLE);
        itemViewHolder.progressBar.setVisibility(View.GONE);

        // Update information depending of upload details
        String status = getStatusText(item);
        switch (item.getUploadStatus()) {
            case UPLOAD_IN_PROGRESS:
                ThemeUtils.colorHorizontalProgressBar(itemViewHolder.progressBar,
                                                      ThemeUtils.primaryAccentColor(parentActivity));
                itemViewHolder.progressBar.setProgress(0);
                itemViewHolder.progressBar.setVisibility(View.VISIBLE);

                FileUploader.FileUploaderBinder binder = parentActivity.getFileUploaderBinder();
                if (binder != null) {
                    if (binder.isUploadingNow(item)) {
                        // really uploading, so...
                        // ... unbind the old progress bar, if any; ...
                        if (progressListener != null) {
                            binder.removeDatatransferProgressListener(
                                progressListener,
                                progressListener.getUpload()   // the one that was added
                            );
                        }
                        // ... then, bind the current progress bar to listen for updates
                        progressListener = new ProgressListener(item, itemViewHolder.progressBar);
                        binder.addDatatransferProgressListener(progressListener, item);
                    } else {
                        // not really uploading; stop listening progress if view is reused!
                        if (progressListener != null && progressListener.isWrapping(itemViewHolder.progressBar)) {
                            binder.removeDatatransferProgressListener(progressListener,
                                                                      progressListener.getUpload()   // the one that was added
                            );
                            progressListener = null;
                        }
                    }
                } else {
                    Log_OC.w(TAG, "FileUploaderBinder not ready yet for upload " + item.getRemotePath());
                }

                itemViewHolder.date.setVisibility(View.GONE);
                itemViewHolder.fileSize.setVisibility(View.GONE);
                itemViewHolder.progressBar.invalidate();
                break;

            case UPLOAD_FAILED:
                itemViewHolder.date.setVisibility(View.GONE);
                break;

            case UPLOAD_SUCCEEDED:
                itemViewHolder.status.setVisibility(View.GONE);
                break;
        }
        itemViewHolder.status.setText(status);

        // bind listeners to perform actions
        if (item.getUploadStatus() == UploadStatus.UPLOAD_IN_PROGRESS) {
            // Cancel
            itemViewHolder.button.setImageResource(R.drawable.ic_action_cancel_grey);
            itemViewHolder.button.setVisibility(View.VISIBLE);
            itemViewHolder.button.setOnClickListener(v -> {
                FileUploader.FileUploaderBinder uploaderBinder = parentActivity.getFileUploaderBinder();
                if (uploaderBinder != null) {
                    uploaderBinder.cancel(item);
                    loadUploadItemsFromDb();
                }
            });

        } else if (item.getUploadStatus() == UploadStatus.UPLOAD_FAILED) {
            // Delete
            itemViewHolder.button.setImageResource(R.drawable.ic_action_delete_grey);
            itemViewHolder.button.setVisibility(View.VISIBLE);
            itemViewHolder.button.setOnClickListener(v -> {
                uploadsStorageManager.removeUpload(item);
                loadUploadItemsFromDb();
            });

        } else {    // UploadStatus.UPLOAD_SUCCESS
            itemViewHolder.button.setVisibility(View.INVISIBLE);
        }

        itemViewHolder.itemLayout.setOnClickListener(null);

        // click on item
        if (item.getUploadStatus() == UploadStatus.UPLOAD_FAILED) {
            if (UploadResult.CREDENTIAL_ERROR == item.getLastResult()) {
                itemViewHolder.itemLayout.setOnClickListener(v ->
                                                                 parentActivity.getFileOperationsHelper().checkCurrentCredentials(
                                                                     item.getAccount(accountManager)));
            } else {
                // not a credentials error
                itemViewHolder.itemLayout.setOnClickListener(v -> {
                    File file = new File(item.getLocalPath());
                    if (file.exists()) {
                        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
                        requester.retry(parentActivity, accountManager, item);
                        loadUploadItemsFromDb();
                    } else {
                        DisplayUtils.showSnackMessage(
                                v.getRootView().findViewById(android.R.id.content),
                                R.string.local_file_not_found_message
                        );
                    }
                });
            }
        } else {
            itemViewHolder.itemLayout.setOnClickListener(v ->
                    onUploadItemClick(item));
        }

        // Set icon or thumbnail
        itemViewHolder.thumbnail.setImageResource(R.drawable.file);

        /*
         * Cancellation needs do be checked and done before changing the drawable in fileIcon, or
         * {@link ThumbnailsCacheManager#cancelPotentialWork} will NEVER cancel any task.
         */
        OCFile fakeFileToCheatThumbnailsCacheManagerInterface = new OCFile(item.getRemotePath());
        fakeFileToCheatThumbnailsCacheManagerInterface.setStoragePath(item.getLocalPath());
        fakeFileToCheatThumbnailsCacheManagerInterface.setMimeType(item.getMimeType());

        boolean allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(
                fakeFileToCheatThumbnailsCacheManagerInterface, itemViewHolder.thumbnail
        );

        // TODO this code is duplicated; refactor to a common place
        if (MimeTypeUtil.isImage(fakeFileToCheatThumbnailsCacheManagerInterface)
                && fakeFileToCheatThumbnailsCacheManagerInterface.getRemoteId() != null &&
                item.getUploadStatus() == UploadStatus.UPLOAD_SUCCEEDED) {
            // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf(fakeFileToCheatThumbnailsCacheManagerInterface.getRemoteId())
            );
            if (thumbnail != null && !fakeFileToCheatThumbnailsCacheManagerInterface.isUpdateThumbnailNeeded()) {
                itemViewHolder.thumbnail.setImageBitmap(thumbnail);
            } else {
                // generate new Thumbnail
                if (allowedToCreateNewThumbnail) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                itemViewHolder.thumbnail, parentActivity.getStorageManager(), parentActivity.getAccount()
                            );
                    if (thumbnail == null) {
                        if (MimeTypeUtil.isVideo(fakeFileToCheatThumbnailsCacheManagerInterface)) {
                            thumbnail = ThumbnailsCacheManager.mDefaultVideo;
                        } else {
                            thumbnail = ThumbnailsCacheManager.mDefaultImg;
                        }
                    }
                    final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                            new ThumbnailsCacheManager.AsyncThumbnailDrawable(
                                parentActivity.getResources(),
                                thumbnail,
                                task
                            );
                    itemViewHolder.thumbnail.setImageDrawable(asyncDrawable);
                    task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(
                            fakeFileToCheatThumbnailsCacheManagerInterface, null));
                }
            }

            if ("image/png".equals(item.getMimeType())) {
                itemViewHolder.thumbnail.setBackgroundColor(parentActivity.getResources()
                        .getColor(R.color.bg_default));
            }


        } else if (MimeTypeUtil.isImage(fakeFileToCheatThumbnailsCacheManagerInterface)) {
            File file = new File(item.getLocalPath());
            // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf(file.hashCode()));
            if (thumbnail != null) {
                itemViewHolder.thumbnail.setImageBitmap(thumbnail);
            } else {
                // generate new Thumbnail
                if (allowedToCreateNewThumbnail) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(itemViewHolder.thumbnail);

                    if (MimeTypeUtil.isVideo(file)) {
                        thumbnail = ThumbnailsCacheManager.mDefaultVideo;
                    } else {
                        thumbnail = ThumbnailsCacheManager.mDefaultImg;
                    }

                    final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                        new ThumbnailsCacheManager.AsyncThumbnailDrawable(parentActivity.getResources(), thumbnail,
                                                                          task);

                    itemViewHolder.thumbnail.setImageDrawable(asyncDrawable);
                    task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, null));
                    Log_OC.v(TAG, "Executing task to generate a new thumbnail");
                }
            }

            if ("image/png".equalsIgnoreCase(item.getMimeType())) {
                itemViewHolder.thumbnail.setBackgroundColor(parentActivity.getResources()
                        .getColor(R.color.bg_default));
            }
        } else {
            itemViewHolder.thumbnail.setImageDrawable(MimeTypeUtil.getFileTypeIcon(item.getMimeType(), fileName,
                                                                                   account, parentActivity));
        }
    }

    /**
     * Gets the status text to show to the user according to the status and last result of the
     * the given upload.
     *
     * @param upload Upload to describe.
     * @return Text describing the status of the given upload.
     */
    private String getStatusText(OCUpload upload) {

        String status;
        switch (upload.getUploadStatus()) {

            case UPLOAD_IN_PROGRESS:
                status = parentActivity.getString(R.string.uploads_view_later_waiting_to_upload);
                FileUploader.FileUploaderBinder binder = parentActivity.getFileUploaderBinder();
                if (binder != null && binder.isUploadingNow(upload)) {
                    // really uploading, bind the progress bar to listen for progress updates
                    status = parentActivity.getString(R.string.uploader_upload_in_progress_ticker);
                }
                break;

            case UPLOAD_SUCCEEDED:
                status = parentActivity.getString(R.string.uploads_view_upload_status_succeeded);
                break;

            case UPLOAD_FAILED:
                status = getUploadFailedStatusText(upload.getLastResult());
                break;

            default:
                status = "Uncontrolled status: " + upload.getUploadStatus().toString();
        }
        return status;
    }

    @NonNull
    private String getUploadFailedStatusText(UploadResult result) {
        String status;
        switch (result) {
            case CREDENTIAL_ERROR:
                status = parentActivity.getString(R.string.uploads_view_upload_status_failed_credentials_error);
                break;
            case FOLDER_ERROR:
                status = parentActivity.getString(R.string.uploads_view_upload_status_failed_folder_error);
                break;
            case FILE_NOT_FOUND:
                status = parentActivity.getString(R.string.uploads_view_upload_status_failed_localfile_error);
                break;
            case FILE_ERROR:
                status = parentActivity.getString(R.string.uploads_view_upload_status_failed_file_error);
                break;
            case PRIVILEGES_ERROR:
                status = parentActivity.getString(R.string.uploads_view_upload_status_failed_permission_error);
                break;
            case NETWORK_CONNECTION:
                status = parentActivity.getString(R.string.uploads_view_upload_status_failed_connection_error);
                break;
            case DELAYED_FOR_WIFI:
                status = parentActivity.getString(R.string.uploads_view_upload_status_waiting_for_wifi);
                break;
            case DELAYED_FOR_CHARGING:
                status = parentActivity.getString(R.string.uploads_view_upload_status_waiting_for_charging);
                break;
            case CONFLICT_ERROR:
                status = parentActivity.getString(R.string.uploads_view_upload_status_conflict);
                break;
            case SERVICE_INTERRUPTED:
                status = parentActivity.getString(R.string.uploads_view_upload_status_service_interrupted);
                break;
            case CANCELLED:
                // should not get here ; cancelled uploads should be wiped out
                status = parentActivity.getString(R.string.uploads_view_upload_status_cancelled);
                break;
            case UPLOADED:
                // should not get here ; status should be UPLOAD_SUCCESS
                status = parentActivity.getString(R.string.uploads_view_upload_status_succeeded);
                break;
            case MAINTENANCE_MODE:
                status = parentActivity.getString(R.string.maintenance_mode);
                break;
            case SSL_RECOVERABLE_PEER_UNVERIFIED:
                status =
                    parentActivity.getString(
                                R.string.uploads_view_upload_status_failed_ssl_certificate_not_trusted
                        );
                break;
            case UNKNOWN:
                status = parentActivity.getString(R.string.uploads_view_upload_status_unknown_fail);
                break;
            case LOCK_FAILED:
                status = parentActivity.getString(R.string.upload_lock_failed);
                break;
            case DELAYED_IN_POWER_SAVE_MODE:
                status = parentActivity.getString(
                        R.string.uploads_view_upload_status_waiting_exit_power_save_mode);
                break;
            case VIRUS_DETECTED:
                status = parentActivity.getString(R.string.uploads_view_upload_status_virus_detected);
                break;
            case LOCAL_STORAGE_FULL:
                status = parentActivity.getString(R.string.upload_local_storage_full);
                break;
            case OLD_ANDROID_API:
                status = parentActivity.getString(R.string.upload_old_android);
                break;
            case SYNC_CONFLICT:
                status = parentActivity.getString(R.string.upload_sync_conflict);
                break;
            case CANNOT_CREATE_FILE:
                status = parentActivity.getString(R.string.upload_cannot_create_file);
                break;
            case LOCAL_STORAGE_NOT_COPIED:
                status = parentActivity.getString(R.string.upload_local_storage_not_copied);
                break;
            default:
                status = parentActivity.getString(R.string.upload_unknown_error);
                break;
        }

        return status;
    }

    @Override
    @NonNull
    public SectionedViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            return new HeaderViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.upload_list_header, parent, false));
        } else {
            return new ItemViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.upload_list_item, parent, false));
        }
    }

    /**
     * Load upload items from {@link UploadsStorageManager}.
     */
    public void loadUploadItemsFromDb() {
        Log_OC.d(TAG, "loadUploadItemsFromDb");

        for (UploadGroup group : uploadGroups) {
            group.refresh();
        }

        notifyDataSetChanged();
    }

    private void onUploadItemClick(OCUpload file) {
        File f = new File(file.getLocalPath());
        if (!f.exists()) {
            DisplayUtils.showSnackMessage(parentActivity, R.string.local_file_not_found_message);
        } else {
            openFileWithDefault(file.getLocalPath());
        }
    }


    /**
     * Open file with app associates with its MIME type. If MIME type unknown, show list with all apps.
     */
    private void openFileWithDefault(String localPath) {
        Intent myIntent = new Intent(Intent.ACTION_VIEW);
        File file = new File(localPath);
        String mimetype = MimeTypeUtil.getBestMimeTypeByFilename(localPath);
        if ("application/octet-stream".equals(mimetype)) {
            mimetype = "*/*";
        }
        myIntent.setDataAndType(Uri.fromFile(file), mimetype);
        try {
            parentActivity.startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            DisplayUtils.showSnackMessage(parentActivity, R.string.file_list_no_app_for_file_type);
            Log_OC.i(TAG, "Could not find app for sending log history.");
        }
    }

    static class HeaderViewHolder extends SectionedViewHolder {
        @BindView(R.id.upload_list_title)
        TextView title;

        @BindView(R.id.upload_list_action)
        ImageView action;

        HeaderViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    static class ItemViewHolder extends SectionedViewHolder {
        @BindView(R.id.upload_name)
        public TextView name;

        @BindView(R.id.thumbnail)
        public ImageView thumbnail;

        @BindView(R.id.upload_file_size)
        public TextView fileSize;

        @BindView(R.id.upload_date)
        public TextView date;

        @BindView(R.id.upload_status)
        public TextView status;

        @BindView(R.id.upload_account)
        public TextView account;

        @BindView(R.id.upload_remote_path)
        public TextView remotePath;

        @BindView(R.id.upload_progress_bar)
        public ProgressBar progressBar;

        @BindView(R.id.upload_right_button)
        public  ImageButton button;

        @BindView(R.id.upload_list_item_layout)
        public LinearLayout itemLayout;

        ItemViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    interface Refresh {
        void refresh();
    }

    enum Type {
        CURRENT, FINISHED, FAILED
    }

    abstract class UploadGroup implements Refresh {
        private Type type;
        private OCUpload[] items;
        private String name;

        UploadGroup(Type type, String groupName) {
            this.type = type;
            this.name = groupName;
            items = new OCUpload[0];
        }

        private String getGroupName() {
            return name;
        }

        public OCUpload[] getItems() {
            return items;
        }

        public OCUpload getItem(int position) {
            return items[position];
        }

        public void setItems(OCUpload... items) {
            this.items = items;
        }

        void fixAndSortItems(OCUpload... array) {
            FileUploader.FileUploaderBinder binder = parentActivity.getFileUploaderBinder();

            for (OCUpload upload : array) {
                upload.setDataFixed(binder);
            }
            Arrays.sort(array, new OCUploadComparator());

            setItems(array);
        }

        private int getGroupItemCount() {
            return items == null ? 0 : items.length;
        }
    }
}
