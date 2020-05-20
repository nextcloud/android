/*
 * Nextcloud Android client application
 *
 * Copyright (C) 2016 Nextcloud
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2+,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

/**
 * Class containing the mime types.
 */
public final class MimeType {
    public static final String DIRECTORY = "DIR";
    public static final String WEBDAV_FOLDER = "httpd/unix-directory";
    public static final String JPEG = "image/jpeg";
    public static final String TIFF = "image/tiff";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String FILE = "application/octet-stream";

    private MimeType() {
        // No instance
    }
}
