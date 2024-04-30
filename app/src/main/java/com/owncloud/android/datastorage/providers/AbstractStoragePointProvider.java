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

import com.owncloud.android.datastorage.StoragePoint;

import java.io.File;

/**
 * @author Bartosz Przybylski
 */
abstract class AbstractStoragePointProvider implements IStoragePointProvider {

    boolean canBeAddedToAvailableList(Iterable<StoragePoint> currentList, String path) {
        if (path == null) {
            return false;
        }

        for (StoragePoint storage : currentList) {
            if (storage.getPath().equals(path)) {
                return false;
            }
        }

        File f = new File(path);
        return f.exists() && f.isDirectory() && f.canRead() && f.canWrite();
    }
}
