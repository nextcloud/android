/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
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

package com.owncloud.android.jobs;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.text.TextUtils;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.ui.activity.Preferences;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.FilesSyncHelper;
import com.owncloud.android.utils.MimeType;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.PowerUtils;

import java.io.File;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/*
    Job that:
        - restarts existing jobs if required
        - finds new and modified files since we last run this
        - creates upload tasks
 */
public class FilesSyncJob extends Job {
    public static final String TAG = "FilesSyncJob";
    public static final String SKIP_CUSTOM = "skipCustom";
    public static final String OVERRIDE_POWER_SAVING = "overridePowerSaving";
    private static final String WAKELOCK_TAG_SEPARATION = ":";

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        final Context context = MainApp.getAppContext();
        PowerManager.WakeLock wakeLock = null;

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, MainApp.getAuthority() +
                    WAKELOCK_TAG_SEPARATION + TAG);
            wakeLock.acquire();
        }

        PersistableBundleCompat bundle = params.getExtras();
        final boolean overridePowerSaving = bundle.getBoolean(OVERRIDE_POWER_SAVING, false);

        // If we are in power save mode, better to postpone upload
        if (PowerUtils.isPowerSaveMode(context) && !overridePowerSaving) {
            wakeLock.release();
            return Result.SUCCESS;
        }

        Resources resources = MainApp.getAppContext().getResources();
        boolean lightVersion = resources.getBoolean(R.bool.syncedFolder_light);

        final boolean skipCustom = bundle.getBoolean(SKIP_CUSTOM, false);
        FilesSyncHelper.restartJobsIfNeeded();
        FilesSyncHelper.insertAllDBEntries(skipCustom);

        // Create all the providers we'll need
        final ContentResolver contentResolver = context.getContentResolver();
        final FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);

        Locale currentLocale = context.getResources().getConfiguration().locale;
        SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", currentLocale);
        sFormatter.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();

        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if ((syncedFolder.isEnabled()) && (!skipCustom || MediaFolderType.CUSTOM != syncedFolder.getType())) {
                syncFolder(context, resources, lightVersion, filesystemDataProvider, currentLocale, sFormatter,
                        requester, syncedFolder);
            }
        }

        if (wakeLock != null) {
            wakeLock.release();
        }

        return Result.SUCCESS;
    }

    private void syncFolder(Context context, Resources resources, boolean lightVersion,
                            FilesystemDataProvider filesystemDataProvider, Locale currentLocale,
                            SimpleDateFormat sFormatter, FileUploader.UploadRequester requester,
                            SyncedFolder syncedFolder) {
        String remotePath;
        boolean subfolderByDate;
        Integer uploadAction;
        boolean needsCharging;
        boolean needsWifi;
        File file;
        ArbitraryDataProvider arbitraryDataProvider;
        Account account = AccountUtils.getOwnCloudAccountByName(context, syncedFolder.getAccount());

        if (lightVersion) {
            arbitraryDataProvider = new ArbitraryDataProvider(context.getContentResolver());
        } else {
            arbitraryDataProvider = null;
        }

        for (String path : filesystemDataProvider.getFilesForUpload(syncedFolder.getLocalPath(),
                Long.toString(syncedFolder.getId()))) {
            file = new File(path);
            Long lastModificationTime = calculateLastModificationTime(file, syncedFolder, sFormatter);
            String mimeType = MimeTypeUtil.getBestMimeTypeByFilename(file.getAbsolutePath());

            if (lightVersion) {
                needsCharging = resources.getBoolean(R.bool.syncedFolder_light_on_charging);
                needsWifi = account == null || arbitraryDataProvider.getBooleanValue(account.name,
                        Preferences.SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI);
                String uploadActionString = resources.getString(R.string.syncedFolder_light_upload_behaviour);
                uploadAction = getUploadAction(uploadActionString);

                subfolderByDate = resources.getBoolean(R.bool.syncedFolder_light_use_subfolders);

                remotePath = resources.getString(R.string.syncedFolder_remote_folder);
            } else {
                needsCharging = syncedFolder.getChargingOnly();
                needsWifi = syncedFolder.getWifiOnly();
                uploadAction = syncedFolder.getUploadAction();
                subfolderByDate = syncedFolder.getSubfolderByDate();
                remotePath = syncedFolder.getRemotePath();
            }

            if (!subfolderByDate) {
                String adaptedPath = file.getAbsolutePath()
                        .replace(syncedFolder.getLocalPath(), "")
                        .replace("/" + file.getName(), "");
                remotePath += adaptedPath;
            }

            requester.uploadFileWithOverwrite(
                    context,
                    account,
                    file.getAbsolutePath(),
                    FileStorageUtils.getInstantUploadFilePath(
                            currentLocale,
                            remotePath, file.getName(),
                            lastModificationTime, subfolderByDate),
                    uploadAction,
                    mimeType,
                    true,           // create parent folder if not existent
                    UploadFileOperation.CREATED_AS_INSTANT_PICTURE,
                    needsWifi,
                    needsCharging,
                    true
            );

            filesystemDataProvider.updateFilesystemFileAsSentForUpload(path,
                    Long.toString(syncedFolder.getId()));
        }
    }

    private Long calculateLastModificationTime(File file, SyncedFolder syncedFolder, SimpleDateFormat formatter) {
        Long lastModificationTime = file.lastModified();

        if (MediaFolderType.IMAGE == syncedFolder.getType()) {
            String mimeTypeString = FileStorageUtils.getMimeTypeFromName(file.getAbsolutePath());
            if (MimeType.JPEG.equalsIgnoreCase(mimeTypeString)
                    || MimeType.TIFF.equalsIgnoreCase(mimeTypeString)) {
                try {
                    ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
                    String exifDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                    if (!TextUtils.isEmpty(exifDate)) {
                        ParsePosition pos = new ParsePosition(0);
                        Date dateTime = formatter.parse(exifDate, pos);
                        lastModificationTime = dateTime.getTime();
                    }
                } catch (Exception e) {
                    Log_OC.d(TAG, "Failed to get the proper time " + e.getLocalizedMessage());
                }
            }
        }

        return lastModificationTime;
    }

    private Integer getUploadAction(String action) {
        switch (action) {
            case "LOCAL_BEHAVIOUR_FORGET":
                return FileUploader.LOCAL_BEHAVIOUR_FORGET;
            case "LOCAL_BEHAVIOUR_MOVE":
                return FileUploader.LOCAL_BEHAVIOUR_MOVE;
            case "LOCAL_BEHAVIOUR_DELETE":
                return FileUploader.LOCAL_BEHAVIOUR_DELETE;
            default:
                return FileUploader.LOCAL_BEHAVIOUR_FORGET;
        }
    }
}
