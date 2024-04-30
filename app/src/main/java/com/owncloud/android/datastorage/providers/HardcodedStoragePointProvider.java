/**
 *   Nextcloud Android client application
 *
 *   @author Bartosz Przybylski
 *   Copyright (C) 2016 Nextcloud
 *   Copyright (C) 2016 Bartosz Przybylski
 *
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */

package com.owncloud.android.datastorage.providers;

import com.owncloud.android.datastorage.StoragePoint;

import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class HardcodedStoragePointProvider extends AbstractStoragePointProvider {

    private static final String[] PATHS = {
            "/mnt/external_sd/",
            "/mnt/extSdCard/",
            "/storage/extSdCard",
            "/storage/sdcard1/",
            "/storage/usbcard1/"
    };

    @Override
    public boolean canProvideStoragePoints() {
        return true;
    }

    @Override
    public Vector<StoragePoint> getAvailableStoragePoint() {
        Vector<StoragePoint> result = new Vector<>();

        for (String s : PATHS) {
            if (canBeAddedToAvailableList(result, s)) {
                result.add(new StoragePoint(s, s, StoragePoint.StorageType.EXTERNAL, StoragePoint.PrivacyType.PUBLIC));
            }
        }

        return result;
    }
}
