/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import androidx.core.net.toUri
import com.nextcloud.model.SearchResultEntryType
import com.owncloud.android.lib.common.SearchResultEntry

private const val SVG_EXTENSION = ".svg"
private const val COLORED_ICON_SUFFIX = "-color.svg"
private const val PREVIEW_PATH = "/core/preview"
private const val PREVIEW_WIDTH_PARAMETER = "x"
private const val PREVIEW_HEIGHT_PARAMETER = "y"

fun SearchResultEntry.getType(): SearchResultEntryType {
    val value = icon.lowercase()

    fun isAvatarUrl(url: String): Boolean {
        val regex = Regex("""^https?://[^/]+/avatar/[^/]+/\d+$""")
        return regex.matches(url)
    }

    return when {
        value.contains("icon-folder") -> SearchResultEntryType.Folder
        value.contains("icon-note") -> SearchResultEntryType.Note
        value.contains("icon-contacts") -> SearchResultEntryType.Contact
        value.contains("icon-calendar") || value.contains("text-calendar") -> SearchResultEntryType.CalendarEvent
        value.contains("icon-deck") -> SearchResultEntryType.Deck
        value.contains("icon-settings") -> SearchResultEntryType.Settings
        value.contains("application-pdf") -> SearchResultEntryType.PDF
        value.contains("package-x-generic") -> SearchResultEntryType.Generic
        value.contains("x-office-spreadsheet") -> SearchResultEntryType.SpreadSheet
        value.contains("x-office-presentation") -> SearchResultEntryType.Presentation
        value.contains("x-office-form") -> SearchResultEntryType.Form
        value.contains("x-office-form-template") -> SearchResultEntryType.FormTemplate
        value.contains("x-office-drawing") -> SearchResultEntryType.Drawing
        value.contains("x-office-document") -> SearchResultEntryType.Document
        value.contains("whiteboard") -> SearchResultEntryType.Whiteboard
        value.contains("text-vcard") -> SearchResultEntryType.TextVCard
        value.contains("text-code") -> SearchResultEntryType.TextCode
        value.contains("link") -> SearchResultEntryType.Link
        value.contains("font") -> SearchResultEntryType.Font
        isAvatarUrl(thumbnailUrl) -> SearchResultEntryType.Avatar
        else -> SearchResultEntryType.Unknown
    }
}

fun SearchResultEntry.isMonochromeIcon(): Boolean {
    val path = thumbnailUrl.toUri().path ?: return false
    return path.endsWith(SVG_EXTENSION, ignoreCase = true) && !path.endsWith(COLORED_ICON_SUFFIX, ignoreCase = true)
}

fun SearchResultEntry.thumbnailUrlForSize(sizePx: Int): String {
    val uri = thumbnailUrl.toUri()
    if (uri.path?.endsWith(PREVIEW_PATH) != true) {
        return thumbnailUrl
    }

    val builder = uri.buildUpon().clearQuery()
    uri.queryParameterNames.forEach { name ->
        val isSizeParameter = name == PREVIEW_WIDTH_PARAMETER || name == PREVIEW_HEIGHT_PARAMETER
        val value = if (isSizeParameter) sizePx.toString() else uri.getQueryParameter(name)
        builder.appendQueryParameter(name, value)
    }
    return builder.build().toString()
}
