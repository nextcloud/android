/*
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
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
