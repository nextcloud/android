/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.app.NotificationManager;
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
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.network.ConnectivityService;
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
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.RefreshFolderOperation;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import androidx.annotation.NonNull;

/**
 * This Adapter populates a ListView with following types of uploads: pending, active, completed. Filtering possible.
 */
public class UploadListAdapter extends SectionedRecyclerViewAdapter<SectionedViewHolder> {
    private static final String TAG = UploadListAdapter.class.getSimpleName();

    private ProgressListener progressListener;
    private final FileActivity parentActivity;
    private final UploadsStorageManager uploadsStorageManager;
    private final FileDataStorageManager storageManager;
    private final ConnectivityService connectivityService;
    private final PowerManagementService powerManagementService;
    private final UserAccountManager accountManager;
    private final Clock clock;
    private final UploadGroup[] uploadGroups;
    private final boolean showUser;
    private final ViewThemeUtils viewThemeUtils;
    private NotificationManager mNotificationManager;

    private final FileUploadHelper uploadHelper = FileUploadHelper.Companion.instance();

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
        viewThemeUtils.platform.colorPrimaryTextViewElement(headerViewHolder.binding.uploadListTitle);

        headerViewHolder.binding.uploadListTitle.setOnClickListener(v -> {
            toggleSectionExpanded(section);
            headerViewHolder.binding.uploadListState.setImageResource(isSectionExpanded(section) ?
                                                                          R.drawable.ic_expand_less :
                                                                          R.drawable.ic_expand_more);
        });

        switch (group.type) {
            case CURRENT, FINISHED -> headerViewHolder.binding.uploadListAction.setImageResource(R.drawable.ic_close);
            case CANCELLED, FAILED ->
                headerViewHolder.binding.uploadListAction.setImageResource(R.drawable.ic_dots_vertical);

        }

