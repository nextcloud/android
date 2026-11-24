/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File

/**
 * Detects the right icon for a file or folder, based on its mime type and file extension.
 *
 * To add further mappings, extend:
 * 1. [FileExtensionMimeTypeMapping.byExtension] for a new file extension to mime type mapping
 * 2. [MimeTypeIconMapping.byMimeType] for a new mime type to icon mapping
 * 3. [MimeTypeIconMapping.byMainMimeType] for a fallback icon of the main part of a mime type
 */
object MimeTypeUtil {
    const val MIMETYPE_TEXT_MARKDOWN = "text/markdown"

    private const val MIME_TYPE_SEPARATOR = '/'
    private const val EXTENSION_SEPARATOR = '.'
    private const val IMAGE_PREFIX = "image/"
    private const val VIDEO_PREFIX = "video/"
    private const val AUDIO_PREFIX = "audio/"
    private const val TEXT_PREFIX = "text/"
    private const val DJVU = "djvu"
    private const val VCARD = "text/vcard"
    private const val CALENDAR = "text/calendar"
    private const val SVG = "image/svg+xml"

    @JvmStatic
    fun getFileTypeIcon(
        mimetype: String?,
        filename: String,
        context: Context,
        viewThemeUtils: ViewThemeUtils
    ): Drawable? {
        val iconId = getFileTypeIconId(mimetype, filename)

        return ContextCompat.getDrawable(context, iconId)
            ?.mutate()
            ?.tintIfZip(iconId, context, viewThemeUtils)
    }

    /**
     * @param mimetype if null, the mime type is guessed from the extension of [filename]
     */
    @JvmStatic
    fun getFileTypeIconId(mimetype: String?, filename: String): Int {
        val possibleMimeTypes = mimetype?.let { listOf(it) } ?: determineMimeTypesByFilename(filename)

        return determineIconIdByMimeTypes(possibleMimeTypes)
    }

    @JvmStatic
    fun getDefaultFolderIcon(context: Context, viewThemeUtils: ViewThemeUtils): Drawable {
        val drawable = checkNotNull(ContextCompat.getDrawable(context, R.drawable.folder))
        viewThemeUtils.platform.tintDrawable(context, drawable, ColorRole.PRIMARY)

        return drawable
    }

    @JvmStatic
    fun getFolderIcon(
        isDarkModeActive: Boolean,
        overlayIconId: Int?,
        context: Context,
        viewThemeUtils: ViewThemeUtils
    ): LayerDrawable {
        val folderDrawable = getDefaultFolderIcon(context, viewThemeUtils)
        if (overlayIconId == null) return LayerDrawable(arrayOf(folderDrawable))

        val overlayDrawable = checkNotNull(ContextCompat.getDrawable(context, overlayIconId))
        val themedOverlayDrawable = if (isDarkModeActive) {
            DrawableUtil.changeColor(overlayDrawable, R.color.dark)
        } else {
            overlayDrawable
        }

        return DrawableUtil.addDrawableAsOverlay(folderDrawable, themedOverlayDrawable)
    }

    /**
     * Picks the first candidate mime type of the file extension, preferring the ones known by Nextcloud.
     *
     * @return [MimeType.FILE] for unknown file extensions
     */
    @JvmStatic
    fun getBestMimeTypeByFilename(filename: String): String =
        getMimeTypeFromCandidates(determineMimeTypesByFilename(filename))

    @JvmStatic
    fun getMimeTypeFromPath(path: String): String {
        val extension = path.substringAfterLast(EXTENSION_SEPARATOR, missingDelimiterValue = "").lowercase()

        return getAndroidMimeType(extension) ?: getMimeTypeFromCandidates(determineMimeTypesByExtension(extension))
    }

    @JvmStatic
    fun isMedia(mimeType: String?): Boolean = isImage(mimeType) || isVideo(mimeType) || isAudio(mimeType)

    @JvmStatic
    fun isImageOrVideo(mimeType: String?): Boolean = isImage(mimeType) || isVideo(mimeType)

    @JvmStatic
    fun isImageOrVideo(file: File): Boolean = isImage(file) || isVideo(file)

    @JvmStatic
    fun isImageOrVideo(file: ServerFileInterface): Boolean = isImage(file) || isVideo(file)

