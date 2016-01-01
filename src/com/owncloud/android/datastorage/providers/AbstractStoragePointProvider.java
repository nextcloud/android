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

import com.owncloud.android.datastorage.StoragePoint;

import java.io.File;
import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
abstract public class AbstractStoragePointProvider implements IStoragePointProvider {

    protected boolean canBeAddedToAvailableList(Vector<StoragePoint> currentList, String path) {
        if (path == null) return false;
        for (StoragePoint storage : currentList)
            if (storage.getPath().equals(path))
                return false;
        File f = new File(path);
        return f.exists() && f.isDirectory() && f.canRead() && f.canWrite();
    }
}
