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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class UniqueStorageList extends Vector<StoragePoint> {
    @Override
    public boolean add(StoragePoint sp) {
        try {
            for (StoragePoint s : this) {
                String thisCanonPath = new File(s.getPath()).getCanonicalPath();
                String otherCanonPath = new File(sp.getPath()).getCanonicalPath();
                if (thisCanonPath.equals(otherCanonPath)) {
                    return true;
                }
            }
        } catch (IOException e) {
            return false;
        }
        return super.add(sp);
    }

    @Override
    public synchronized boolean addAll(Collection<? extends StoragePoint> collection) {
        for (StoragePoint sp : collection) {
            add(sp);
        }
        return true;
    }
}
