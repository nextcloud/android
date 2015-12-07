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
import java.util.ArrayList;
import java.util.List;

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
		// Android 6.0 and later changed some internal implementation of accessing storage
		// so for now we return only single path named "external"
		// TODO(przybylski): add 6.0 code
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
			return new Storage[] {
				new Storage(context.getString(R.string.storage_description_external),
						    Environment.getExternalStorageDirectory().getAbsolutePath())
			};
		}

		List<Storage> list = new ArrayList<>();

		String storagePath = Environment.getExternalStorageDirectory().getPath();
		if (canBeAddedToAvailableList(storagePath))
			list.add(new Storage(context.getString(R.string.storage_description_external),
					 			 storagePath));

		storagePath = System.getenv("SECONDARY_STORAGE"); // : separated paths to sd cards
		list.addAll(getSDCardStorage(storagePath, context));

		//list.add(new Storage("Costam", Environment.getExternalStorageDirectory().getAbsolutePath() + "/costam"));

		return list.toArray(new Storage[list.size()]);
	}

	static public String getStorageDescriptionByPath(String path, Context context) {
		Storage[] storages = getAvailableStoragePoints(context);
		for (Storage s : storages) {
			if (s.getPath().equals(path))
				return s.getDescription();
		}
		return context.getString(R.string.storage_description_unknown);
	}

	private static List<Storage> getSDCardStorage(String storagePath, Context context) {
		if (storagePath == null) return new ArrayList<>();

		String[] paths = storagePath.split(":");
		List<Storage> list = new ArrayList<>();
		if (paths.length == 1)
			list.add(new Storage(context.getString(R.string.storage_description_sd), paths[0]));
		else
			for (int i = 0; i < paths.length; ++i) {
				if (canBeAddedToAvailableList(paths[i]))
					list.add(new Storage(context.getString(R.string.storage_description_sd_no, i+1),
							paths[i]));
			}
		return list;
	}

	private static boolean canBeAddedToAvailableList(String path) {
		if (path == null) return false;
		File f = new File(path);
		return f.exists() && f.isDirectory();
	}
}
