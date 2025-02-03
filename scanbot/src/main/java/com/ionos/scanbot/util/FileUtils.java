/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileUtils {

	public static final String PATH_SEPARATOR = "/";

	public static final String ROOT_PATH = "/";

	private FileUtils() {
		throw new AssertionError();
	}

	public static boolean isValidName(String text) {
		Pattern pattern = Pattern.compile("^[^/\\\\:*?\"<>|%.][^/\\\\:*?\\\"<>|%]*$");
		Matcher matcher = pattern.matcher(text);

		boolean isMatch = matcher.matches();
		boolean isWhitespaceName = text.matches("^\\s*$");

		return isMatch && !isWhitespaceName;
	}
	public static String extractFileName(String fullPath) {
		if (fullPath == null) {
			return "";
		}

		int separatorPlace = fullPath.lastIndexOf(PATH_SEPARATOR);
		if (separatorPlace != -1) {
			return fullPath.substring(fullPath.lastIndexOf(PATH_SEPARATOR) + 1);
		} else {
			return fullPath;
		}
	}
}