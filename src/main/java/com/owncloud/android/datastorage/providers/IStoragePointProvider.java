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

import com.owncloud.android.datastorage.StoragePoint;

import java.util.List;

/**
 * @author Bartosz Przybylski
 */
public interface IStoragePointProvider {

    /**
     *  This method is used for querying storage provider to check if it can provide
     *  usable and reliable data storage places.
     *
     *  @return true if provider can reliably return storage path
     */
    boolean canProvideStoragePoints();

    /**
     *
     * @return available storage points
     */
    List<StoragePoint> getAvailableStoragePoint();
}
