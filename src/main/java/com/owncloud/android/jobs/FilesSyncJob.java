/**
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

import android.content.ContentResolver;
import android.content.Context;
import android.media.ExifInterface;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.MediaFolder;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.FilesSyncHelper;

import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class FilesSyncJob extends Job {
    public static final String TAG = "FilesSyncJob";

    private static final String LAST_AUTOUPLOAD_JOB_RUN = "last_autoupload_job_run";

    // TODO: check for wifi status & charging status, stop and restart jobs as required

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();

        PowerManager powerManager = (PowerManager) context.getSystemService(context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                TAG);
        wakeLock.acquire();

        restartJobsIfNeeded();

        FilesSyncHelper.prepareSyncStatusForAccounts();

        // Create all the providers we'll need
        final FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);

        for (SyncedFolder syncedFolder : syncedFolderProvider.getSyncedFolders()) {
            if (syncedFolder.isEnabled()) {
                String syncedFolderType;
                if (MediaFolder.IMAGE == syncedFolder.getType()) {
                    syncedFolderType = "image/";
                    Log.d("IN A JOB", "SYNCED FOLDER VIDEO");
                } else if (MediaFolder.VIDEO == syncedFolder.getType()) {
                    syncedFolderType = "video/";
                    Log.d("IN A JOB", "SYNCED FOLDER IMAGE");
                } else {
                    syncedFolderType = null;
                    Log.d("IN A JOB", "SYNCED FOLDER ENABLED");
                }

                Log.d("IN A JOB", "SYNCED FOLDER ENABLED");

                // ignore custom folders for now
                if (syncedFolderType != null) {
                    for (String path : filesystemDataProvider.getFilesForUpload(syncedFolder.getLocalPath(),
                            syncedFolder.getAccount(), syncedFolderType)) {
                        File file = new File(path);

                        Log.d("IN A JOB", "PERO");
                        Long lastModificationTime = file.lastModified();
                        final Locale currentLocale = context.getResources().getConfiguration().locale;

                        if (syncedFolder.equals("image/")) {
                            String mimetypeString = FileStorageUtils.getMimeTypeFromName(file.getAbsolutePath());
                            if ("image/jpeg".equalsIgnoreCase(mimetypeString) || "image/tiff".
                                    equalsIgnoreCase(mimetypeString)) {
                                try {
                                    ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
                                    String exifDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                                    if (!TextUtils.isEmpty(exifDate)) {
                                        ParsePosition pos = new ParsePosition(0);
                                        SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss",
                                                currentLocale);
                                        sFormatter.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
                                        Date dateTime = sFormatter.parse(exifDate, pos);
                                        lastModificationTime = dateTime.getTime();
                                    }

                                } catch (IOException e) {
                                    Log_OC.d(TAG, "Failed to get the proper time " + e.getLocalizedMessage());
                                }
                            }
                        }

                        PersistableBundleCompat bundle = new PersistableBundleCompat();
                        bundle.putString(AutoUploadJob.LOCAL_PATH, file.getAbsolutePath());
                        bundle.putString(AutoUploadJob.REMOTE_PATH, FileStorageUtils.getInstantUploadFilePath(
                                currentLocale,
                                syncedFolder.getRemotePath(), file.getName(),
                                lastModificationTime,
                                syncedFolder.getSubfolderByDate()));
                        bundle.putString(AutoUploadJob.ACCOUNT, syncedFolder.getAccount());
                        bundle.putInt(AutoUploadJob.UPLOAD_BEHAVIOUR, syncedFolder.getUploadAction());

                        new JobRequest.Builder(AutoUploadJob.TAG)
                                .setExecutionWindow(30_000L, 80_000L)
                                .setRequiresCharging(syncedFolder.getChargingOnly())
                                .setRequiredNetworkType(syncedFolder.getWifiOnly() ? JobRequest.NetworkType.UNMETERED :
                                        JobRequest.NetworkType.CONNECTED)
                                .setExtras(bundle)
                                .setRequirementsEnforced(true)
                                .setUpdateCurrent(false)
                                .build()
                                .schedule();

                        filesystemDataProvider.updateFilesInList(new Object[]{path}, syncedFolder.getAccount());
                    }
                }
            }
        }

        wakeLock.release();
        return Result.SUCCESS;
    }


    private void restartJobsIfNeeded() {
        final Context context = MainApp.getAppContext();
        List<Integer> restartedJobIds = new ArrayList<Integer>();
        int jobId;
        boolean restartedInCurrentIteration = false;

        for (JobRequest jobRequest : JobManager.instance().getAllJobRequestsForTag(AutoUploadJob.TAG)) {
            restartedInCurrentIteration = false;
            // Handle case of charging
            if (jobRequest.requiresCharging() && Device.isCharging(context)) {
                if (jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.CONNECTED) &&
                        !Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
                    jobId = jobRequest.cancelAndEdit().build().schedule();
                    restartedInCurrentIteration = true;
                } else if (jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.UNMETERED) &&
                        Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED)) {
                    jobId = jobRequest.cancelAndEdit().build().schedule();
                    restartedInCurrentIteration = true;
                }
            }

            // Handle case of wifi

            if (!restartedInCurrentIteration) {
                if (jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.CONNECTED) &&
                        !Device.getNetworkType(context).equals(JobRequest.NetworkType.ANY)) {
                    jobRequest.cancelAndEdit().build().schedule();
                } else if (jobRequest.requiredNetworkType().equals(JobRequest.NetworkType.UNMETERED) &&
                        Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED)) {
                    jobRequest.cancelAndEdit().build().schedule();
                }
            }
        }
    }
}
