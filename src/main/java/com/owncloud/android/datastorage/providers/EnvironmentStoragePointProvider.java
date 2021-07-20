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

package com.owncloud.android.datastorage.providers;

import android.text.TextUtils;

import com.owncloud.android.datastorage.StoragePoint;

import java.util.List;
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
    public List<StoragePoint> getAvailableStoragePoint() {
        List<StoragePoint> result = new Vector<>();

        addEntriesFromEnv(result, sSecondaryStorageEnvName);

        return result;
    }

    private void addEntriesFromEnv(List<StoragePoint> result, String envName) {
        String env = System.getenv(envName);
        if (env != null) {
            for (String p : env.split(":")) {
                if (canBeAddedToAvailableList(result, p)) {
                    result.add(new StoragePoint(p, p, StoragePoint.StorageType.EXTERNAL,
                            StoragePoint.PrivacyType.PUBLIC));
                }
            }
        }
    }
}
