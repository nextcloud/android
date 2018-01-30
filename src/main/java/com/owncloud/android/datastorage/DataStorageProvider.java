/**
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

import android.os.Build;
import android.os.Environment;

import com.owncloud.android.MainApp;
import com.owncloud.android.datastorage.providers.EnvironmentStoragePointProvider;
import com.owncloud.android.datastorage.providers.HardcodedStoragePointProvider;
import com.owncloud.android.datastorage.providers.IStoragePointProvider;
import com.owncloud.android.datastorage.providers.MountCommandStoragePointProvider;
import com.owncloud.android.datastorage.providers.SystemDefaultStoragePointProvider;
import com.owncloud.android.datastorage.providers.VDCStoragePointProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class DataStorageProvider {

    private static final Vector<IStoragePointProvider> mStorageProviders = new Vector<>();
    private static final UniqueStorageList mCachedStoragePoints = new UniqueStorageList();
    private static final DataStorageProvider sInstance = new DataStorageProvider() {{
        // There is no system wide way to get usb storage so we need to provide multiple
        // handcrafted ways to add those.
        addStoragePointProvider(new SystemDefaultStoragePointProvider());
        addStoragePointProvider(new EnvironmentStoragePointProvider());
        addStoragePointProvider(new VDCStoragePointProvider());
        addStoragePointProvider(new MountCommandStoragePointProvider());
        addStoragePointProvider(new HardcodedStoragePointProvider());
    }};


    public static DataStorageProvider getInstance() {
        return sInstance;
    }

    private DataStorageProvider() {}

    public StoragePoint[] getAvailableStoragePoints() {
        if (mCachedStoragePoints.size() != 0) {
            return mCachedStoragePoints.toArray(new StoragePoint[mCachedStoragePoints.size()]);
        }

        List<String> paths = new ArrayList<>();
        StoragePoint storagePoint;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (File f : MainApp.getAppContext().getExternalMediaDirs()) {
                if (f != null && !paths.contains(f.getAbsolutePath())) {
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
        } else {
            for (IStoragePointProvider p : mStorageProviders) {
                if (p.canProvideStoragePoints()) {
                    mCachedStoragePoints.addAll(p.getAvailableStoragePoint());
                }
            }

            for (int i = 0; i < mCachedStoragePoints.size(); i++) {
                paths.add(mCachedStoragePoints.get(i).getPath());
            }
        }

        // Now we go add private ones
        // Add internal storage directory
        storagePoint = new StoragePoint();
        storagePoint.setDescription(MainApp.getAppContext().getFilesDir().getAbsolutePath());
        storagePoint.setPath(MainApp.getAppContext().getFilesDir().getAbsolutePath());
        storagePoint.setPrivacyType(StoragePoint.PrivacyType.PRIVATE);
        storagePoint.setStorageType(StoragePoint.StorageType.INTERNAL);
        if (!paths.contains(MainApp.getAppContext().getFilesDir().getAbsolutePath())) {
            mCachedStoragePoints.add(storagePoint);
        }

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
                if (!paths.contains(externalFilesDirPath)) {
                    mCachedStoragePoints.add(storagePoint);
                }
            }
        }

        return mCachedStoragePoints.toArray(new StoragePoint[mCachedStoragePoints.size()]);
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

    public void addStoragePointProvider(IStoragePointProvider provider) {
        mStorageProviders.add(provider);
    }

    public void removeStoragePointProvider(IStoragePointProvider provider) {
        mStorageProviders.remove(provider);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

}
