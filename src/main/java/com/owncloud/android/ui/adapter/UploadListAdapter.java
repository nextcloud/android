/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud
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
import android.support.design.widget.Snackbar;
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
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * This Adapter populates a ListView with following types of uploads: pending,
 * active, completed. Filtering possible.
 */
public class UploadListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {

    private static final String TAG = UploadListAdapter.class.getSimpleName();

    private ProgressListener mProgressListener;

    private FileActivity mParentActivity;

    private UploadsStorageManager mUploadsStorageManager;

    private UploadGroup[] mUploadGroups = null;

    @Override
    public int getSectionCount() {
        return mUploadGroups.length;
    }

    @Override
    public int getItemCount(int section) {
        return mUploadGroups[section].getItems().length;

    }

    @Override
    public void onBindHeaderViewHolder(SectionedViewHolder holder, int section, boolean expanded) {
        HeaderViewHolder headerViewHolder = (HeaderViewHolder) holder;

        UploadGroup group = mUploadGroups[section];

        headerViewHolder.title.setText(String.format(mParentActivity.getString(R.string.uploads_view_group_header),
                group.getGroupName(), group.getGroupItemCount()));
        headerViewHolder.title.setTextColor(ThemeUtils.primaryAccentColor());
    }

    @Override
    public void onBindFooterViewHolder(SectionedViewHolder holder, int section) {

    }

    public UploadListAdapter(FileActivity parentActivity) {
        Log_OC.d(TAG, "UploadListAdapter");
        mParentActivity = parentActivity;
        mUploadsStorageManager = new UploadsStorageManager(mParentActivity.getContentResolver(),
                parentActivity.getApplicationContext());
        mUploadGroups = new UploadGroup[3];

        shouldShowHeadersForEmptySections(false);

        mUploadGroups[0] = new UploadGroup(mParentActivity.getString(R.string.uploads_view_group_current_uploads)) {
            @Override
            public void refresh() {
                setItems(mUploadsStorageManager.getCurrentAndPendingUploadsForCurrentAccount());
                Arrays.sort(getItems(), comparator);
            }
        };

        mUploadGroups[1] = new UploadGroup(mParentActivity.getString(R.string.uploads_view_group_failed_uploads)) {
            @Override
            public void refresh() {
                setItems(mUploadsStorageManager.getFailedButNotDelayedUploadsForCurrentAccount());
                Arrays.sort(getItems(), comparator);
            }
        };

        mUploadGroups[2] = new UploadGroup(mParentActivity.getString(R.string.uploads_view_group_finished_uploads)) {
            @Override
            public void refresh() {
                setItems(mUploadsStorageManager.getFinishedUploadsForCurrentAccount());
                Arrays.sort(getItems(), comparator);
            }
        };
        loadUploadItemsFromDb();
    }

