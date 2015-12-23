/**
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   Copyright (C) 2015  Bartosz Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.utils;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.owncloud.android.R;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Bartosz Przybylski
 */
public class DataStorageUtils {
	static public final class Storage {
		private String mDescription;
		private String mPath;

		Storage(String description, String path) {
			mDescription = description;
			mPath = path;
		}

		public String getPath() { return mPath; }
		public String getDescription() { return mDescription; }
	}

	static public Storage[] getAvailableStoragePoints(Context context) {
        List<Storage> result = new ArrayList<>();

        String storagePath = Environment.getExternalStorageDirectory().getPath();
        if (canBeAddedToAvailableList(result, storagePath))
            result.add(new Storage(context.getString(R.string.storage_description_default),
                    storagePath));


        int cardNo = 1;

        for (String potentialPath : getPotentialPaths(getMountedResources()))
            if (canBeAddedToAvailableList(result, potentialPath))
                result.add(new Storage(constructSDCardName(context, cardNo++), potentialPath));

        return result.toArray(new Storage[result.size()]);
	}

	public static String getStorageDescriptionByPath(String path, Context context) {
		Storage[] storages = getAvailableStoragePoints(context);
		for (Storage s : storages) {
			if (s.getPath().equals(path))
				return s.getDescription();
		}
		return context.getString(R.string.storage_description_unknown);
	}

    private static String constructSDCardName(Context context, int no) {
        return context.getString(R.string.storage_description_sd_no, no);
    }

    private static String getMountedResources() {
        String s = "";
        try {
            final Process process = new ProcessBuilder().command("mount")
                    .redirectErrorStream(true).start();

            process.waitFor();
            final InputStream is = process.getInputStream();
            final byte buffer[] = new byte[1024];
            while (is.read(buffer) != -1)
                s += new String(buffer);
            is.close();
        } catch (final Exception e) { }
        return s;
    }

    private static String[] getPotentialPaths(String mounted) {
        final List<String> result = new ArrayList<String>();
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
        return result.toArray(new String[result.size()]);
    }

	private static boolean canBeAddedToAvailableList(List<Storage> currentList, String path) {
		if (path == null) return false;
        for (Storage storage : currentList)
            if (storage.getPath().equals(path))
                return false;
		File f = new File(path);
		return f.exists() && f.isDirectory();
	}
}
