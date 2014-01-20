/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.utils;

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