        headerViewHolder.binding.uploadListAction.setOnClickListener(v -> {
            switch (group.type) {
                case CURRENT -> {
                    for (OCUpload upload : group.getItems()) {
                        uploadHelper.cancelFileUpload(upload.getRemotePath(), upload.getAccountName());
                    }
                    loadUploadItemsFromDb();
                }
                case FINISHED -> {
                    uploadsStorageManager.clearSuccessfulUploads();
                    loadUploadItemsFromDb();
                }
                case FAILED -> {
                    showFailedPopupMenu(headerViewHolder);
                }
                case CANCELLED -> {
                    showCancelledPopupMenu(headerViewHolder);
                }
            }
        });
    }

    private void showFailedPopupMenu(HeaderViewHolder headerViewHolder) {
        PopupMenu failedPopup = new PopupMenu(MainApp.getAppContext(), headerViewHolder.binding.uploadListAction);
        failedPopup.inflate(R.menu.upload_list_failed_options);
        failedPopup.setOnMenuItemClickListener(i -> {
            int itemId = i.getItemId();

            if (itemId == R.id.action_upload_list_failed_clear) {
                uploadsStorageManager.clearFailedButNotDelayedUploads();
                clearTempEncryptedFolder();
                loadUploadItemsFromDb();
            } else if (itemId == R.id.action_upload_list_failed_retry) {

                // FIXME For e2e resume is not working
                new Thread(() -> {
                    FileUploadHelper.Companion.instance().retryFailedUploads(
                        uploadsStorageManager,
                        connectivityService,
                        accountManager,
                        powerManagementService);
                    parentActivity.runOnUiThread(this::loadUploadItemsFromDb);
                }).start();
            }

            return true;
        });

        failedPopup.show();
    }

    private void showCancelledPopupMenu(HeaderViewHolder headerViewHolder) {
        PopupMenu popup = new PopupMenu(MainApp.getAppContext(), headerViewHolder.binding.uploadListAction);
        popup.inflate(R.menu.upload_list_cancelled_options);

        popup.setOnMenuItemClickListener(i -> {
            int itemId = i.getItemId();

            if (itemId == R.id.action_upload_list_cancelled_clear) {
                uploadsStorageManager.clearCancelledUploadsForCurrentAccount();
                loadUploadItemsFromDb();
                clearTempEncryptedFolder();
            } else if (itemId == R.id.action_upload_list_cancelled_resume) {
                retryCancelledUploads();
            }

            return true;
        });

        popup.show();
    }

    private void clearTempEncryptedFolder() {
        Optional<User> user = parentActivity.getUser();
        user.ifPresent(value -> FileDataStorageManager.clearTempEncryptedFolder(value.getAccountName()));
    }

    // FIXME For e2e resume is not working
    private void retryCancelledUploads() {
        new Thread(() -> {
            boolean showNotExistMessage = FileUploadHelper.Companion.instance().retryCancelledUploads(
                uploadsStorageManager,
                connectivityService,
                accountManager,
                powerManagementService);

            parentActivity.runOnUiThread(this::loadUploadItemsFromDb);
            parentActivity.runOnUiThread(() -> {
                if (showNotExistMessage) {
                    showNotExistMessage();
                }
            });
        }).start();
    }

    private void showNotExistMessage() {
        DisplayUtils.showSnackMessage(parentActivity, R.string.upload_action_file_not_exist_message);
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
                             final Clock clock,
                             final ViewThemeUtils viewThemeUtils) {
        Log_OC.d(TAG, "UploadListAdapter");

        this.parentActivity = fileActivity;
        this.uploadsStorageManager = uploadsStorageManager;
        this.storageManager = storageManager;
        this.accountManager = accountManager;
        this.connectivityService = connectivityService;
        this.powerManagementService = powerManagementService;
        this.clock = clock;
        this.viewThemeUtils = viewThemeUtils;

        uploadGroups = new UploadGroup[4];

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

        uploadGroups[2] = new UploadGroup(Type.CANCELLED,
                                          parentActivity.getString(
                                              R.string.uploads_view_group_manually_cancelled_uploads)) {
            @Override
            public void refresh() {
                fixAndSortItems(uploadsStorageManager.getCancelledUploadsForCurrentAccount());
            }
        };

        uploadGroups[3] = new UploadGroup(Type.FINISHED,
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
            case UPLOAD_IN_PROGRESS -> {
                viewThemeUtils.platform.themeHorizontalProgressBar(itemViewHolder.binding.uploadProgressBar);
                itemViewHolder.binding.uploadProgressBar.setProgress(0);
                itemViewHolder.binding.uploadProgressBar.setVisibility(View.VISIBLE);

                if (uploadHelper.isUploadingNow(item)) {
                    // really uploading, so...
                    // ... unbind the old progress bar, if any; ...
                    if (progressListener != null) {
                        String targetKey = FileUploadHelper.Companion.buildRemoteName(progressListener.getUpload().getAccountName(), progressListener.getUpload().getRemotePath());
                        uploadHelper.removeUploadTransferProgressListener(progressListener, targetKey);
                    }
                    // ... then, bind the current progress bar to listen for updates
                    progressListener = new ProgressListener(item, itemViewHolder.binding.uploadProgressBar);
                    String targetKey = FileUploadHelper.Companion.buildRemoteName(item.getAccountName(), item.getRemotePath());
                    uploadHelper.addUploadTransferProgressListener(progressListener, targetKey);

                } else {
                    // not really uploading; stop listening progress if view is reused!
                    if (progressListener != null &&
                        progressListener.isWrapping(itemViewHolder.binding.uploadProgressBar)) {

                        String targetKey = FileUploadHelper.Companion.buildRemoteName(progressListener.getUpload().getAccountName(), progressListener.getUpload().getRemotePath());

                        uploadHelper.removeUploadTransferProgressListener(progressListener, targetKey);
                        progressListener = null;
                    }
                }

                itemViewHolder.binding.uploadDate.setVisibility(View.GONE);
                itemViewHolder.binding.uploadFileSize.setVisibility(View.GONE);
                itemViewHolder.binding.uploadProgressBar.invalidate();
            }
            case UPLOAD_FAILED -> itemViewHolder.binding.uploadDate.setVisibility(View.GONE);
            case UPLOAD_SUCCEEDED, UPLOAD_CANCELLED ->
                itemViewHolder.binding.uploadStatus.setVisibility(View.GONE);
        }

        // show status if same file conflict or local file deleted or upload cancelled
        if ((item.getUploadStatus() == UploadStatus.UPLOAD_SUCCEEDED && item.getLastResult() != UploadResult.UPLOADED)
            || item.getUploadStatus() == UploadStatus.UPLOAD_CANCELLED) {

            itemViewHolder.binding.uploadStatus.setVisibility(View.VISIBLE);
            itemViewHolder.binding.uploadDate.setVisibility(View.GONE);
            itemViewHolder.binding.uploadFileSize.setVisibility(View.GONE);
        }

        itemViewHolder.binding.uploadStatus.setText(status);

        // bind listeners to perform actions
        if (item.getUploadStatus() == UploadStatus.UPLOAD_IN_PROGRESS) {
            // Cancel
            itemViewHolder.binding.uploadRightButton.setImageResource(R.drawable.ic_action_cancel_grey);
            itemViewHolder.binding.uploadRightButton.setVisibility(View.VISIBLE);
            itemViewHolder.binding.uploadRightButton.setOnClickListener(v -> {
                uploadHelper.cancelFileUpload(item.getRemotePath(), item.getAccountName());
                loadUploadItemsFromDb();
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
        } else {    // UploadStatus.UPLOAD_SUCCEEDED
            itemViewHolder.binding.uploadRightButton.setVisibility(View.INVISIBLE);
        }

        itemViewHolder.binding.uploadListItemLayout.setOnClickListener(null);

        // Set icon or thumbnail
        itemViewHolder.binding.thumbnail.setImageResource(R.drawable.file);

        // click on item
        if (item.getUploadStatus() == UploadStatus.UPLOAD_FAILED ||
            item.getUploadStatus() == UploadStatus.UPLOAD_CANCELLED) {

            final UploadResult uploadResult = item.getLastResult();
            itemViewHolder.binding.uploadListItemLayout.setOnClickListener(v -> {
                if (uploadResult == UploadResult.CREDENTIAL_ERROR) {
                    final Optional<User> optUser = accountManager.getUser(item.getAccountName());
                    final User user = optUser.orElseThrow(RuntimeException::new);
                    parentActivity.getFileOperationsHelper().checkCurrentCredentials(user);
                    return;
                } else if (uploadResult == UploadResult.SYNC_CONFLICT && optionalUser.isPresent()) {
                    User user = optionalUser.get();
                    if (checkAndOpenConflictResolutionDialog(user, itemViewHolder, item, status)) {
                        return;
                    }
                }

                // not a credentials error
                File file = new File(item.getLocalPath());
                Optional<User> user = accountManager.getUser(item.getAccountName());
                if (file.exists() && user.isPresent()) {
                    uploadHelper.retryUpload(item, user.get());
                    loadUploadItemsFromDb();
                } else {
                    DisplayUtils.showSnackMessage(
                        v.getRootView().findViewById(android.R.id.content),
                        R.string.local_file_not_found_message
                                                 );
                }
            });
        } else if (item.getUploadStatus() == UploadStatus.UPLOAD_SUCCEEDED) {
            itemViewHolder.binding.uploadListItemLayout.setOnClickListener(v -> onUploadedItemClick(item));
        }


        // click on thumbnail to open locally
        if (item.getUploadStatus() != UploadStatus.UPLOAD_SUCCEEDED) {
            itemViewHolder.binding.thumbnail.setOnClickListener(v -> onUploadingItemClick(item));
        }

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
                Optional<User> user = parentActivity.getUser();
                if (allowedToCreateNewThumbnail && user.isPresent()) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                        new ThumbnailsCacheManager.ThumbnailGenerationTask(
                            itemViewHolder.binding.thumbnail,
                            parentActivity.getStorageManager(),
                            user.get()
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
                                                                   parentActivity,
                                                                   viewThemeUtils);
                itemViewHolder.binding.thumbnail.setImageDrawable(icon);
            }
        }
    }

    private boolean checkAndOpenConflictResolutionDialog(User user,
                                                         ItemViewHolder itemViewHolder,
                                                         OCUpload item,
                                                         String status) {
        String remotePath = item.getRemotePath();
        OCFile localFile = storageManager.getFileByEncryptedRemotePath(remotePath);

        if (localFile == null) {
            // Remote file doesn't exist, try to refresh folder
            OCFile folder = storageManager.getFileByEncryptedRemotePath(new File(remotePath).getParent() + "/");

            if (folder != null && folder.isFolder()) {
                refreshFolderAndUpdateUI(itemViewHolder, user, folder, remotePath, item, status);
                return true;
            }

            // Destination folder doesn't exist anymore
        }

        if (localFile != null) {
            this.openConflictActivity(localFile, item);
            return true;
        }

        // Remote file doesn't exist anymore = there is no more conflict
        return false;
    }

    private void refreshFolderAndUpdateUI(ItemViewHolder holder, User user, OCFile folder, String remotePath, OCUpload item, String status) {
        Context context = MainApp.getAppContext();

        this.refreshFolder(context, holder, user, folder, (caller, result) -> {
            holder.binding.uploadStatus.setText(status);

            if (result.isSuccess()) {
                OCFile fileOnServer = storageManager.getFileByEncryptedRemotePath(remotePath);

                if (fileOnServer != null) {
                    openConflictActivity(fileOnServer, item);
                } else {
                    displayFileNotFoundError(holder.itemView, context);
                }
            }
        });
    }

    private void displayFileNotFoundError(View itemView, Context context) {
        String message = context.getString(R.string.uploader_file_not_found_message);
        DisplayUtils.showSnackMessage(itemView, message);
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

    public void removeUpload(OCUpload item) {
        uploadsStorageManager.removeUpload(item);
        cancelOldErrorNotification(item);
        loadUploadItemsFromDb();
    }

    private void refreshFolder(
        Context context,
        ItemViewHolder view,
        User user,
        OCFile folder,
        OnRemoteOperationListener listener) {
        view.binding.uploadListItemLayout.setClickable(false);
        view.binding.uploadStatus.setText(R.string.uploads_view_upload_status_fetching_server_version);
        new RefreshFolderOperation(folder,
                                   clock.getCurrentTime(),
                                   false,
                                   false,
                                   true,
                                   storageManager,
                                   user,
                                   context)
            .execute(user, context, (caller, result) -> {
                view.binding.uploadListItemLayout.setClickable(true);
                listener.onRemoteOperationFinish(caller, result);
            }, parentActivity.getHandler());
    }

    private void openConflictActivity(OCFile file, OCUpload upload) {
        file.setStoragePath(upload.getLocalPath());

        Context context = MainApp.getAppContext();
        Optional<User> user = accountManager.getUser(upload.getAccountName());
        if (user.isPresent()) {
            Intent intent = ConflictsResolveActivity.createIntent(file,
                                                                  user.get(),
                                                                  upload.getUploadId(),
                                                                  Intent.FLAG_ACTIVITY_NEW_TASK,
                                                                  context);

            context.startActivity(intent);
        }
    }

    /**
     * Gets the status text to show to the user according to the status and last result of the the given upload.
     *
     * @param upload Upload to describe.
     * @return Text describing the status of the given upload.
     */
    private String getStatusText(OCUpload upload) {
        String status;
        switch (upload.getUploadStatus()) {
            case UPLOAD_IN_PROGRESS -> {
                status = parentActivity.getString(R.string.uploads_view_later_waiting_to_upload);
                if (uploadHelper.isUploadingNow(upload)) {
                    // really uploading, bind the progress bar to listen for progress updates
                    status = parentActivity.getString(R.string.uploader_upload_in_progress_ticker);
                }
                if (parentActivity.getAppPreferences().isGlobalUploadPaused()) {
                    status = parentActivity.getString(R.string.upload_global_pause_title);
                }
            }
            case UPLOAD_SUCCEEDED -> {
                if (upload.getLastResult() == UploadResult.SAME_FILE_CONFLICT) {
                    status = parentActivity.getString(R.string.uploads_view_upload_status_succeeded_same_file);
                } else if (upload.getLastResult() == UploadResult.FILE_NOT_FOUND) {
                    status = getUploadFailedStatusText(upload.getLastResult());
                } else {
                    status = parentActivity.getString(R.string.uploads_view_upload_status_succeeded);
                }
            }
            case UPLOAD_FAILED -> {
                status = getUploadFailedStatusText(upload.getLastResult());
            }
            case UPLOAD_CANCELLED -> {
                status = parentActivity.getString(R.string.upload_manually_cancelled);
            }

            default -> {
                status = "Uncontrolled status: " + upload.getUploadStatus();
            }
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
            case QUOTA_EXCEEDED:
                status = parentActivity.getString(R.string.upload_quota_exceeded);
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

    /**
     * Open local file.
     */
    private void onUploadingItemClick(OCUpload file) {
        File f = new File(file.getLocalPath());
        if (!f.exists()) {
            DisplayUtils.showSnackMessage(parentActivity, R.string.local_file_not_found_message);
        } else {
            openFileWithDefault(file.getLocalPath());
        }
    }

    /**
     * Open remote file.
     */
    private void onUploadedItemClick(OCUpload upload) {
        final OCFile file = parentActivity.getStorageManager().getFileByEncryptedRemotePath(upload.getRemotePath());
        if (file == null) {
            DisplayUtils.showSnackMessage(parentActivity, R.string.error_retrieving_file);
            Log_OC.i(TAG, "Could not find uploaded file on remote.");
            return;
        }

        if (PreviewImageFragment.canBePreviewed(file)) {
            //show image preview and stay in uploads tab
            Intent intent = FileDisplayActivity.openFileIntent(parentActivity, parentActivity.getUser().get(), file);
            parentActivity.startActivity(intent);
        } else {
            Intent intent = new Intent(parentActivity, FileDisplayActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(FileDisplayActivity.KEY_FILE_PATH, upload.getRemotePath());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            parentActivity.startActivity(intent);
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
        CURRENT, FINISHED, FAILED, CANCELLED
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
            for (OCUpload upload : array) {
                upload.setDataFixed(uploadHelper);
            }
            Arrays.sort(array, new OCUploadComparator());

            setItems(array);
        }

        private int getGroupItemCount() {
            return items == null ? 0 : items.length;
        }
    }

    public void cancelOldErrorNotification(OCUpload upload) {

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) parentActivity.getSystemService(parentActivity.NOTIFICATION_SERVICE);
        }

        if (upload == null) {
            return;
        }
        mNotificationManager.cancel(NotificationUtils.createUploadNotificationTag(upload.getRemotePath(), upload.getLocalPath()),
                                    FileUploadWorker.NOTIFICATION_ERROR_ID);

    }

}
