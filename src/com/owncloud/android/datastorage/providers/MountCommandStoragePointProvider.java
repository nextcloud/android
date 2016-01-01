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

import java.util.Locale;
import java.util.Vector;

/**
 * @author Bartosz Przybylski
 */
public class MountCommandStoragePointProvider extends AbstractCommandLineStoragePoint {

    static private final String[] sCommand = new String[] { "mount" };

    @Override
    protected String[] getCommand() {
        return sCommand;
    }

    @Override
    public Vector<StoragePoint> getAvailableStoragePoint() {
        Vector<StoragePoint> result = new Vector<>();

        for (String p : getPotentialPaths(getCommandLineResult()))
            if (canBeAddedToAvailableList(result, p))
                result.add(new StoragePoint(p, p));

        return result;
    }

    private Vector<String> getPotentialPaths(String mounted) {
        final Vector<String> result = new Vector<>();
        final String reg = "(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*";

        for (String line : mounted.split("\n"))
            if (!line.toLowerCase(Locale.US).contains("asec") && line.matches(reg)) {
                String parts[] = line.split(" ");
                for (String path : parts) {
                    if (path.startsWith("/") &&
                            !path.toLowerCase(Locale.US).contains("vold"))
                        result.add(path);
                }
            }
        return result;
    }
}
