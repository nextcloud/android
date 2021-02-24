/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2012 Bartek Przybylski
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.authentication;

import android.text.TextUtils;

import java.util.Locale;

/**
 * Helper class for authenticator-URL related logic.
 */
public final class AuthenticatorUrlUtils {
    public static final String WEBDAV_PATH_4_0_AND_LATER = "/remote.php/webdav";

    private static final String HTTPS_PROTOCOL = "https://";
    private static final String HTTP_PROTOCOL = "http://";

    private AuthenticatorUrlUtils() {
    }

    public static String normalizeUrlSuffix(String url) {
        String normalizedUrl = url;
        if (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }
        return trimUrlWebdav(normalizedUrl);
    }

    public static String normalizeUrl(String url, boolean sslWhenUnprefixed) {
        String normalizedUrl = url;

        if (!TextUtils.isEmpty(normalizedUrl)) {
            normalizedUrl = normalizedUrl.trim();

            if (!normalizedUrl.toLowerCase(Locale.ROOT).startsWith(HTTP_PROTOCOL) &&
                    !normalizedUrl.toLowerCase(Locale.ROOT).startsWith(HTTPS_PROTOCOL)) {
                if (sslWhenUnprefixed) {
                    normalizedUrl = HTTPS_PROTOCOL + normalizedUrl;
                } else {
                    normalizedUrl = HTTP_PROTOCOL + normalizedUrl;
                }
            }

            normalizedUrl = normalizeUrlSuffix(normalizedUrl);
        }
        return normalizedUrl != null ? normalizedUrl : "";
    }

    public static String trimWebdavSuffix(String url) {
        String trimmedUrl = url;
        while (trimmedUrl.endsWith("/")) {
            trimmedUrl = trimmedUrl.substring(0, url.length() - 1);
        }

        int pos = trimmedUrl.lastIndexOf(WEBDAV_PATH_4_0_AND_LATER);
        if (pos >= 0) {
            trimmedUrl = trimmedUrl.substring(0, pos);

        }
        return trimmedUrl;
    }

    private static String trimUrlWebdav(String url) {
        if (url.toLowerCase(Locale.ROOT).endsWith(WEBDAV_PATH_4_0_AND_LATER)) {
            return url.substring(0, url.length() - WEBDAV_PATH_4_0_AND_LATER.length());
        }

        return url;
    }

    public static String stripIndexPhpOrAppsFiles(String url) {
        String strippedUrl = url;
        if (strippedUrl.endsWith("/index.php")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.lastIndexOf("/index.php"));
        } else if (strippedUrl.contains("/index.php/apps/")) {
            strippedUrl = strippedUrl.substring(0, strippedUrl.lastIndexOf("/index.php/apps/"));
        }

        return strippedUrl;
    }
}
