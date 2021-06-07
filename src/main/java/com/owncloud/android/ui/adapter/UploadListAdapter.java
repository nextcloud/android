/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 *
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
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter;
import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.core.Clock;
import com.nextcloud.client.device.PowerManagementService;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.UploadListHeaderBinding;
import com.owncloud.android.databinding.UploadListItemBinding;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.db.OCUploadComparator;
import com.owncloud.android.db.UploadResult;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ThemeBarUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import java.io.File;
import java.util.Arrays;

import androidx.annotation.NonNull;

/**
 * This Adapter populates a ListView with following types of uploads: pending, active, completed. Filtering possible.
 */
public class UploadListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {
    private static final String TAG = UploadListAdapter.class.getSimpleName();

    private ProgressListener progressListener;
    private FileActivity parentActivity;
    private UploadsStorageManager uploadsStorageManager;
    private FileDataStorageManager storageManager;
    private ConnectivityService connectivityService;
    private PowerManagementService powerManagementService;
    private UserAccountManager accountManager;
    private Clock clock;
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

        headerViewHolder.binding.uploadListTitle.setText(
            String.format(parentActivity.getString(R.string.uploads_view_group_header),
                          group.getGroupName(), group.getGroupItemCount()));
        headerViewHolder.binding.uploadListTitle.setTextColor(ThemeColorUtils.primaryAccentColor(parentActivity));

        headerViewHolder.binding.uploadListTitle.setOnClickListener(v -> toggleSectionExpanded(section));

        switch (group.type) {
            case CURRENT:
            case FINISHED:
                headerViewHolder.binding.uploadListAction.setImageResource(R.drawable.ic_close);
                break;
            case FAILED:
                headerViewHolder.binding.uploadListAction.setImageResource(R.drawable.ic_sync);
                break;
        }

