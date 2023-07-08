/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datastorage;

import android.os.Environment;

import com.owncloud.android.MainApp;
import com.owncloud.android.utils.PermissionUtil;

import java.io.File;

/**
 * @author Bartosz Przybylski
 */
public class DataStorageProvider {

    private static final UniqueStorageList mCachedStoragePoints = new UniqueStorageList();
    private static final DataStorageProvider sInstance = new DataStorageProvider();

    public static DataStorageProvider getInstance() {
        return sInstance;
    }

    private DataStorageProvider() {}

    public StoragePoint[] getAvailableStoragePoints() {
        if (mCachedStoragePoints.size() != 0) {
            return mCachedStoragePoints.toArray(new StoragePoint[0]);
        }

        StoragePoint storagePoint;
        for (File f : MainApp.getAppContext().getExternalMediaDirs()) {
            if (f != null) {
                storagePoint = new StoragePoint();
                storagePoint.setPath(f.getAbsolutePath());
                storagePoint.setDescription(f.getAbsolutePath());
                storagePoint.setPrivacyType(StoragePoint.PrivacyType.PUBLIC);
                if (f.getAbsolutePath().startsWith("/storage/emulated/0")) {
                    storagePoint.setStorageType(StoragePoint.StorageType.INTERNAL);
                    mCachedStoragePoints.add(storagePoint);
                } else {
                    storagePoint.setStorageType(StoragePoint.StorageType.EXTERNAL);
                    if (isExternalStorageWritable()) {
                        mCachedStoragePoints.add(storagePoint);
                    }
                }
            }
        }

        // Now we go add private ones
        // Add internal storage directory
        storagePoint = new StoragePoint();
        storagePoint.setDescription(MainApp.getAppContext().getFilesDir().getAbsolutePath());
        storagePoint.setPath(MainApp.getAppContext().getFilesDir().getAbsolutePath());
        storagePoint.setPrivacyType(StoragePoint.PrivacyType.PRIVATE);
        storagePoint.setStorageType(StoragePoint.StorageType.INTERNAL);
            mCachedStoragePoints.add(storagePoint);

        // Add external storage directory if available.
        if (isExternalStorageWritable()) {
            File externalFilesDir = MainApp.getAppContext().getExternalFilesDir(null);

            if (externalFilesDir != null) {
                String externalFilesDirPath = externalFilesDir.getAbsolutePath();

                storagePoint = new StoragePoint();
                storagePoint.setPath(externalFilesDirPath);
                storagePoint.setDescription(externalFilesDirPath);
                storagePoint.setPrivacyType(StoragePoint.PrivacyType.PRIVATE);
                storagePoint.setStorageType(StoragePoint.StorageType.EXTERNAL);
                mCachedStoragePoints.add(storagePoint);
            }
        }

        // Add Downloads subfolder, if the app has permission for external storage
        if (PermissionUtil.checkExternalStoragePermission(MainApp.getAppContext(), true)) {
            File downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            storagePoint = new StoragePoint();
            storagePoint.setPath(downloadsFolder.getAbsolutePath());
            storagePoint.setDescription(downloadsFolder.getAbsolutePath());
            storagePoint.setPrivacyType(StoragePoint.PrivacyType.PUBLIC);
            storagePoint.setStorageType(StoragePoint.StorageType.EXTERNAL);
            mCachedStoragePoints.add(storagePoint);
        }


        return mCachedStoragePoints.toArray(new StoragePoint[0]);
    }

    public String getStorageDescriptionByPath(String path) {
        for (StoragePoint s : getAvailableStoragePoints()) {
            if (s.getPath().equals(path)) {
                return s.getDescription();
            }
        }
        // Fallback to just display complete path
        return path;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
