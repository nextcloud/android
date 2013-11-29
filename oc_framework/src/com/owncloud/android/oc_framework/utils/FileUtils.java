/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
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

package com.owncloud.android.oc_framework.utils;

import java.io.File;

import android.util.Log;

public class FileUtils {

	public static final String PATH_SEPARATOR = "/";


	public static String getParentPath(String remotePath) {
		String parentPath = new File(remotePath).getParent();
		parentPath = parentPath.endsWith(PATH_SEPARATOR) ? parentPath : parentPath + PATH_SEPARATOR;
		return parentPath;
	}
	
	/**
	 * Validate the fileName to detect if contains any forbidden character: / , \ , < , > , : , " , | , ? , *
	 * @param fileName
	 * @return
	 */
	public static boolean isValidName(String fileName) {
		boolean result = true;
		
		Log.d("FileUtils", "fileName =======" + fileName);
		if (fileName.contains(PATH_SEPARATOR) ||
				fileName.contains("\\") || fileName.contains("<") || fileName.contains(">") ||
				fileName.contains(":") || fileName.contains("\"") || fileName.contains("|") || 
				fileName.contains("?") || fileName.contains("*")) {
			result = false;
		}
		return result;
	}
	
	/**
	 * Validate the path to detect if contains any forbidden character: \ , < , > , : , " , | , ? , *
	 * @param path
	 * @return
	 */
	public static boolean isValidPath(String path) {
		boolean result = true;
		
		Log.d("FileUtils", "path ....... " + path);
		if (path.contains("\\") || path.contains("<") || path.contains(">") ||
				path.contains(":") || path.contains("\"") || path.contains("|") || 
				path.contains("?") || path.contains("*")) {
			result = false;
		}
		return result;
	}
}