    @Override
    public void onBindViewHolder(SectionedViewHolder holder, int section, int relativePosition, int absolutePosition) {
        ItemViewHolder itemViewHolder = (ItemViewHolder) holder;

        OCUpload item = mUploadGroups[section].getItem(relativePosition);

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

        //* upload date
        long updateTime = item.getUploadEndTimestamp();
        CharSequence dateString = DisplayUtils.getRelativeDateTimeString(mParentActivity, updateTime,
                DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
        itemViewHolder.date.setText(dateString);

        Account account = AccountUtils.getOwnCloudAccountByName(mParentActivity, item.getAccountName());
        if (account != null) {
            itemViewHolder.account.setText(DisplayUtils.getAccountNameDisplayText(mParentActivity, account,
                    account.name, item.getAccountName()));
        } else {
            itemViewHolder.account.setText(item.getAccountName());
        }

        // Reset fields visibility
        itemViewHolder.date.setVisibility(View.VISIBLE);
        itemViewHolder.remotePath.setVisibility(View.VISIBLE);

        itemViewHolder.fileSize.setVisibility(View.VISIBLE);
        itemViewHolder.account.setVisibility(View.VISIBLE);
        itemViewHolder.status.setVisibility(View.VISIBLE);
        itemViewHolder.progressBar.setVisibility(View.GONE);

        // Update information depending of upload details
        String status = getStatusText(item);
        switch (item.getUploadStatus()) {
            case UPLOAD_IN_PROGRESS:
                ThemeUtils.colorHorizontalProgressBar(itemViewHolder.progressBar, ThemeUtils.primaryAccentColor());
                itemViewHolder.progressBar.setProgress(0);
                itemViewHolder.progressBar.setVisibility(View.VISIBLE);

                FileUploader.FileUploaderBinder binder = mParentActivity.getFileUploaderBinder();
                if (binder != null) {
                    if (binder.isUploadingNow(item)) {
                        // really uploading, so...
                        // ... unbind the old progress bar, if any; ...
                        if (mProgressListener != null) {
                            binder.removeDatatransferProgressListener(
                                    mProgressListener,
                                    mProgressListener.getUpload()   // the one that was added
                            );
                        }
                        // ... then, bind the current progress bar to listen for updates
                        mProgressListener = new ProgressListener(item, itemViewHolder.progressBar);
                        binder.addDatatransferProgressListener(mProgressListener, item);
                    } else {
                        // not really uploading; stop listening progress if view is reused!
                        if (mProgressListener != null && mProgressListener.isWrapping(itemViewHolder.progressBar)) {
                            binder.removeDatatransferProgressListener(mProgressListener,
                                    mProgressListener.getUpload()   // the one that was added
                            );
                            mProgressListener = null;
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
                FileUploader.FileUploaderBinder uploaderBinder = mParentActivity.getFileUploaderBinder();
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
                mUploadsStorageManager.removeUpload(item);
                loadUploadItemsFromDb();
            });

        } else {    // UploadStatus.UPLOAD_SUCCESS
            itemViewHolder.button.setVisibility(View.INVISIBLE);
        }

        itemViewHolder.itemLayout.setOnClickListener(null);

        // click on item
        if (item.getUploadStatus() == UploadStatus.UPLOAD_FAILED) {
            if (UploadResult.CREDENTIAL_ERROR.equals(item.getLastResult())) {
                itemViewHolder.itemLayout.setOnClickListener(v ->
                        mParentActivity.getFileOperationsHelper().checkCurrentCredentials(
                                item.getAccount(mParentActivity)));
            } else {
                // not a credentials error
                itemViewHolder.itemLayout.setOnClickListener(v -> {
                    File file = new File(item.getLocalPath());
                    if (file.exists()) {
                        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
                        requester.retry(mParentActivity, item);
                        loadUploadItemsFromDb();
                    } else {
                        Snackbar.make(v.getRootView().findViewById(android.R.id.content),
                                mParentActivity.getString(R.string.local_file_not_found_message), Snackbar.LENGTH_LONG)
                                .show();
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
        fakeFileToCheatThumbnailsCacheManagerInterface.setMimetype(item.getMimeType());

        boolean allowedToCreateNewThumbnail = (ThumbnailsCacheManager.cancelPotentialThumbnailWork(
                fakeFileToCheatThumbnailsCacheManagerInterface, itemViewHolder.thumbnail)
        );

        // TODO this code is duplicated; refactor to a common place
        if ((MimeTypeUtil.isImage(fakeFileToCheatThumbnailsCacheManagerInterface)
                && fakeFileToCheatThumbnailsCacheManagerInterface.getRemoteId() != null &&
                item.getUploadStatus() == UploadStatus.UPLOAD_SUCCEEDED)) {
            // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf(fakeFileToCheatThumbnailsCacheManagerInterface.getRemoteId())
            );
            if (thumbnail != null && !fakeFileToCheatThumbnailsCacheManagerInterface.needsUpdateThumbnail()) {
                itemViewHolder.thumbnail.setImageBitmap(thumbnail);
            } else {
                // generate new Thumbnail
                if (allowedToCreateNewThumbnail) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                    itemViewHolder.thumbnail, mParentActivity.getStorageManager(), mParentActivity.getAccount()
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
                                    mParentActivity.getResources(),
                                    thumbnail,
                                    task
                            );
                    itemViewHolder.thumbnail.setImageDrawable(asyncDrawable);
                    task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(
                            fakeFileToCheatThumbnailsCacheManagerInterface, null));
                }
            }

            if ("image/png".equals(item.getMimeType())) {
                itemViewHolder.thumbnail.setBackgroundColor(mParentActivity.getResources()
                        .getColor(R.color.background_color));
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
                            new ThumbnailsCacheManager.AsyncThumbnailDrawable(mParentActivity.getResources(), thumbnail,
                                    task);

                    itemViewHolder.thumbnail.setImageDrawable(asyncDrawable);
                    task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, null));
                    Log_OC.v(TAG, "Executing task to generate a new thumbnail");
                }
            }

            if ("image/png".equalsIgnoreCase(item.getMimeType())) {
                itemViewHolder.thumbnail.setBackgroundColor(mParentActivity.getResources()
                        .getColor(R.color.background_color));
            }
        } else {
            itemViewHolder.thumbnail.setImageDrawable(MimeTypeUtil.getFileTypeIcon(item.getMimeType(), fileName,
                    account));
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
                status = mParentActivity.getString(R.string.uploads_view_later_waiting_to_upload);
                FileUploader.FileUploaderBinder binder = mParentActivity.getFileUploaderBinder();
                if (binder != null && binder.isUploadingNow(upload)) {
                    // really uploading, bind the progress bar to listen for progress updates
                    status = mParentActivity.getString(R.string.uploader_upload_in_progress_ticker);
                }
                break;

            case UPLOAD_SUCCEEDED:
                status = mParentActivity.getString(R.string.uploads_view_upload_status_succeeded);
                break;

            case UPLOAD_FAILED:
                switch (upload.getLastResult()) {
                    case CREDENTIAL_ERROR:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_failed_credentials_error
                        );
                        break;
                    case FOLDER_ERROR:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_failed_folder_error
                        );
                        break;
                    case FILE_NOT_FOUND:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_failed_localfile_error
                        );
                        break;
                    case FILE_ERROR:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_failed_file_error
                        );
                        break;
                    case PRIVILEDGES_ERROR:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_failed_permission_error
                        );
                        break;
                    case NETWORK_CONNECTION:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_failed_connection_error
                        );
                        break;
                    case DELAYED_FOR_WIFI:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_waiting_for_wifi
                        );
                        break;
                    case DELAYED_FOR_CHARGING:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_waiting_for_charging);
                        break;
                    case CONFLICT_ERROR:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_conflict
                        );
                        break;
                    case SERVICE_INTERRUPTED:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_service_interrupted
                        );
                        break;
                    case CANCELLED:
                        // should not get here ; cancelled uploads should be wiped out
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_cancelled
                        );
                        break;
                    case UPLOADED:
                        // should not get here ; status should be UPLOAD_SUCCESS
                        status = mParentActivity.getString(R.string.uploads_view_upload_status_succeeded);
                        break;
                    case MAINTENANCE_MODE:
                        status = mParentActivity.getString(R.string.maintenance_mode);
                        break;
                    case SSL_RECOVERABLE_PEER_UNVERIFIED:
                        status =
                                mParentActivity.getString(
                                        R.string.uploads_view_upload_status_failed_ssl_certificate_not_trusted
                                );
                        break;
                    case UNKNOWN:
                        status = mParentActivity.getString(R.string.uploads_view_upload_status_unknown_fail);
                        break;
                    case DELAYED_IN_POWER_SAVE_MODE:
                        status = mParentActivity.getString(
                                R.string.uploads_view_upload_status_waiting_exit_power_save_mode);
                        break;
                    default:
                        status = "New fail result but no description for the user";
                        break;
                }
                break;

            default:
                status = "Uncontrolled status: " + upload.getUploadStatus().toString();
        }
        return status;
    }


    @Override
    public SectionedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.upload_list_header, parent, false);
            return new HeaderViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.upload_list_item, parent, false);
            return new ItemViewHolder(v);
        }
    }

    /**
     * Load upload items from {@link UploadsStorageManager}.
     */
    public void loadUploadItemsFromDb() {
        Log_OC.d(TAG, "loadUploadItemsFromDb");

        for (UploadGroup group : mUploadGroups) {
            group.refresh();
        }

        notifyDataSetChanged();
    }

    private boolean onUploadItemClick(OCUpload file) {
        File f = new File(file.getLocalPath());
        if (!f.exists()) {
            DisplayUtils.showSnackMessage(mParentActivity, R.string.local_file_not_found_message);
        } else {
            openFileWithDefault(file.getLocalPath());
        }
        return true;
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
            mParentActivity.startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            DisplayUtils.showSnackMessage(mParentActivity, R.string.file_list_no_app_for_file_type);
            Log_OC.i(TAG, "Could not find app for sending log history.");
        }
    }

    static class HeaderViewHolder extends SectionedViewHolder {
        private final TextView title;

        HeaderViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.upload_list_title);
        }
    }

    static class ItemViewHolder extends SectionedViewHolder {
        private TextView name;
        private ImageView thumbnail;
        private TextView fileSize;
        private TextView date;
        private TextView status;
        private TextView account;
        private TextView remotePath;
        private ProgressBar progressBar;
        private ImageButton button;
        private LinearLayout itemLayout;

        ItemViewHolder(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.upload_name);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            fileSize = itemView.findViewById(R.id.upload_file_size);
            date = itemView.findViewById(R.id.upload_date);
            status = itemView.findViewById(R.id.upload_status);
            account = itemView.findViewById(R.id.upload_account);
            remotePath = itemView.findViewById(R.id.upload_remote_path);
            progressBar = itemView.findViewById(R.id.upload_progress_bar);
            button = itemView.findViewById(R.id.upload_right_button);
            itemLayout = itemView.findViewById(R.id.upload_list_item_layout);
        }
    }

    interface Refresh {
        void refresh();
    }

    abstract class UploadGroup implements Refresh {
        private OCUpload[] items;
        private String name;

        UploadGroup(String groupName) {
            this.name = groupName;
            items = new OCUpload[0];
        }

        String getGroupName() {
            return name;
        }

        public OCUpload[] getItems() {
            return items;
        }

        public OCUpload getItem(int position) {
            return items[position];
        }

        public void setItems(OCUpload[] items) {
            this.items = items;
        }

        int getGroupItemCount() {
            return items == null ? 0 : items.length;
        }

        Comparator<OCUpload> comparator = new Comparator<OCUpload>() {
            @Override
            public int compare(OCUpload upload1, OCUpload upload2) {
                if (upload1 == null) {
                    return -1;
                }
                if (upload2 == null) {
                    return 1;
                }
                if (UploadStatus.UPLOAD_IN_PROGRESS.equals(upload1.getUploadStatus())) {
                    if (!UploadStatus.UPLOAD_IN_PROGRESS.equals(upload2.getUploadStatus())) {
                        return -1;
                    }
                    // both are in progress
                    FileUploader.FileUploaderBinder binder = mParentActivity.getFileUploaderBinder();
                    if (binder != null) {
                        if (binder.isUploadingNow(upload1)) {
                            return -1;
                        } else if (binder.isUploadingNow(upload2)) {
                            return 1;
                        }
                    }
                } else if (upload2.getUploadStatus().equals(UploadStatus.UPLOAD_IN_PROGRESS)) {
                    return 1;
                }
                if (upload1.getUploadEndTimestamp() == 0 || upload2.getUploadEndTimestamp() == 0) {
                    return compareUploadId(upload1, upload2);
                } else {
                    return compareUpdateTime(upload1, upload2);
                }
            }

            @SuppressFBWarnings("Bx")
            private int compareUploadId(OCUpload upload1, OCUpload upload2) {
                return Long.valueOf(upload1.getUploadId()).compareTo(upload2.getUploadId());
            }

            @SuppressFBWarnings("Bx")
            private int compareUpdateTime(OCUpload upload1, OCUpload upload2) {
                return Long.valueOf(upload2.getUploadEndTimestamp()).compareTo(upload1.getUploadEndTimestamp());
            }
        };
    }
}
