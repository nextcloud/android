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
import android.content.res.Resources;
import android.media.ExifInterface;
import android.os.Handler;
import android.text.TextUtils;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.jobs.AutoUploadJob;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.Preferences;
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
    private boolean lightVersion;

    private SyncedFolder syncedFolder;

    private Map<String, Runnable> uploadMap = new HashMap<>();
    private Handler handler = new Handler();

    public AdvancedFileAlterationListener(SyncedFolder syncedFolder, boolean lightVersion) {
        super();

        context = MainApp.getAppContext();
        this.lightVersion = lightVersion;
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

                    String remotePath;
                    boolean subfolderByDate;
                    boolean chargingOnly;
                    boolean wifiOnly;
                    Integer uploadAction;
                    String accountName = syncedFolder.getAccount();

                    if (lightVersion) {
                        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(context
                                .getContentResolver());

                        Resources resources = MainApp.getAppContext().getResources();

                        remotePath = resources.getString(R.string.syncedFolder_remote_folder) + OCFile.PATH_SEPARATOR +
                                new File(syncedFolder.getLocalPath()).getName() + OCFile.PATH_SEPARATOR;
                        subfolderByDate = resources.getBoolean(R.bool.syncedFolder_light_use_subfolders);
                        chargingOnly = resources.getBoolean(R.bool.syncedFolder_light_on_charging);
                        wifiOnly = arbitraryDataProvider.getBooleanValue(accountName,
                                Preferences.SYNCED_FOLDER_LIGHT_UPLOAD_ON_WIFI);
                        String uploadActionString = resources.getString(R.string.syncedFolder_light_upload_behaviour);
                        uploadAction = getUploadAction(uploadActionString);
                    } else {
                        remotePath = syncedFolder.getRemotePath();
                        subfolderByDate = syncedFolder.getSubfolderByDate();
                        chargingOnly = syncedFolder.getChargingOnly();
                        wifiOnly = syncedFolder.getWifiOnly();
                        uploadAction = syncedFolder.getUploadAction();
                    }

                    PersistableBundleCompat bundle = new PersistableBundleCompat();
                    bundle.putString(AutoUploadJob.LOCAL_PATH, file.getAbsolutePath());
                    bundle.putString(AutoUploadJob.REMOTE_PATH, FileStorageUtils.getInstantUploadFilePath(
                            currentLocale,
                            remotePath, file.getName(),
                            finalLastModificationTime,
                            subfolderByDate));
                    bundle.putString(AutoUploadJob.ACCOUNT, accountName);
                    bundle.putInt(AutoUploadJob.UPLOAD_BEHAVIOUR, uploadAction);

                    new JobRequest.Builder(AutoUploadJob.TAG)
                            .setExecutionWindow(30_000L, 80_000L)
                            .setRequiresCharging(chargingOnly)
                            .setRequiredNetworkType(wifiOnly ? JobRequest.NetworkType.UNMETERED :
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
