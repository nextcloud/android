/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.owncloud.android.R
import com.owncloud.android.utils.theme.ViewThemeUtils

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

    @JvmStatic
    fun isImage(mimeType: String?): Boolean {
        val lowercaseMimeType = mimeType?.lowercase() ?: return false

        return lowercaseMimeType.startsWith(MimeType.IMAGE_PREFIX) && !lowercaseMimeType.contains(MimeType.DJVU)
    }

    @JvmStatic
    fun isVideo(mimeType: String?): Boolean = mimeType.hasLowercasePrefix(MimeType.VIDEO_PREFIX)

    fun isAudio(mimeType: String?): Boolean = mimeType.hasLowercasePrefix(MimeType.AUDIO_PREFIX)

    /**
     * @return true for simple text, i.e. not application-dependent like .doc or .docx
     */
    fun isText(mimeType: String?): Boolean = mimeType.hasLowercasePrefix(MimeType.TEXT_PREFIX)

    @JvmStatic
    fun isVCard(mimeType: String?): Boolean = MimeType.VCARD.equals(mimeType, ignoreCase = true)

    @JvmStatic
    fun isCalendar(mimeType: String?): Boolean = MimeType.CALENDAR.equals(mimeType, ignoreCase = true)

    @JvmStatic
    fun isPDF(mimeType: String?): Boolean = MimeType.PDF.equals(mimeType, ignoreCase = true)

    fun isSVG(mimeType: String?): Boolean = MimeType.SVG.equals(mimeType, ignoreCase = true)

    fun isFolder(mimeType: String?): Boolean = MimeType.DIRECTORY.equals(mimeType, ignoreCase = true)

    private fun Drawable.tintIfZip(iconId: Int, context: Context, viewThemeUtils: ViewThemeUtils): Drawable = apply {
        if (iconId == R.drawable.file_zip) {
            viewThemeUtils.platform.tintDrawable(context, this, ColorRole.PRIMARY)
        }
    }

    private fun String?.hasLowercasePrefix(prefix: String): Boolean = this?.lowercase()?.startsWith(prefix) == true

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
