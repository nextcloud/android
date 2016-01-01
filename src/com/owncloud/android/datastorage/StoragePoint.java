/**
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   Copyright (C) 2016  Bartosz Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
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

package com.owncloud.android.datastorage;

/**
 * @author Bartosz Przybylski
 */
public class StoragePoint implements Comparable<StoragePoint> {
    private String mDescription;
    private String mPath;

    public StoragePoint(String description, String path) {
        mDescription = description;
        mPath = path;
    }

    public String getPath() { return mPath; }
    public String getDescription() { return mDescription; }

    @Override
    public int compareTo(StoragePoint another) {
        return mPath.compareTo(another.getPath());
    }
}
