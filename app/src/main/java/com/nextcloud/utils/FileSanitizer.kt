/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.extensions.StringConstants
import com.owncloud.android.datamodel.OCFile

/**
 * Utility class for sanitizing file and folder names to ensure they are safe
 * for use across different file systems and platforms.
 *
 * This handles common issues like:
 * - Control characters and non-printable Unicode
 * - Reserved characters on Windows/Linux/macOS
 * - Leading/trailing whitespace and dots
 * - Path traversal sequences
 * - Excessively long filenames
 */
object FileSanitizer {

    private const val MAX_FILENAME_LENGTH = 250
    private const val REPLACEMENT_CHAR = "_"

    /**
     * Characters that are generally unsafe across file systems (Windows, Linux, macOS).
     */
    private val UNSAFE_CHARACTERS = charArrayOf(
        '<', '>', ':', '"', '|', '?', '*',
        '\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0005', '\u0006', '\u0007',
        '\u0008', '\u0009', '\u000A', '\u000B', '\u000C', '\u000D', '\u000E', '\u000F',
        '\u0010', '\u0011', '\u0012', '\u0013', '\u0014', '\u0015', '\u0016', '\u0017',
        '\u0018', '\u0019', '\u001A', '\u001B', '\u001C', '\u001D', '\u001E', '\u001F'
    )

    /**
     * Reserved filenames on Windows that cannot be used regardless of extension.
     */
    private val WINDOWS_RESERVED_NAMES = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM0", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "LPT0", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"
    )

    /**
     * Sanitizes a filename by removing or replacing unsafe characters.
     *
     * @param filename The filename to sanitize (without path separators).
     * @return A sanitized filename safe for use across file systems.
     */
    fun sanitizeFilename(filename: String): String {
        if (filename.isBlank()) {
            return REPLACEMENT_CHAR
        }

        var sanitized = filename

        // Replace unsafe characters
        for (unsafeChar in UNSAFE_CHARACTERS) {
            sanitized = sanitized.replace(unsafeChar.toString(), REPLACEMENT_CHAR)
        }

        // Remove path separators that shouldn't be in a filename
        sanitized = sanitized.replace("/", REPLACEMENT_CHAR)
        sanitized = sanitized.replace("\\", REPLACEMENT_CHAR)

        // Strip leading and trailing dots and spaces
        sanitized = sanitized.trimStart('.', ' ').trimEnd('.', ' ')

        // Handle Windows reserved names
        if (isWindowsReservedName(sanitized)) {
            sanitized = "${REPLACEMENT_CHAR}$sanitized"
        }

        // Truncate if too long
        sanitized = truncateFilename(sanitized, MAX_FILENAME_LENGTH)

        // If everything was stripped, return a placeholder
        return sanitized.ifBlank { REPLACEMENT_CHAR }
    }

    /**
     * Sanitizes a full remote path by sanitizing each segment individually.
     *
     * @param remotePath The full remote path (e.g., "/folder/subfolder/file.txt").
     * @return A sanitized path with each segment cleaned.
     */
    fun sanitizeRemotePath(remotePath: String): String {
        if (remotePath.isBlank()) {
            return OCFile.PATH_SEPARATOR
        }

        val segments = remotePath.split(OCFile.PATH_SEPARATOR)
        val sanitizedSegments = segments.map { segment ->
            if (segment.isEmpty()) segment else sanitizeFilename(segment)
        }

        val result = sanitizedSegments.joinToString(OCFile.PATH_SEPARATOR)
        return result.ifBlank { OCFile.PATH_SEPARATOR }
    }

    /**
     * Checks whether the given filename contains path traversal sequences.
     *
     * @param filename The filename or path to check.
     * @return true if path traversal is detected, false otherwise.
     */
    fun containsPathTraversal(filename: String): Boolean {
        if (filename.isEmpty()) return false

        val normalized = filename.replace("\\", "/")
        val segments = normalized.split("/")

        return segments.any { segment ->
            segment == ".." || segment == "." && segments.size > 1
        }
    }

    /**
     * Checks if the given name (without extension) is a Windows reserved name.
     *
     * @param name The filename to check (with or without extension).
     * @return true if the base name is a reserved Windows name.
     */
    fun isWindowsReservedName(name: String): Boolean {
        val baseName = name.substringBefore(StringConstants.DOT).uppercase()
        return baseName in WINDOWS_RESERVED_NAMES
    }

    /**
     * Truncates a filename to the specified maximum length, preserving the file extension.
     *
     * @param filename The filename to truncate.
     * @param maxLength The maximum allowed length.
     * @return The truncated filename with extension preserved if possible.
     */
    fun truncateFilename(filename: String, maxLength: Int): String {
        if (filename.length <= maxLength) return filename

        val dotIndex = filename.lastIndexOf('.')
        if (dotIndex <= 0) {
            return filename.take(maxLength)
        }

        val extension = filename.substring(dotIndex)
        val baseName = filename.substring(0, dotIndex)

        val availableLength = maxLength - extension.length
        return if (availableLength > 0) {
            baseName.take(availableLength) + extension
        } else {
            filename.take(maxLength)
        }
    }

    /**
     * Checks whether a filename contains any Unicode bidirectional control characters
     * that could be used for filename spoofing attacks.
     *
     * @param filename The filename to check.
     * @return true if bidi control characters are found.
     */
    fun containsBidiCharacters(filename: String): Boolean {
        val bidiCodePoints = intArrayOf(
            0x200E, 0x200F, // LRM, RLM
            0x202A, 0x202B, 0x202C, 0x202D, 0x202E, // LRE, RLE, PDF, LRO, RLO
            0x2066, 0x2067, 0x2068, 0x2069, // LRI, RLI, FSI, PDI
            0x061C // ALM
        )

        for (i in filename.indices) {
            val codePoint = filename.codePointAt(i)
            if (codePoint in bidiCodePoints) {
                return true
            }
        }
        return false
    }

    /**
     * Removes all Unicode bidirectional control characters from a filename.
     *
     * @param filename The filename to clean.
     * @return The filename with bidi characters removed.
     */
    fun removeBidiCharacters(filename: String): String {
        val bidiCodePoints = setOf(
            0x200E, 0x200F,
            0x202A, 0x202B, 0x202C, 0x202D, 0x202E,
            0x2066, 0x2067, 0x2068, 0x2069,
            0x061C
        )

        val sb = StringBuilder(filename.length)
        var i = 0
        while (i < filename.length) {
            val codePoint = filename.codePointAt(i)
            if (codePoint !in bidiCodePoints) {
                sb.appendCodePoint(codePoint)
            }
            i += Character.charCount(codePoint)
        }
        return sb.toString()
    }

    /**
     * Generates a unique filename by appending a numeric suffix if the name already exists.
     *
     * @param desiredName The desired filename.
     * @param existingNames Set of existing filenames in the same directory.
     * @return A unique filename that does not collide with existing names.
     */
    fun generateUniqueFilename(desiredName: String, existingNames: Set<String>): String {
        if (desiredName !in existingNames) return desiredName

        val dotIndex = desiredName.lastIndexOf('.')
        val baseName: String
        val extension: String

        if (dotIndex > 0) {
            baseName = desiredName.substring(0, dotIndex)
            extension = desiredName.substring(dotIndex)
        } else {
            baseName = desiredName
            extension = ""
        }

        var counter = 2
        var candidate = "$baseName ($counter)$extension"
        while (candidate in existingNames) {
            counter++
            candidate = "$baseName ($counter)$extension"
        }
        return candidate
    }
}
