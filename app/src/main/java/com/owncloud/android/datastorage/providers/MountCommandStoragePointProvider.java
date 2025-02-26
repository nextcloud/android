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
                String[] parts = line.split(" ");
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
