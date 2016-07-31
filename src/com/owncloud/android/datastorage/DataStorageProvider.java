/**
 *   ownCloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.datastorage;

import android.os.Build;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datastorage.providers.EnvironmentStoragePointProvider;
import com.owncloud.android.datastorage.providers.HardcodedStoragePointProvider;
import com.owncloud.android.datastorage.providers.IStoragePointProvider;
import com.owncloud.android.datastorage.providers.MountCommandStoragePointProvider;
import com.owncloud.android.datastorage.providers.SystemDefaultStoragePointProvider;
import com.owncloud.android.datastorage.providers.VDCStoragePointProvider;

import java.io.File;
import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class DataStorageProvider {

    private static Vector<IStoragePointProvider> mStorageProviders = new Vector<>();
    private static UniqueStorageList mCachedStoragePoints = new UniqueStorageList();
    private static DataStorageProvider sInstance = new DataStorageProvider() {{
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
        if (mCachedStoragePoints.size() != 0)
            return mCachedStoragePoints.toArray(new StoragePoint[mCachedStoragePoints.size()]);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            for (File f : MainApp.getAppContext().getExternalFilesDirs(null)) {
                if (f != null) {
                    mCachedStoragePoints.add(new StoragePoint(f.getAbsolutePath(), f.getAbsolutePath()));
                }
            }
        } else {
            for (IStoragePointProvider p : mStorageProviders)
                if (p.canProvideStoragePoints()) {
                    mCachedStoragePoints.addAll(p.getAvailableStoragePoint());
                }
        }

        return mCachedStoragePoints.toArray(new StoragePoint[mCachedStoragePoints.size()]);
    }

    public String getStorageDescriptionByPath(String path) {
        for (StoragePoint s : getAvailableStoragePoints())
            if (s.getPath().equals(path))
                return s.getDescription();
        return MainApp.getAppContext().getString(R.string.storage_description_unknown);
    }

    public void addStoragePointProvider(IStoragePointProvider provider) {
        mStorageProviders.add(provider);
    }

    public void removeStoragePointProvider(IStoragePointProvider provider) {
        mStorageProviders.remove(provider);
    }
}
