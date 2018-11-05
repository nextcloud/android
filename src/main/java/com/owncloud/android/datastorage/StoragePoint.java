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

package com.owncloud.android.datastorage;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Bartosz Przybylski
 */
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoragePoint implements Comparable<StoragePoint> {
    public enum StorageType {
        INTERNAL, EXTERNAL
    }

    public enum PrivacyType {
        PRIVATE, PUBLIC
    }

    private String description;
    private String path;
    private StorageType storageType;
    private PrivacyType privacyType;

    @Override
    public int compareTo(StoragePoint another) {
        return path.compareTo(another.getPath());
    }
}
