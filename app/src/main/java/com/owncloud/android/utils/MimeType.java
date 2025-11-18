/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
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

    // Audio MIME types - AediNex Music
    public static final String AUDIO = "audio/*";
    public static final String AUDIO_MPEG = "audio/mpeg";
    public static final String AUDIO_MP3 = "audio/mp3";
    public static final String AUDIO_FLAC = "audio/flac";
    public static final String AUDIO_OGG = "audio/ogg";
    public static final String AUDIO_OPUS = "audio/opus";
    public static final String AUDIO_AAC = "audio/aac";
    public static final String AUDIO_M4A = "audio/mp4";
    public static final String AUDIO_WAV = "audio/wav";
    public static final String AUDIO_WMA = "audio/x-ms-wma";
    public static final String AUDIO_WEBM = "audio/webm";
    public static final String AUDIO_AMR = "audio/amr";
    public static final String AUDIO_3GPP = "audio/3gpp";

    private MimeType() {
        // No instance
    }
}