        headerViewHolder.binding.uploadListAction.setOnClickListener(v -> {
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
                    new Thread(() -> FileUploader.retryFailedUploads(
                        parentActivity,
                        null,
                        uploadsStorageManager,
                        connectivityService,
                        accountManager,
                        powerManagementService,
                        null
                    )).start();
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
                             final FileDataStorageManager storageManager,
                             final UserAccountManager accountManager,
                             final ConnectivityService connectivityService,
                             final PowerManagementService powerManagementService,
                             final Clock clock) {
        Log_OC.d(TAG, "UploadListAdapter");
        this.parentActivity = fileActivity;
        this.uploadsStorageManager = uploadsStorageManager;
        this.storageManager = storageManager;
        this.accountManager = accountManager;
        this.connectivityService = connectivityService;
        this.powerManagementService = powerManagementService;
        this.clock = clock;
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

        itemViewHolder.binding.uploadName.setText(item.getLocalPath());

        // local file name
        File remoteFile = new File(item.getRemotePath());
        String fileName = remoteFile.getName();
        if (fileName.length() == 0) {
            fileName = File.separator;
        }
        itemViewHolder.binding.uploadName.setText(fileName);

        // remote path to parent folder
        itemViewHolder.binding.uploadRemotePath.setText(new File(item.getRemotePath()).getParent());

        // file size
        if (item.getFileSize() != 0) {
            itemViewHolder.binding.uploadFileSize.setText(String.format("%s, ",
                    DisplayUtils.bytesToHumanReadable(item.getFileSize())));
        } else {
            itemViewHolder.binding.uploadFileSize.setText("");
        }

        // upload date
        long updateTime = item.getUploadEndTimestamp();
        CharSequence dateString = DisplayUtils.getRelativeDateTimeString(parentActivity,
                                                                         updateTime,
                                                                         DateUtils.SECOND_IN_MILLIS,
                                                                         DateUtils.WEEK_IN_MILLIS, 0);
        itemViewHolder.binding.uploadDate.setText(dateString);

        // account
        final Optional<User> optionalUser = accountManager.getUser(item.getAccountName());
        if (showUser) {
            itemViewHolder.binding.uploadAccount.setVisibility(View.VISIBLE);
            if (optionalUser.isPresent()) {
                itemViewHolder.binding.uploadAccount.setText(
                    DisplayUtils.getAccountNameDisplayText(optionalUser.get()));
            } else {
                itemViewHolder.binding.uploadAccount.setText(item.getAccountName());
            }
        } else {
            itemViewHolder.binding.uploadAccount.setVisibility(View.GONE);
        }

        // Reset fields visibility
        itemViewHolder.binding.uploadDate.setVisibility(View.VISIBLE);
        itemViewHolder.binding.uploadRemotePath.setVisibility(View.VISIBLE);
        itemViewHolder.binding.uploadFileSize.setVisibility(View.VISIBLE);
        itemViewHolder.binding.uploadStatus.setVisibility(View.VISIBLE);
        itemViewHolder.binding.uploadProgressBar.setVisibility(View.GONE);

        // Update information depending of upload details
        String status = getStatusText(item);
        switch (item.getUploadStatus()) {
            case UPLOAD_IN_PROGRESS:
                ThemeBarUtils.colorHorizontalProgressBar(itemViewHolder.binding.uploadProgressBar,
                                                         ThemeColorUtils.primaryAccentColor(parentActivity));
                itemViewHolder.binding.uploadProgressBar.setProgress(0);
                itemViewHolder.binding.uploadProgressBar.setVisibility(View.VISIBLE);

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
                        progressListener = new ProgressListener(item, itemViewHolder.binding.uploadProgressBar);
                        binder.addDatatransferProgressListener(progressListener, item);
                    } else {
                        // not really uploading; stop listening progress if view is reused!
                        if (progressListener != null &&
                            progressListener.isWrapping(itemViewHolder.binding.uploadProgressBar)) {
                            binder.removeDatatransferProgressListener(progressListener,
                                                                      progressListener.getUpload() // the one that was added
                                                                     );
                            progressListener = null;
                        }
                    }
                } else {
                    Log_OC.w(TAG, "FileUploaderBinder not ready yet for upload " + item.getRemotePath());
                }

                itemViewHolder.binding.uploadDate.setVisibility(View.GONE);
                itemViewHolder.binding.uploadFileSize.setVisibility(View.GONE);
                itemViewHolder.binding.uploadProgressBar.invalidate();
                break;

            case UPLOAD_FAILED:
                itemViewHolder.binding.uploadDate.setVisibility(View.GONE);
                break;

            case UPLOAD_SUCCEEDED:
                itemViewHolder.binding.uploadStatus.setVisibility(View.GONE);
                break;
        }
        itemViewHolder.binding.uploadStatus.setText(status);

        // bind listeners to perform actions
        if (item.getUploadStatus() == UploadStatus.UPLOAD_IN_PROGRESS) {
            // Cancel
            itemViewHolder.binding.uploadRightButton.setImageResource(R.drawable.ic_action_cancel_grey);
            itemViewHolder.binding.uploadRightButton.setVisibility(View.VISIBLE);
            itemViewHolder.binding.uploadRightButton.setOnClickListener(v -> {
                FileUploader.FileUploaderBinder uploaderBinder = parentActivity.getFileUploaderBinder();
                if (uploaderBinder != null) {
                    uploaderBinder.cancel(item);
                    loadUploadItemsFromDb();
                }
            });

        } else if (item.getUploadStatus() == UploadStatus.UPLOAD_FAILED) {
            if (item.getLastResult() == UploadResult.SYNC_CONFLICT) {
                itemViewHolder.binding.uploadRightButton.setImageResource(R.drawable.ic_dots_vertical);
                itemViewHolder.binding.uploadRightButton.setOnClickListener(view -> {
                    if (optionalUser.isPresent()) {
                        User user = optionalUser.get();
                        showItemConflictPopup(user, itemViewHolder, item, status, view);
                    }
                });
            } else {
                // Delete
                itemViewHolder.binding.uploadRightButton.setImageResource(R.drawable.ic_action_delete_grey);
                itemViewHolder.binding.uploadRightButton.setOnClickListener(v -> removeUpload(item));
            }
            itemViewHolder.binding.uploadRightButton.setVisibility(View.VISIBLE);
        } else {    // UploadStatus.UPLOAD_SUCCESS
            itemViewHolder.binding.uploadRightButton.setVisibility(View.INVISIBLE);
        }