    @JvmStatic
    fun isImage(mimeType: String?): Boolean {
        val lowercaseMimeType = mimeType?.lowercase() ?: return false

        return lowercaseMimeType.startsWith(IMAGE_PREFIX) && !lowercaseMimeType.contains(DJVU)
    }

    @JvmStatic
    fun isImage(file: File): Boolean = isImage(extractMimeType(file))

    @JvmStatic
    fun isImage(file: ServerFileInterface): Boolean = file.matchesMimeType { isImage(it) }

    @JvmStatic
    fun isVideo(mimeType: String?): Boolean = mimeType.hasLowercasePrefix(VIDEO_PREFIX)

    @JvmStatic
    fun isVideo(file: File): Boolean = isVideo(extractMimeType(file))

    @JvmStatic
    fun isVideo(file: ServerFileInterface): Boolean = isVideo(file.mimeType)

    @JvmStatic
    fun isAudio(mimeType: String?): Boolean = mimeType.hasLowercasePrefix(AUDIO_PREFIX)

    @JvmStatic
    fun isAudio(file: OCFile): Boolean = isAudio(file.mimeType)

    /**
     * @return true if the file is simple text, i.e. not application-dependent like .doc or .docx
     */
    @JvmStatic
    fun isText(file: OCFile): Boolean = file.matchesMimeType { it.hasLowercasePrefix(TEXT_PREFIX) }

    @JvmStatic
    fun isVCard(mimeType: String?): Boolean = VCARD.equals(mimeType, ignoreCase = true)

    @JvmStatic
    fun isVCard(file: OCFile): Boolean = file.matchesMimeType { isVCard(it) }

    @JvmStatic
    fun isCalendar(mimeType: String?): Boolean = CALENDAR.equals(mimeType, ignoreCase = true)

    @JvmStatic
    fun isCalendar(file: OCFile): Boolean = file.matchesMimeType { isCalendar(it) }

    @JvmStatic
    fun isPDF(mimeType: String?): Boolean = MimeType.PDF.equals(mimeType, ignoreCase = true)

    @JvmStatic
    fun isPDF(file: OCFile): Boolean = file.matchesMimeType { isPDF(it) }

    @JvmStatic
    fun isSVG(file: OCFile): Boolean = SVG.equals(file.mimeType, ignoreCase = true)

    @JvmStatic
    fun isFolder(mimeType: String?): Boolean = MimeType.DIRECTORY.equals(mimeType, ignoreCase = true)

    private fun Drawable.tintIfZip(iconId: Int, context: Context, viewThemeUtils: ViewThemeUtils): Drawable = apply {
        if (iconId == R.drawable.file_zip) {
            viewThemeUtils.platform.tintDrawable(context, this, ColorRole.PRIMARY)
        }
    }

    private fun String?.hasLowercasePrefix(prefix: String): Boolean = this?.lowercase()?.startsWith(prefix) == true

    private inline fun ServerFileInterface.matchesMimeType(predicate: (String?) -> Boolean): Boolean =
        predicate(mimeType) || predicate(getMimeTypeFromPath(remotePath))

    private fun extractMimeType(file: File): String? {
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString().lowercase())

        return getAndroidMimeType(fileExtension)
    }

    private fun determineIconIdByMimeTypes(mimeTypes: List<String>): Int =
        mimeTypes.firstNotNullOfOrNull { MimeTypeIconMapping.byMimeType[it] }
            ?: mimeTypes.firstNotNullOfOrNull {
                MimeTypeIconMapping.byMainMimeType[it.substringBefore(MIME_TYPE_SEPARATOR)]
            }
            ?: R.drawable.file

    private fun determineMimeTypesByFilename(filename: String): List<String> =
        determineMimeTypesByExtension(filename.substringAfterLast(EXTENSION_SEPARATOR).lowercase())

    private fun determineMimeTypesByExtension(extension: String): List<String> =
        FileExtensionMimeTypeMapping.byExtension[extension]
            ?: getAndroidMimeType(extension)?.let { listOf(it) }
            ?: emptyList()

    private fun getMimeTypeFromCandidates(candidates: List<String>): String = candidates.firstOrNull() ?: MimeType.FILE

    private fun getAndroidMimeType(extension: String?): String? =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}
