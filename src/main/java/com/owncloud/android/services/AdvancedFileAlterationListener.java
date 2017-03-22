/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.services;

import android.content.Context;
import android.media.ExifInterface;
import android.os.Handler;
import android.text.TextUtils;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileStorageUtils;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * Magical file alteration listener
 */

public class AdvancedFileAlterationListener implements FileAlterationListener {

    public static final String TAG = "AdvancedFileAlterationListener";
    public static final int DELAY_INVOCATION_MS = 2500;
    private Context context;

    private SyncedFolder syncedFolder;

    private Map<String, Runnable> uploadMap = new HashMap<>();
    private Handler handler = new Handler();

    public AdvancedFileAlterationListener(SyncedFolder syncedFolder) {
        super();

        context = MainApp.getAppContext();
        this.syncedFolder = syncedFolder;
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
        // This method is intentionally empty
    }

    @Override
    public void onDirectoryCreate(File directory) {
        // This method is intentionally empty
    }

    @Override
    public void onDirectoryChange(File directory) {
        // This method is intentionally empty
    }

    @Override
    public void onDirectoryDelete(File directory) {
        // This method is intentionally empty
    }

    @Override
    public void onFileCreate(final File file) {
        onFileCreate(file, DELAY_INVOCATION_MS);
    }

    public void onFileCreate(final File file, int delay) {
        if (file != null) {
            uploadMap.put(file.getAbsolutePath(), null);

            String mimetypeString = FileStorageUtils.getMimeTypeFromName(file.getAbsolutePath());
            Long lastModificationTime = file.lastModified();
            final Locale currentLocale = context.getResources().getConfiguration().locale;

            if ("image/jpeg".equalsIgnoreCase(mimetypeString) || "image/tiff".equalsIgnoreCase(mimetypeString)) {
                try {
                    ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
                    String exifDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                    if (!TextUtils.isEmpty(exifDate)) {
                        ParsePosition pos = new ParsePosition(0);
                        SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", currentLocale);
                        sFormatter.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
                        Date dateTime = sFormatter.parse(exifDate, pos);
                        lastModificationTime = dateTime.getTime();
                    }

                } catch (IOException e) {
                    Log_OC.d(TAG, "Failed to get the proper time " + e.getLocalizedMessage());
                }
            }


            final Long finalLastModificationTime = lastModificationTime;

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    PersistableBundleCompat bundle = new PersistableBundleCompat();
                    bundle.putString(AutoUploadJob.LOCAL_PATH, file.getAbsolutePath());
                    bundle.putString(AutoUploadJob.REMOTE_PATH, FileStorageUtils.getInstantUploadFilePath(
                            currentLocale,
                            syncedFolder.getRemotePath(), file.getName(),
                            finalLastModificationTime,
                            syncedFolder.getSubfolderByDate()));
                    bundle.putString(AutoUploadJob.ACCOUNT, syncedFolder.getAccount());
                    bundle.putInt(AutoUploadJob.UPLOAD_BEHAVIOUR, syncedFolder.getUploadAction());

                    new JobRequest.Builder(AutoUploadJob.TAG)
                            .setExecutionWindow(30_000L, 80_000L)
                            .setRequiresCharging(syncedFolder.getChargingOnly())
                            .setRequiredNetworkType(syncedFolder.getWifiOnly() ? JobRequest.NetworkType.UNMETERED :
                                    JobRequest.NetworkType.ANY)
                            .setExtras(bundle)
                            .setPersisted(false)
                            .setRequirementsEnforced(true)
                            .setUpdateCurrent(false)
                            .build()
                            .schedule();

                    uploadMap.remove(file.getAbsolutePath());
                }
            };

            uploadMap.put(file.getAbsolutePath(), runnable);
            handler.postDelayed(runnable, delay);
        }
    }

    @Override
    public void onFileChange(File file) {
        onFileChange(file, 2500);
    }

    public void onFileChange(File file, int delay) {
        Runnable runnable;
        if ((runnable = uploadMap.get(file.getAbsolutePath())) != null) {
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, delay);
        }
    }

    @Override
    public void onFileDelete(File file) {
        Runnable runnable;
        if ((runnable = uploadMap.get(file.getAbsolutePath())) != null) {
            handler.removeCallbacks(runnable);
            uploadMap.remove(file.getAbsolutePath());
        }
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
        // This method is intentionally empty
    }

    public int getActiveTasksCount() {
        return uploadMap.size();
    }
}
