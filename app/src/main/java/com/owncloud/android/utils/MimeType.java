/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
    public static final String PNG = "image/png";
    public static final String WEBP = "image/webp";
    public static final String BMP = "image/bmp";
    public static final String HEIC = "image/heic";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String FILE = "application/octet-stream";
    public static final String PDF = "application/pdf";
    public static final String MP4 = "video/mp4";

    private MimeType() {
        // No instance
    }
}
