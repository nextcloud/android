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
	public static boolean validateName(String fileName, boolean isFolder) {
		boolean result = true;
		
		Log.d("FileUtils", "fileName ======= " + fileName);
		String name = fileName.substring(1);
		if (isFolder) {
			name = name.substring(0, name.length() - 1);
		}
		if (name.contains("/") || fileName.contains("\\") || fileName.contains("<") || 
				fileName.contains(">") || fileName.contains(":") || fileName.contains("\"") || 
				fileName.contains("|") || fileName.contains("?") || fileName.contains("*")) {
			result = false;
		}
		return result;
	}
}
