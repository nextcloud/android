/*
 * Nextcloud Android client application
 *
 * @author Bartosz Przybylski
 * Copyright (C) 2016 Nextcloud
 * Copyright (C) 2016 Bartosz Przybylski
 *
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */

package com.owncloud.android.datastorage.providers;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datastorage.StoragePoint;

import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class SystemDefaultStoragePointProvider extends AbstractStoragePointProvider {
    @Override
    public boolean canProvideStoragePoints() {
        return true;
    }

    @Override
    public Vector<StoragePoint> getAvailableStoragePoint() {
        Vector<StoragePoint> result = new Vector<>();

        final String defaultStringDesc = MainApp.string(R.string.storage_description_default);
        // Add private internal storage data directory.
        result.add(new StoragePoint(defaultStringDesc,
                MainApp.getAppContext().getFilesDir().getAbsolutePath(), StoragePoint.StorageType.INTERNAL,
                StoragePoint.PrivacyType.PRIVATE));

        return result;
    }
}