        itemViewHolder.binding.uploadListItemLayout.setOnClickListener(null);

        // click on item
        if (item.getUploadStatus() == UploadStatus.UPLOAD_FAILED) {
            final UploadResult uploadResult = item.getLastResult();
            itemViewHolder.binding.uploadListItemLayout.setOnClickListener(v -> {
                if (uploadResult == UploadResult.CREDENTIAL_ERROR) {
                    parentActivity.getFileOperationsHelper().checkCurrentCredentials(item.getAccount(accountManager));
                    return;
                } else if (uploadResult == UploadResult.SYNC_CONFLICT && optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    if (checkAndOpenConflictResolutionDialog(user, itemViewHolder, item, status)) {
                        return;
                    }
                }

                // not a credentials error
                File file = new File(item.getLocalPath());
                if (file.exists()) {
                    FileUploader.retryUpload(parentActivity, item.getAccount(accountManager), item);
                    loadUploadItemsFromDb();
                } else {
                    DisplayUtils.showSnackMessage(
                        v.getRootView().findViewById(android.R.id.content),
                        R.string.local_file_not_found_message
                    );
                }
            });
        } else {
            itemViewHolder.binding.uploadListItemLayout.setOnClickListener(v -> onUploadItemClick(item));
        }

        // Set icon or thumbnail
        itemViewHolder.binding.thumbnail.setImageResource(R.drawable.file);

        /*
         * Cancellation needs do be checked and done before changing the drawable in fileIcon, or
         * {@link ThumbnailsCacheManager#cancelPotentialWork} will NEVER cancel any task.
         */
        OCFile fakeFileToCheatThumbnailsCacheManagerInterface = new OCFile(item.getRemotePath());
        fakeFileToCheatThumbnailsCacheManagerInterface.setStoragePath(item.getLocalPath());
        fakeFileToCheatThumbnailsCacheManagerInterface.setMimeType(item.getMimeType());

