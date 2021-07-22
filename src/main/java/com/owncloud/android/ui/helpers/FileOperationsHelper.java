/*
 * ownCloud Android client application
 *
 * @author masensio
 * @author David A. Velasco
 * @author Juan Carlos González Cabrero
 * @author Andy Scherzinger
 * @author Chris Narkiewicz
 *
 * Copyright (C) 2015 ownCloud Inc.
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.helpers;

import android.Manifest;
import android.accounts.Account;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.User;
import com.nextcloud.client.network.ConnectivityService;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.FileMenuFilter;
import com.owncloud.android.files.StreamMediaFileOperation;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.CheckEtagRemoteOperation;
import com.owncloud.android.lib.resources.files.model.FileVersion;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.operations.SynchronizeFileOperation;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.ExternalSiteWebView;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.RichDocumentsEditorWebView;
import com.owncloud.android.ui.activity.ShareActivity;
import com.owncloud.android.ui.activity.TextEditorWebView;
import com.owncloud.android.ui.dialog.SendFilesDialog;
import com.owncloud.android.ui.dialog.SendShareDialog;
import com.owncloud.android.ui.events.EncryptionEvent;
import com.owncloud.android.ui.events.FavoriteEvent;
import com.owncloud.android.ui.events.SyncEventFinished;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.PermissionUtil;
import com.owncloud.android.utils.UriUtils;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

/**
 * Helper implementation for file operations locally and remote.
 */
public class FileOperationsHelper {

    private static final String TAG = FileOperationsHelper.class.getSimpleName();
    private static final Pattern mPatternUrl = Pattern.compile("^URL=(.+)$");
    private static final Pattern mPatternString = Pattern.compile("<string>(.+)</string>");
    private static final String FILE_EXTENSION_URL = "url";
    private static final String FILE_EXTENSION_DESKTOP = "desktop";
    private static final String FILE_EXTENSION_WEBLOC = "webloc";
    public static final int SINGLE_LINK_SIZE = 1;

    private FileActivity fileActivity;
    private CurrentAccountProvider currentAccount;
    private ConnectivityService connectivityService;

    /// Identifier of operation in progress which result shouldn't be lost
    private long mWaitingForOpId = Long.MAX_VALUE;

    public FileOperationsHelper(FileActivity fileActivity,
                                CurrentAccountProvider currentAccount,
                                ConnectivityService connectivityService) {
        this.fileActivity = fileActivity;
        this.currentAccount = currentAccount;
        this.connectivityService = connectivityService;
    }

