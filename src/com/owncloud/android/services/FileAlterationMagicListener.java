/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.services;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.media.ExifInterface;
import android.os.Handler;
import android.os.PersistableBundle;
import android.text.TextUtils;

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

public class FileAlterationMagicListener implements FileAlterationListener {

    public static final String TAG = "FileAlterationMagicListener";

    private Context context;

    private SyncedFolder syncedFolder;
    private Handler handler = new Handler();

    private Map<String, Runnable> fileRunnable = new HashMap<>();

    public FileAlterationMagicListener(SyncedFolder syncedFolder) {
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
        if (file != null) {

            String mimetypeString = FileStorageUtils.getMimeTypeFromName(file.getAbsolutePath());
            Long dateFolder = file.lastModified();
            final Locale current = context.getResources().getConfiguration().locale;

            if (mimetypeString.equalsIgnoreCase("image/jpeg") || mimetypeString.equals("image/tiff")) {
                try {
                    ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
                    if (!TextUtils.isEmpty(exifInterface.getAttribute(ExifInterface.TAG_DATETIME))) {
                        String exifDate = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);
                        ParsePosition pos = new ParsePosition(0);
                        SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", current);
                        sFormatter.setTimeZone(TimeZone.getTimeZone(TimeZone.getDefault().getID()));
                        Date datetime = sFormatter.parse(exifDate, pos);
                        dateFolder = datetime.getTime();
                    }

                } catch (IOException e) {
                    Log_OC.d(TAG, "Failed to get the proper time " + e.getLocalizedMessage());
                }
            }


            final Long finalDateFolder = dateFolder;

                final Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    PersistableBundle bundle = new PersistableBundle();
                    bundle.putString(SyncedFolderJobService.LOCAL_PATH, file.getAbsolutePath());
                    bundle.putString(SyncedFolderJobService.REMOTE_PATH, FileStorageUtils.getInstantUploadFilePath(
                            current,
                            syncedFolder.getRemotePath(), file.getName(),
                            finalDateFolder,
                            syncedFolder.getSubfolderByDate()));
                    bundle.putString(SyncedFolderJobService.ACCOUNT, syncedFolder.getAccount());
                    bundle.putInt(SyncedFolderJobService.UPLOAD_BEHAVIOUR, syncedFolder.getUploadAction());

                    JobScheduler js = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

                    Long date = new Date().getTime();
                    JobInfo job = new JobInfo.Builder(
                            date.intValue(),
                            new ComponentName(context, SyncedFolderJobService.class))
                            .setRequiresCharging(syncedFolder.getChargingOnly())
                            .setMinimumLatency(10000)
                            .setRequiredNetworkType(syncedFolder.getWifiOnly() ? JobInfo.NETWORK_TYPE_UNMETERED : JobInfo.NETWORK_TYPE_ANY)
                            .setExtras(bundle)
                            .setPersisted(true)
                            .build();

                    Integer result = js.schedule(job);
                    if (result <= 0) {
                        Log_OC.d(TAG, "Job failed to start: " + result);
                    }

                    fileRunnable.remove(file.getAbsolutePath());
                }
            };

            fileRunnable.put(file.getAbsolutePath(), runnable);
            handler.postDelayed(runnable, 1500);
        }

    }

    @Override
    public void onFileChange(File file) {
        if (fileRunnable.containsKey(file.getAbsolutePath())) {
            handler.removeCallbacks(fileRunnable.get(file.getAbsolutePath()));
            handler.postDelayed(fileRunnable.get(file.getAbsolutePath()), 1500);
        }
    }

    @Override
    public void onFileDelete(File file) {
        if (fileRunnable.containsKey(file.getAbsolutePath())) {
            handler.removeCallbacks(fileRunnable.get(file.getAbsolutePath()));
            fileRunnable.remove(file.getAbsolutePath());
        }
    }

    @Override
    public void onStop(FileAlterationObserver observer) {
        // This method is intentionally empty
    }
}