        boolean allowedToCreateNewThumbnail = ThumbnailsCacheManager.cancelPotentialThumbnailWork(
                fakeFileToCheatThumbnailsCacheManagerInterface, itemViewHolder.binding.thumbnail
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
                itemViewHolder.binding.thumbnail.setImageBitmap(thumbnail);
            } else {
                // generate new Thumbnail
                if (allowedToCreateNewThumbnail) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(
                                itemViewHolder.binding.thumbnail,
                                parentActivity.getStorageManager(),
                                parentActivity.getAccount()
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
                    itemViewHolder.binding.thumbnail.setImageDrawable(asyncDrawable);
                    task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(
                            fakeFileToCheatThumbnailsCacheManagerInterface, null));
                }
            }

            if ("image/png".equals(item.getMimeType())) {
                itemViewHolder.binding.thumbnail.setBackgroundColor(parentActivity.getResources()
                        .getColor(R.color.bg_default));
            }


        } else if (MimeTypeUtil.isImage(fakeFileToCheatThumbnailsCacheManagerInterface)) {
            File file = new File(item.getLocalPath());
            // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf(file.hashCode()));
            if (thumbnail != null) {
                itemViewHolder.binding.thumbnail.setImageBitmap(thumbnail);
            } else {
                // generate new Thumbnail
                if (allowedToCreateNewThumbnail) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(itemViewHolder.binding.thumbnail);

                    if (MimeTypeUtil.isVideo(file)) {
                        thumbnail = ThumbnailsCacheManager.mDefaultVideo;
                    } else {
                        thumbnail = ThumbnailsCacheManager.mDefaultImg;
                    }

                    final ThumbnailsCacheManager.AsyncThumbnailDrawable asyncDrawable =
                        new ThumbnailsCacheManager.AsyncThumbnailDrawable(parentActivity.getResources(), thumbnail,
                                                                          task);

                    itemViewHolder.binding.thumbnail.setImageDrawable(asyncDrawable);
                    task.execute(new ThumbnailsCacheManager.ThumbnailGenerationTaskObject(file, null));
                    Log_OC.v(TAG, "Executing task to generate a new thumbnail");
                }
            }

            if ("image/png".equalsIgnoreCase(item.getMimeType())) {
                itemViewHolder.binding.thumbnail.setBackgroundColor(parentActivity.getResources()
                        .getColor(R.color.bg_default));
            }
        } else {
            if (optionalUser.isPresent()) {
                final User user = optionalUser.get();
                final Drawable icon = MimeTypeUtil.getFileTypeIcon(item.getMimeType(),
                                                                   fileName,
                                                                   user,
                                                                   parentActivity);
                itemViewHolder.binding.thumbnail.setImageDrawable(icon);
            }
        }
    }

    private boolean checkAndOpenConflictResolutionDialog(User user,
                                                         ItemViewHolder itemViewHolder,
                                                         OCUpload item,
                                                         String status) {
        String remotePath = item.getRemotePath();
        OCFile ocFile = storageManager.getFileByPath(remotePath);

        if (ocFile == null) { // Remote file doesn't exist, try to refresh folder
            OCFile folder = storageManager.getFileByPath(new File(remotePath).getParent() + "/");
            if (folder != null && folder.isFolder()) {
                this.refreshFolder(itemViewHolder, user.toPlatformAccount(), folder, (caller, result) -> {
                    itemViewHolder.binding.uploadStatus.setText(status);
                    if (result.isSuccess()) {
                        OCFile file = storageManager.getFileByPath(remotePath);
                        if (file != null) {
                            this.openConflictActivity(file, item);
                        }
                    }
                });
                return true;
            }

            // Destination folder doesn't exist anymore
        }

        if (ocFile != null) {
            this.openConflictActivity(ocFile, item);
            return true;
        }

        // Remote file doesn't exist anymore = there is no more conflict
        return false;
    }

    private void showItemConflictPopup(User user,
                                       ItemViewHolder itemViewHolder,
                                       OCUpload item,
                                       String status,
                                       View view) {
        PopupMenu popup = new PopupMenu(MainApp.getAppContext(), view);
        popup.inflate(R.menu.upload_list_item_file_conflict);
        popup.setOnMenuItemClickListener(i -> {
            int itemId = i.getItemId();

            if (itemId == R.id.action_upload_list_resolve_conflict) {
                checkAndOpenConflictResolutionDialog(user, itemViewHolder, item, status);
            } else {
                removeUpload(item);
            }

            return true;
        });
        popup.show();
    }

    private void removeUpload(OCUpload item) {
        uploadsStorageManager.removeUpload(item);
        loadUploadItemsFromDb();
    }

    private void refreshFolder(
        ItemViewHolder view,
        Account account,
        OCFile folder,
        OnRemoteOperationListener listener) {
        view.binding.uploadListItemLayout.setClickable(false);
        view.binding.uploadStatus.setText(R.string.uploads_view_upload_status_fetching_server_version);
        Context context = MainApp.getAppContext();
        new RefreshFolderOperation(folder,
                                   clock.getCurrentTime(),
                                   false,
                                   false,
                                   true,
                                   storageManager,
                                   account,
                                   context)
            .execute(account, context, (caller, result) -> {
                view.binding.uploadListItemLayout.setClickable(true);
                listener.onRemoteOperationFinish(caller, result);
            }, parentActivity.getHandler());
    }

    private void openConflictActivity(OCFile file, OCUpload upload) {
        file.setStoragePath(upload.getLocalPath());

        Context context = MainApp.getAppContext();
        Intent intent = ConflictsResolveActivity.createIntent(file,
                                                              upload.getAccount(accountManager),
                                                              upload.getUploadId(),
                                                              Intent.FLAG_ACTIVITY_NEW_TASK,
                                                              context);

        context.startActivity(intent);
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
                UploadListHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
            );
        } else {
            return new ItemViewHolder(
                UploadListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
            );
        }
    }

    /**
     * Load upload items from {@link UploadsStorageManager}.
     */
    public final void loadUploadItemsFromDb() {
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
        UploadListHeaderBinding binding;

        HeaderViewHolder(UploadListHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    static class ItemViewHolder extends SectionedViewHolder {
        UploadListItemBinding binding;

        ItemViewHolder(UploadListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
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