    @Nullable
    private String getUrlFromFile(String storagePath, Pattern pattern) {
        String url = null;

        InputStreamReader fr = null;
        BufferedReader br = null;
        try {
            fr = new InputStreamReader(new FileInputStream(storagePath), Charset.forName("UTF-8"));
            br = new BufferedReader(fr);

            String line;
            while ((line = br.readLine()) != null) {
                Matcher m = pattern.matcher(line);
                if (m.find()) {
                    url = m.group(1);
                    break;
                }
            }
        } catch (IOException e) {
            Log_OC.d(TAG, e.getMessage());
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Log_OC.d(TAG, "Error closing buffered reader for URL file", e);
                }
            }

            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    Log_OC.d(TAG, "Error closing file reader for URL file", e);
                }
            }
        }
        return url;
    }

    @Nullable
    private Intent createIntentFromFile(String storagePath) {
        String url = null;
        int lastIndexOfDot = storagePath.lastIndexOf('.');
        if (lastIndexOfDot >= 0) {
            String fileExt = storagePath.substring(lastIndexOfDot + 1);
            if (FILE_EXTENSION_URL.equalsIgnoreCase(fileExt) || FILE_EXTENSION_DESKTOP.equalsIgnoreCase(fileExt)) {
                // Windows internet shortcut file .url
                // Ubuntu internet shortcut file .desktop
                url = getUrlFromFile(storagePath, mPatternUrl);
            } else if (FILE_EXTENSION_WEBLOC.equalsIgnoreCase(fileExt)) {
                // mac internet shortcut file .webloc
                url = getUrlFromFile(storagePath, mPatternString);
            }
        }
        if (url == null) {
            return null;
        }
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }


    public void startSyncForFileAndIntent(OCFile file, Intent intent) {
        new Thread(() -> {
            User user = fileActivity.getUser().orElseThrow(RuntimeException::new);
            FileDataStorageManager storageManager = new FileDataStorageManager(fileActivity.getAccount(),
                                                                               fileActivity.getContentResolver());

            // check if file is in conflict (this is known due to latest folder refresh)
            if (file.isInConflict()) {
                syncFile(file, user, storageManager);
                EventBus.getDefault().post(new SyncEventFinished(intent));

                return;
            }

            // check if latest sync is >30s ago
            OCFile parent = storageManager.getFileById(file.getParentId());
            if (parent != null && parent.getLastSyncDateForData() + 30 * 1000 > System.currentTimeMillis()) {
                EventBus.getDefault().post(new SyncEventFinished(intent));

                return;
            }

            // if offline or walled garden, show old version with warning
            if (!connectivityService.getConnectivity().isConnected() || connectivityService.isInternetWalled()) {
                DisplayUtils.showSnackMessage(fileActivity, R.string.file_not_synced);
                EventBus.getDefault().post(new SyncEventFinished(intent));

                return;
            }

            // check for changed eTag
            CheckEtagRemoteOperation checkEtagOperation = new CheckEtagRemoteOperation(file.getRemotePath(),
                                                                                       file.getEtag());
            RemoteOperationResult result = checkEtagOperation.execute(user.toPlatformAccount(), fileActivity);

            // eTag changed, sync file
            if (result.getCode() == RemoteOperationResult.ResultCode.ETAG_CHANGED) {
                syncFile(file, user, storageManager);
            }

            EventBus.getDefault().post(new SyncEventFinished(intent));
        }).start();
    }

    private void syncFile(OCFile file, User user, FileDataStorageManager storageManager) {
        fileActivity.runOnUiThread(() -> fileActivity.showLoadingDialog(fileActivity.getResources()
                                                                            .getString(R.string.sync_in_progress)));

        SynchronizeFileOperation sfo = new SynchronizeFileOperation(file,
                                                                    null,
                                                                    user,
                                                                    true,
                                                                    fileActivity,
                                                                    storageManager);
        RemoteOperationResult result = sfo.execute(fileActivity);

        if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            // ISSUE 5: if the user is not running the app (this is a service!),
            // this can be very intrusive; a notification should be preferred
            Intent intent = ConflictsResolveActivity.createIntent(file,
                                                                  user.toPlatformAccount(),
                                                                  -1,
                                                                  Intent.FLAG_ACTIVITY_NEW_TASK,
                                                                  fileActivity);

            fileActivity.startActivity(intent);
        } else {
            if (file.isDown()) {
                FileStorageUtils.checkIfFileFinishedSaving(file);
                if (!result.isSuccess()) {
                    DisplayUtils.showSnackMessage(fileActivity, R.string.file_not_synced);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Failed to sleep for a bit");
                    }
                }
            }
        }
        fileActivity.dismissLoadingDialog();
    }

    public void openFile(OCFile file) {
        if (file != null) {
            final Intent openFileWithIntent = createOpenFileIntent(file);

            List<ResolveInfo> launchables = fileActivity.getPackageManager().
                    queryIntentActivities(openFileWithIntent, PackageManager.GET_RESOLVED_FILTER);

            if (launchables.isEmpty()) {
                Optional<User> optionalUser = fileActivity.getUser();

                if (optionalUser.isPresent() && FileMenuFilter.isEditorAvailable(fileActivity.getContentResolver(),
                                                                                 optionalUser.get(),
                                                                                 file.getMimeType())) {
                    openFileWithTextEditor(file, fileActivity);
                } else {
                    Account account = fileActivity.getAccount();
                    OCCapability capability = fileActivity.getStorageManager().getCapability(account.name);
                    if (capability.getRichDocumentsMimeTypeList().contains(file.getMimeType()) &&
                        capability.getRichDocumentsDirectEditing().isTrue()) {
                        openFileAsRichDocument(file, fileActivity);
                        return;
                    } else {
                        DisplayUtils.showSnackMessage(fileActivity, R.string.file_list_no_app_for_file_type);
                        return;
                    }
                }
            }

            fileActivity.showLoadingDialog(fileActivity.getResources().getString(R.string.sync_in_progress));
            new Thread(new Runnable() {
                @Override
                public void run() {
                    User user = currentAccount.getUser();
                    FileDataStorageManager storageManager =
                        new FileDataStorageManager(user.toPlatformAccount(), fileActivity.getContentResolver());
                    // a fresh object is needed; many things could have occurred to the file
                    // since it was registered to observe again, assuming that local files
                    // are linked to a remote file AT MOST, SOMETHING TO BE DONE;
                    SynchronizeFileOperation sfo = new SynchronizeFileOperation(file,
                                                                                null,
                                                                                user,
                                                                                true,
                                                                                fileActivity,
                                                                                storageManager);
                    RemoteOperationResult result = sfo.execute(fileActivity);
                    fileActivity.dismissLoadingDialog();
                    if (result.getCode() == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
                        // ISSUE 5: if the user is not running the app (this is a service!),
                        // this can be very intrusive; a notification should be preferred
                        Intent intent = ConflictsResolveActivity.createIntent(file,
                                                                              user.toPlatformAccount(),
                                                                              -1,
                                                                              Intent.FLAG_ACTIVITY_NEW_TASK,
                                                                              fileActivity);
                        fileActivity.startActivity(intent);
                    } else {
                        if (!launchables.isEmpty()) {
                            try {
                                if (!result.isSuccess()) {
                                    DisplayUtils.showSnackMessage(fileActivity, R.string.file_not_synced);
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException e) {
                                        Log.e(TAG, "Failed to sleep");
                                    }
                                }

                                openFileWithIntent.setFlags(openFileWithIntent.getFlags() |
                                        Intent.FLAG_ACTIVITY_NEW_TASK);
                                fileActivity.startActivity(openFileWithIntent);
                            } catch (ActivityNotFoundException exception) {
                                DisplayUtils.showSnackMessage(fileActivity, R.string.file_list_no_app_for_file_type);
                            }
                        } else {
                            DisplayUtils.showSnackMessage(fileActivity, R.string.file_list_no_app_for_file_type);
                        }
                    }

                }
            }).start();

        } else {
            Log_OC.e(TAG, "Trying to open a NULL OCFile");
        }
    }

    public void openFileAsRichDocument(OCFile file, Context context) {
        Intent collaboraWebViewIntent = new Intent(context, RichDocumentsEditorWebView.class);
        collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, "Collabora");
        collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_FILE, file);
        collaboraWebViewIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
        context.startActivity(collaboraWebViewIntent);
    }

    public void openFileWithTextEditor(OCFile file, Context context) {
        Intent textEditorIntent = new Intent(context, TextEditorWebView.class);
        textEditorIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, "Text");
        textEditorIntent.putExtra(ExternalSiteWebView.EXTRA_FILE, file);
        textEditorIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
        context.startActivity(textEditorIntent);
    }

    public void openRichWorkspaceWithTextEditor(OCFile file, String url, Context context) {
        Intent textEditorIntent = new Intent(context, TextEditorWebView.class);
        textEditorIntent.putExtra(ExternalSiteWebView.EXTRA_TITLE, "Text");
        textEditorIntent.putExtra(ExternalSiteWebView.EXTRA_URL, url);
        textEditorIntent.putExtra(ExternalSiteWebView.EXTRA_FILE, file);
        textEditorIntent.putExtra(ExternalSiteWebView.EXTRA_SHOW_SIDEBAR, false);
        context.startActivity(textEditorIntent);
    }

    @NonNull
    private Intent createOpenFileIntent(OCFile file) {
        String storagePath = file.getStoragePath();
        Uri fileUri = getFileUri(file, MainApp.getAppContext().getResources().getStringArray(R.array
                .ms_office_extensions));
        Intent openFileWithIntent = null;
        int lastIndexOfDot = storagePath.lastIndexOf('.');
        if (lastIndexOfDot >= 0) {
            String fileExt = storagePath.substring(lastIndexOfDot + 1);
            String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExt);
            if (guessedMimeType != null) {
                openFileWithIntent = new Intent(Intent.ACTION_VIEW);
                openFileWithIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                openFileWithIntent.setDataAndType(
                        fileUri,
                        guessedMimeType
                );
            }
        }

        if (openFileWithIntent == null) {
            openFileWithIntent = createIntentFromFile(storagePath);
        }

        if (openFileWithIntent == null) {
            openFileWithIntent = new Intent(Intent.ACTION_VIEW);
            openFileWithIntent.setDataAndType(
                    fileUri,
                    file.getMimeType()
            );
        }

        openFileWithIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        return openFileWithIntent;
    }

    private Uri getFileUri(OCFile file, String... officeExtensions) {
        if (file.getFileName().contains(".") &&
                Arrays.asList(officeExtensions).contains(file.getFileName().substring(file.getFileName().
                        lastIndexOf(".") + 1)) &&
                !file.getStoragePath().startsWith(MainApp.getAppContext().getFilesDir().getAbsolutePath())) {
            return file.getLegacyExposedFileUri();
        } else {
            return file.getExposedFileUri(fileActivity);
        }
    }

    public void streamMediaFile(OCFile file) {
        fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_a_moment));
        final User user = currentAccount.getUser();
        new Thread(() -> {
            StreamMediaFileOperation sfo = new StreamMediaFileOperation(file.getLocalId());
            RemoteOperationResult result = sfo.execute(user.toPlatformAccount(), fileActivity);

            fileActivity.dismissLoadingDialog();

            if (!result.isSuccess()) {
                DisplayUtils.showSnackMessage(fileActivity, R.string.stream_not_possible_headline);
                return;
            }

            Intent openFileWithIntent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse((String) result.getData().get(0));

            openFileWithIntent.setDataAndType(uri, file.getMimeType());

            fileActivity.startActivity(Intent.createChooser(openFileWithIntent,
                                                            fileActivity.getString(R.string.stream)));
        }).start();
    }

    /**
     * Helper method to share a file via a public link. Starts a request to do it in {@link OperationsService}
     *
     * @param file     The file to share.
     * @param password Optional password to protect the public share.
     */
    public void shareFileViaPublicShare(OCFile file, String password) {
        if (file != null) {
            fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_a_moment));
            Intent service = new Intent(fileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_CREATE_SHARE_VIA_LINK);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
            if (!TextUtils.isEmpty(password)) {
                service.putExtra(OperationsService.EXTRA_SHARE_PASSWORD, password);
            }
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);

        } else {
            Log_OC.e(TAG, "Trying to share a NULL OCFile");
            // TODO user-level error?
        }
    }

    public void getFileWithLink(@NonNull OCFile file) {
        List<OCShare> shares = fileActivity.getStorageManager().getSharesByPathAndType(file.getRemotePath(),
                                                                                       ShareType.PUBLIC_LINK,
                                                                                       "");

        if (shares.size() == SINGLE_LINK_SIZE) {
            FileActivity.copyAndShareFileLink(fileActivity, file, shares.get(0).getShareLink());
        } else {
            if (fileActivity instanceof FileDisplayActivity) {
                ((FileDisplayActivity) fileActivity).showDetails(file, 1);
            } else {
                showShareFile(file);
            }
        }
    }

    /**
     * Helper method to share a file with a known sharee. Starts a request to do it in {@link OperationsService}
     *
     * @param file        The file to share.
     * @param shareeName  Name (user name or group name) of the target sharee.
     * @param shareType   The share type determines the sharee type.
     * @param permissions Permissions to grant to sharee on the shared file.
     */
    public void shareFileWithSharee(OCFile file, String shareeName, ShareType shareType, int permissions) {
        if (file != null) {
            // TODO check capability?
            fileActivity.showLoadingDialog(fileActivity.getApplicationContext().
                    getString(R.string.wait_a_moment));

            Intent service = new Intent(fileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_CREATE_SHARE_WITH_SHAREE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_SHARE_WITH, shareeName);
            service.putExtra(OperationsService.EXTRA_SHARE_TYPE, shareType);
            service.putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, permissions);
            mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);

        } else {
            Log_OC.e(TAG, "Trying to share a NULL OCFile");
        }
    }

    /**
     * Helper method to revert to a file version. Starts a request to do it in {@link OperationsService}
     *
     * @param fileVersion The file version to restore
     */
    public void restoreFileVersion(FileVersion fileVersion) {
        if (fileVersion != null) {
            fileActivity.showLoadingDialog(fileActivity.getApplicationContext().
                    getString(R.string.wait_a_moment));

            Intent service = new Intent(fileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_RESTORE_VERSION);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_FILE_VERSION, fileVersion);
            mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);
        } else {
            Log_OC.e(TAG, "Trying to restore a NULL FileVersion");
        }
    }

    /**
     * Helper method to unshare a file publicly shared via link. Starts a request to do it in {@link OperationsService}
     *
     * @param file The file to unshare.
     */
    public void unshareShare(OCFile file, OCShare share) {

        // Unshare the file: Create the intent
        Intent unshareService = new Intent(fileActivity, OperationsService.class);
        unshareService.setAction(OperationsService.ACTION_UNSHARE);
        unshareService.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        unshareService.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        unshareService.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());

        queueShareIntent(unshareService);
    }

    private void queueShareIntent(Intent shareIntent) {
        // Unshare the file
        mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(shareIntent);

        fileActivity.showLoadingDialog(fileActivity.getApplicationContext().getString(R.string.wait_a_moment));
    }

    /**
     * Show an instance of {@link ShareType} for sharing or unsharing the {@link OCFile} received as parameter.
     *
     * @param file File to share or unshare.
     */
    public void showShareFile(OCFile file) {
        Intent intent = new Intent(fileActivity, ShareActivity.class);
        intent.putExtra(FileActivity.EXTRA_FILE, file);
        intent.putExtra(FileActivity.EXTRA_ACCOUNT, fileActivity.getAccount());
        fileActivity.startActivity(intent);
    }

    /**
     * Updates a public share on a file to set its label. Starts a request to do it in {@link OperationsService}
     *
     * @param label new label
     */
    public void setLabelToPublicShare(OCShare share, String label) {
        // Set password updating share
        Intent updateShareIntent = new Intent(fileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_PUBLIC_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_PUBLIC_LABEL, (label == null) ? "" : label);

        queueShareIntent(updateShareIntent);
    }

    /**
     * Updates a share on a file to set its password.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param share    File which share will be protected with a password.
     * @param password Password to set for the public link; null or empty string to clear
     *                 the current password
     */
    public void setPasswordToShare(OCShare share, String password) {
        Intent updateShareIntent = new Intent(fileActivity, OperationsService.class);
        if (TextUtils.isEmpty(share.getShareLink())) {
            updateShareIntent.setAction(OperationsService.ACTION_UPDATE_USER_SHARE);
        } else {
            updateShareIntent.setAction(OperationsService.ACTION_UPDATE_PUBLIC_SHARE);
        }
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_PASSWORD, (password == null) ? "" : password);
        queueShareIntent(updateShareIntent);
    }


    /**
     * Updates a public share on a file to set its expiration date.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param share                  {@link OCShare} instance which permissions will be updated.
     * @param expirationTimeInMillis Expiration date to set. A negative value clears the current expiration
     *                               date, leaving the link unrestricted. Zero makes no change.
     */
    public void setExpirationDateToShare(OCShare share, long expirationTimeInMillis) {
        Intent updateShareIntent = new Intent(fileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_USER_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_EXPIRATION_DATE_IN_MILLIS, expirationTimeInMillis);
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, 0);
        queueShareIntent(updateShareIntent);
    }

    /**
     * Updates a share on a file to set its access permissions.
     * Starts a request to do it in {@link OperationsService}
     *
     * @param share       {@link OCShare} instance which permissions will be updated.
     * @param permissions New permissions to set. A value <= 0 makes no update.
     */
    public void setPermissionsToShare(OCShare share, int permissions) {
        Intent updateShareIntent = new Intent(fileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_USER_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, permissions);
        queueShareIntent(updateShareIntent);
    }

    /**
     * Updates a public share on a folder to set its editing permission. Starts a request to do it in {@link
     * OperationsService}
     *
     * @param share            {@link OCShare} instance which permissions will be updated.
     * @param uploadPermission New state of the permission for editing the folder shared via link.
     */
    public void setUploadPermissionsToPublicShare(OCShare share, boolean uploadPermission) {
        Intent updateShareIntent = new Intent(fileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_USER_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        if (uploadPermission) {
            updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, 3);
        } else {
            updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_PERMISSIONS, 1);
        }

        queueShareIntent(updateShareIntent);
    }

    public void setHideFileDownloadPermissionsToPublicShare(OCShare share, boolean hideFileDownload) {
        Intent updateShareIntent = new Intent(fileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_PUBLIC_SHARE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_HIDE_FILE_DOWNLOAD, hideFileDownload);

        queueShareIntent(updateShareIntent);
    }

    public void updateNoteToShare(OCShare share, String note) {
        Intent updateShareIntent = new Intent(fileActivity, OperationsService.class);
        updateShareIntent.setAction(OperationsService.ACTION_UPDATE_SHARE_NOTE);
        updateShareIntent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_ID, share.getId());
        updateShareIntent.putExtra(OperationsService.EXTRA_SHARE_NOTE, note);

        queueShareIntent(updateShareIntent);
    }

    public void sendShareFile(OCFile file, boolean hideNcSharingOptions) {
        // Show dialog
        FragmentManager fm = fileActivity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);

        OCCapability capability = fileActivity.getStorageManager().getCapability(fileActivity.getAccount().name);
        SendShareDialog mSendShareDialog = SendShareDialog.newInstance(file, hideNcSharingOptions, capability);
        mSendShareDialog.setFileOperationsHelper(this);
        mSendShareDialog.show(ft, "TAG_SEND_SHARE_DIALOG");
    }

    public void sendFiles(Set<OCFile> files) {
        // Show dialog
        FragmentManager fm = fileActivity.getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.addToBackStack(null);

        SendFilesDialog sendFilesDialog = SendFilesDialog.newInstance(files);
        sendFilesDialog.show(ft, "TAG_SEND_SHARE_DIALOG");
    }

    public void sendShareFile(OCFile file) {
        sendShareFile(file, !file.canReshare());
    }

    public void sendCachedImage(OCFile file, String packageName, String activityName) {
        if (file != null) {
            Context context = MainApp.getAppContext();
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            // set MimeType
            sendIntent.setType(file.getMimeType());
            sendIntent.setComponent(new ComponentName(packageName, activityName));
            sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("content://" +
                    context.getResources().getString(R.string.image_cache_provider_authority) +
                                                                   file.getRemotePath()));
            sendIntent.putExtra(Intent.ACTION_SEND, true);      // Send Action
            sendIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            fileActivity.startActivity(Intent.createChooser(sendIntent,
                                                            context.getString(R.string.actionbar_send_file)));
        } else {
            Log_OC.wtf(TAG, "Trying to send a NULL OCFile");
        }
    }

    public void setPictureAs(OCFile file, View view) {
        if (file != null) {
            Context context = MainApp.getAppContext();
            Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
            Uri uri;

            try {
                if (file.isDown()) {
                    File externalFile = new File(file.getStoragePath());

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        uri = FileProvider.getUriForFile(context,
                                context.getResources().getString(R.string.file_provider_authority), externalFile);
                    } else {
                        uri = Uri.fromFile(externalFile);
                    }
                } else {
                    uri = Uri.parse(UriUtils.URI_CONTENT_SCHEME +
                            context.getResources().getString(R.string.image_cache_provider_authority) +
                                        file.getRemotePath());
                }

                intent.setDataAndType(uri, file.getMimeType());
                fileActivity.startActivityForResult(Intent.createChooser(intent,
                                                                         fileActivity.getString(R.string.set_as)),
                                                    200);

                intent.setDataAndType(uri, file.getMimeType());
            } catch (ActivityNotFoundException exception) {
                DisplayUtils.showSnackMessage(view, R.string.picture_set_as_no_app);
            }
        } else {
            Log_OC.wtf(TAG, "Trying to send a NULL OCFile");
        }
    }

    /**
     * Request the synchronization of a file or folder with the OC server, including its contents.
     *
     * @param file The file or folder to synchronize
     */
    public void syncFile(OCFile file) {
        if (!file.isFolder()) {
            Intent intent = new Intent(fileActivity, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FILE);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            intent.putExtra(OperationsService.EXTRA_SYNC_FILE_CONTENTS, true);
            mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(intent);
            fileActivity.showLoadingDialog(fileActivity.getApplicationContext().
                    getString(R.string.wait_a_moment));

        } else {
            Intent intent = new Intent(fileActivity, OperationsService.class);
            intent.setAction(OperationsService.ACTION_SYNC_FOLDER);
            intent.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
            intent.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            fileActivity.startService(intent);
        }
    }

    public void toggleFavoriteFiles(Collection<OCFile> files, boolean shouldBeFavorite) {
        List<OCFile> alreadyRightStateList = new ArrayList<>();
        for (OCFile file : files) {
            if (file.isFavorite() == shouldBeFavorite) {
                alreadyRightStateList.add(file);
            }
        }

        files.removeAll(alreadyRightStateList);

        for (OCFile file : files) {
            toggleFavoriteFile(file, shouldBeFavorite);
        }
    }

    public void toggleFavoriteFile(OCFile file, boolean shouldBeFavorite) {
        if (file.isFavorite() != shouldBeFavorite) {
            EventBus.getDefault().post(new FavoriteEvent(file.getRemotePath(), shouldBeFavorite, file.getRemoteId()));
        }
    }

    public void toggleEncryption(OCFile file, boolean shouldBeEncrypted) {
        if (file.isEncrypted() != shouldBeEncrypted) {
            EventBus.getDefault().post(new EncryptionEvent(file.getLocalId(),
                                                           file.getRemoteId(),
                                                           file.getRemotePath(),
                                                           shouldBeEncrypted));
        }
    }

    public void renameFile(OCFile file, String newFilename) {
        // RenameFile
        Intent service = new Intent(fileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_RENAME);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
        service.putExtra(OperationsService.EXTRA_NEWNAME, newFilename);
        mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);

        fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_a_moment));
    }


    /**
     * Start operations to delete one or several files
     *
     * @param files         Files to delete
     * @param onlyLocalCopy When 'true' only local copy of the files is removed; otherwise files are also deleted
     *                      in the server.
     * @param inBackground  When 'true', do not show any loading dialog
     */
    public void removeFiles(Collection<OCFile> files, boolean onlyLocalCopy, boolean inBackground) {
        for (OCFile file : files) {
            // RemoveFile
            Intent service = new Intent(fileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_REMOVE);
            service.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
            service.putExtra(OperationsService.EXTRA_FILE, file);
            service.putExtra(OperationsService.EXTRA_REMOVE_ONLY_LOCAL, onlyLocalCopy);
            service.putExtra(OperationsService.EXTRA_IN_BACKGROUND, inBackground);
            mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }

        if (!inBackground) {
            fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_a_moment));
        }
    }


    public void createFolder(String remotePath) {
        // Create Folder
        Intent service = new Intent(fileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_CREATE_FOLDER);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
        service.putExtra(OperationsService.EXTRA_REMOTE_PATH, remotePath);
        mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);

        fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_a_moment));
    }

    /**
     * Cancel the transference in downloads (files/folders) and file uploads
     *
     * @param file OCFile
     */
    public void cancelTransference(OCFile file) {
        User currentUser = fileActivity.getUser().orElseThrow(IllegalStateException::new);
        if (file.isFolder()) {
            OperationsService.OperationsServiceBinder opsBinder =
                    fileActivity.getOperationsServiceBinder();
            if (opsBinder != null) {
                opsBinder.cancel(currentUser.toPlatformAccount(), file);
            }
        }

        // for both files and folders
        FileDownloaderBinder downloaderBinder = fileActivity.getFileDownloaderBinder();
        if (downloaderBinder != null && downloaderBinder.isDownloading(currentUser, file)) {
            downloaderBinder.cancel(currentUser.toPlatformAccount(), file);
        }
        FileUploaderBinder uploaderBinder = fileActivity.getFileUploaderBinder();
        if (uploaderBinder != null && uploaderBinder.isUploading(currentUser, file)) {
            uploaderBinder.cancel(currentUser.toPlatformAccount(), file);
        }
    }

    /**
     * Start operations to move one or several files
     *
     * @param files        Files to move
     * @param targetFolder Folder where the files while be moved into
     */
    public void moveFiles(Collection<OCFile> files, OCFile targetFolder) {
        for (OCFile file : files) {
            Intent service = new Intent(fileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_MOVE_FILE);
            service.putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, targetFolder.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
            mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }
        fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_a_moment));
    }

    /**
     * Start operations to copy one or several files
     *
     * @param files        Files to copy
     * @param targetFolder Folder where the files while be copied into
     */
    public void copyFiles(Collection<OCFile> files, OCFile targetFolder) {
        for (OCFile file : files) {
            Intent service = new Intent(fileActivity, OperationsService.class);
            service.setAction(OperationsService.ACTION_COPY_FILE);
            service.putExtra(OperationsService.EXTRA_NEW_PARENT_PATH, targetFolder.getRemotePath());
            service.putExtra(OperationsService.EXTRA_REMOTE_PATH, file.getRemotePath());
            service.putExtra(OperationsService.EXTRA_ACCOUNT, fileActivity.getAccount());
            mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);
        }
        fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_a_moment));
    }

    public long getOpIdWaitingFor() {
        return mWaitingForOpId;
    }


    public void setOpIdWaitingFor(long waitingForOpId) {
        mWaitingForOpId = waitingForOpId;
    }

    /**
     * Starts a check of the currently stored credentials for the given account.
     *
     * @param account OC account which credentials will be checked.
     */
    public void checkCurrentCredentials(Account account) {
        Intent service = new Intent(fileActivity, OperationsService.class);
        service.setAction(OperationsService.ACTION_CHECK_CURRENT_CREDENTIALS);
        service.putExtra(OperationsService.EXTRA_ACCOUNT, account);
        mWaitingForOpId = fileActivity.getOperationsServiceBinder().queueNewOperation(service);

        fileActivity.showLoadingDialog(fileActivity.getString(R.string.wait_checking_credentials));
    }

    public void uploadFromCamera(Activity activity, int requestCode) {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        File photoFile = createImageFile(activity);

        Uri photoUri = FileProvider.getUriForFile(activity.getApplicationContext(),
                                                  activity.getResources().getString(R.string.file_provider_authority), photoFile);
        pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        if (pictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            if (PermissionUtil.checkSelfPermission(activity, Manifest.permission.CAMERA)) {
                activity.startActivityForResult(pictureIntent, requestCode);
            } else {
                PermissionUtil.requestCameraPermission(activity);
            }
        } else {
            DisplayUtils.showSnackMessage(activity, "No Camera found");
        }
    }

    public static File createImageFile(Activity activity) {
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return new File(storageDir + "/directCameraUpload.jpg");
    }

    public static String getCapturedImageName() {
        return new SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(new Date()) + ".jpg";
    }

    /**
     * @return -1 if no space could computed, otherwise available space in bytes
     */
    public static Long getAvailableSpaceOnDevice() {
        StatFs stat;
        try {
            stat = new StatFs(MainApp.getStoragePath());
        } catch (NullPointerException | IllegalArgumentException e) {
            return -1L;
        }

        return stat.getBlockSizeLong() * stat.getAvailableBlocksLong();
    }
}
