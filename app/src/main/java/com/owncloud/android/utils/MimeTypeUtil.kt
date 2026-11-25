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

object MimeTypeUtil {
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

    fun getMimeTypeFromPath(path: String): String {
        val extension = path.substringAfterLast(MimeType.EXTENSION_SEPARATOR, missingDelimiterValue = "").lowercase()

        return getAndroidMimeType(extension) ?: getMimeTypeFromCandidates(determineMimeTypesByExtension(extension))
    }

    @JvmStatic
    fun isMedia(mimeType: String?): Boolean = isImage(mimeType) || isVideo(mimeType) || isAudio(mimeType)

    fun isImageOrVideo(mimeType: String?): Boolean = isImage(mimeType) || isVideo(mimeType)

    fun isImageOrVideo(file: File): Boolean = isImage(file) || isVideo(file)

    fun isImageOrVideo(file: ServerFileInterface): Boolean = isImage(file) || isVideo(file)

    fun isImage(mimeType: String?): Boolean {
        val lowercaseMimeType = mimeType?.lowercase() ?: return false

        return lowercaseMimeType.startsWith(MimeType.IMAGE_PREFIX) && !lowercaseMimeType.contains(MimeType.DJVU)
    }

    fun isImage(file: File): Boolean = isImage(extractMimeType(file))

    @JvmStatic
    fun isImage(file: ServerFileInterface): Boolean = file.matchesMimeType { isImage(it) }

    @JvmStatic
    fun isVideo(mimeType: String?): Boolean = mimeType.hasLowercasePrefix(MimeType.VIDEO_PREFIX)

    fun isVideo(file: File): Boolean = isVideo(extractMimeType(file))

    @JvmStatic
    fun isVideo(file: ServerFileInterface): Boolean = isVideo(file.mimeType)

    fun isAudio(mimeType: String?): Boolean = mimeType.hasLowercasePrefix(MimeType.AUDIO_PREFIX)

    fun isAudio(file: OCFile): Boolean = isAudio(file.mimeType)

    fun isText(file: OCFile): Boolean = file.matchesMimeType { it.hasLowercasePrefix(MimeType.TEXT_PREFIX) }

    fun isVCard(mimeType: String?): Boolean = MimeType.VCARD.equals(mimeType, ignoreCase = true)

    @JvmStatic
    fun isVCard(file: OCFile): Boolean = file.matchesMimeType { isVCard(it) }

    fun isCalendar(mimeType: String?): Boolean = MimeType.CALENDAR.equals(mimeType, ignoreCase = true)

    @JvmStatic
    fun isCalendar(file: OCFile): Boolean = file.matchesMimeType { isCalendar(it) }

    @JvmStatic
    fun isPDF(mimeType: String?): Boolean = MimeType.PDF.equals(mimeType, ignoreCase = true)

    fun isPDF(file: OCFile): Boolean = file.matchesMimeType { isPDF(it) }

    fun isSVG(file: OCFile): Boolean = MimeType.SVG.equals(file.mimeType, ignoreCase = true)

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
        mimeTypes.firstNotNullOfOrNull { MimeType.iconByMimeType[it] }
            ?: mimeTypes.firstNotNullOfOrNull {
                MimeType.iconByMainMimeType[it.substringBefore(MimeType.MIME_TYPE_SEPARATOR)]
            }
            ?: R.drawable.file

    private fun determineMimeTypesByFilename(filename: String): List<String> =
        determineMimeTypesByExtension(filename.substringAfterLast(MimeType.EXTENSION_SEPARATOR).lowercase())

    private fun determineMimeTypesByExtension(extension: String): List<String> =
        MimeType.mimeTypesByExtension[extension]
            ?: getAndroidMimeType(extension)?.let { listOf(it) }
            ?: emptyList()

    private fun getMimeTypeFromCandidates(candidates: List<String>): String = candidates.firstOrNull() ?: MimeType.FILE

    private fun getAndroidMimeType(extension: String?): String? =
        MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
}
