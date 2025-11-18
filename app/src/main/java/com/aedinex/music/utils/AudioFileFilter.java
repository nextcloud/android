/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.utils;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.utils.MimeType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for filtering and identifying audio files.
 * Supports all major audio formats used in music applications.
 */
public final class AudioFileFilter {

    /**
     * Supported audio file extensions
     */
    public static final Set<String> SUPPORTED_AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList(
        "mp3", "m4a", "aac", "flac", "ogg", "opus", "wav", "wma", "webm", "amr", "3gp", "mka"
    ));

    /**
     * High quality audio formats (lossless)
     */
    public static final Set<String> LOSSLESS_AUDIO_FORMATS = new HashSet<>(Arrays.asList(
        "flac", "wav", "alac", "ape", "wv"
    ));

    /**
     * Supported audio MIME types
     */
    public static final Set<String> SUPPORTED_AUDIO_MIME_TYPES = new HashSet<>(Arrays.asList(
        MimeType.AUDIO_MPEG,
        MimeType.AUDIO_MP3,
        MimeType.AUDIO_FLAC,
        MimeType.AUDIO_OGG,
        MimeType.AUDIO_OPUS,
        MimeType.AUDIO_AAC,
        MimeType.AUDIO_M4A,
        MimeType.AUDIO_WAV,
        MimeType.AUDIO_WMA,
        MimeType.AUDIO_WEBM,
        MimeType.AUDIO_AMR,
        MimeType.AUDIO_3GPP
    ));

    private AudioFileFilter() {
        // Utility class - private constructor
    }

    /**
     * Check if a file is an audio file based on its extension
     *
     * @param filename The file name to check
     * @return true if the file has a supported audio extension
     */
    public static boolean isAudioFile(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return false;
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
        return SUPPORTED_AUDIO_EXTENSIONS.contains(extension);
    }

    /**
     * Check if an OCFile is an audio file
     *
     * @param file The OCFile to check
     * @return true if the file is an audio file
     */
    public static boolean isAudioFile(OCFile file) {
        if (file == null || file.isFolder()) {
            return false;
        }

        // Check by MIME type first
        String mimeType = file.getMimeType();
        if (mimeType != null && isAudioMimeType(mimeType)) {
            return true;
        }

        // Fallback to file extension
        return isAudioFile(file.getFileName());
    }

    /**
     * Check if a MIME type is an audio type
     *
     * @param mimeType The MIME type to check
     * @return true if the MIME type represents audio
     */
    public static boolean isAudioMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return false;
        }

        // Check if it starts with "audio/"
        if (mimeType.toLowerCase(Locale.ROOT).startsWith("audio/")) {
            return true;
        }

        // Check against known audio MIME types
        return SUPPORTED_AUDIO_MIME_TYPES.contains(mimeType);
    }

    /**
     * Check if an audio file is lossless quality
     *
     * @param filename The file name to check
     * @return true if the file format is lossless
     */
    public static boolean isLosslessAudio(String filename) {
        if (filename == null || filename.isEmpty()) {
            return false;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return false;
        }

        String extension = filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
        return LOSSLESS_AUDIO_FORMATS.contains(extension);
    }

    /**
     * Get a user-friendly format name for an audio file
     *
     * @param filename The file name
     * @return A formatted string representing the audio format (e.g., "FLAC", "MP3")
     */
    public static String getAudioFormatName(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "Unknown";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "Unknown";
        }

        String extension = filename.substring(lastDotIndex + 1).toUpperCase(Locale.ROOT);

        // Special cases for better display
        switch (extension) {
            case "M4A":
                return "AAC (M4A)";
            case "OGG":
                return "OGG Vorbis";
            case "OPUS":
                return "Opus";
            default:
                return extension;
        }
    }

    /**
     * Get quality indicator for audio file
     *
     * @param filename The file name
     * @return Quality string: "Lossless", "High Quality", or "Standard"
     */
    public static String getAudioQuality(String filename) {
        if (isLosslessAudio(filename)) {
            return "Lossless";
        }

        String ext = getFileExtension(filename);
        if ("opus".equals(ext) || "aac".equals(ext) || "m4a".equals(ext)) {
            return "High Quality";
        }

        return "Standard";
    }

    /**
     * Extract file extension from filename
     *
     * @param filename The file name
     * @return The file extension in lowercase, or empty string if none
     */
    private static String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
