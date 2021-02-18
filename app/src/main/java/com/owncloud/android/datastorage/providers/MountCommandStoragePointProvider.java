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
import java.util.Locale;
import java.util.Vector;
import java.util.regex.Pattern;

/**
 * @author Bartosz Przybylski
 */
public class MountCommandStoragePointProvider extends AbstractCommandLineStoragePoint {

    static private final String[] sCommand = new String[] { "mount" };

    private static Pattern sPattern = Pattern.compile("(?i).*vold.*(vfat|ntfs|exfat|fat32|ext3|ext4).*rw.*");

    @Override
    protected String[] getCommand() {
        return sCommand;
    }

    @Override
    public List<StoragePoint> getAvailableStoragePoint() {
        List<StoragePoint> result = new Vector<>();

        for (String p : getPotentialPaths(getCommandLineResult())) {
            if (canBeAddedToAvailableList(result, p)) {
                result.add(new StoragePoint(p, p, StoragePoint.StorageType.EXTERNAL, StoragePoint.PrivacyType.PUBLIC));
            }
        }

        return result;
    }

    private List<String> getPotentialPaths(String mounted) {
        final List<String> result = new Vector<>();

        for (String line : mounted.split("\n")) {
            if (!line.toLowerCase(Locale.US).contains("asec") && sPattern.matcher(line).matches()) {
                String parts[] = line.split(" ");
                for (String path : parts) {
                    if (path.length() > 0 && path.charAt(0) == '/' && !path.toLowerCase(Locale.US).contains("vold")) {
                        result.add(path);
                    }
                }
            }
        }
        return result;
    }
}
