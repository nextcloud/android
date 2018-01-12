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

import android.os.Environment;

import com.owncloud.android.MainApp;
import com.owncloud.android.datastorage.providers.EnvironmentStoragePointProvider;
import com.owncloud.android.datastorage.providers.HardcodedStoragePointProvider;
import com.owncloud.android.datastorage.providers.IStoragePointProvider;
import com.owncloud.android.datastorage.providers.MountCommandStoragePointProvider;
import com.owncloud.android.datastorage.providers.SystemDefaultStoragePointProvider;
import com.owncloud.android.datastorage.providers.VDCStoragePointProvider;

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

        // Add internal storage directory
        mCachedStoragePoints.add(new StoragePoint(MainApp.getAppContext().getFilesDir().getAbsolutePath(),
                MainApp.getAppContext().getFilesDir().getAbsolutePath()));

        // Add external storage directory if available.
        if (isExternalStorageWritable()) {
            mCachedStoragePoints.add(new StoragePoint(
                    MainApp.getAppContext().getExternalFilesDir(null).getAbsolutePath(),
                    MainApp.getAppContext().getExternalFilesDir(null).getAbsolutePath()));
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
