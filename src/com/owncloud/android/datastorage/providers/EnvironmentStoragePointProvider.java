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

package com.owncloud.android.datastorage.providers;

import android.text.TextUtils;

import com.owncloud.android.datastorage.StoragePoint;

import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class EnvironmentStoragePointProvider extends AbstractStoragePointProvider {

    private static final String sSecondaryStorageEnvName = "SECONDARY_STORAGE";

    @Override
    public boolean canProvideStoragePoints() {
        return !TextUtils.isEmpty(System.getenv(sSecondaryStorageEnvName));
    }

    @Override
    public Vector<StoragePoint> getAvailableStoragePoint() {
        Vector<StoragePoint> result = new Vector<>();

        addEntriesFromEnv(result, sSecondaryStorageEnvName);

        return result;
    }

    private void addEntriesFromEnv(Vector<StoragePoint> result, String envName) {
        String env = System.getenv(envName);
        if (env != null)
            for (String p : env.split(":"))
                if (canBeAddedToAvailableList(result, p))
                    result.add(new StoragePoint(p, p));
    }
}
