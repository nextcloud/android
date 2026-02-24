/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.nextcloud.model.SearchResultEntryType
import com.owncloud.android.lib.common.SearchResultEntry

fun SearchResultEntry.getType(): SearchResultEntryType {
    val value = icon.lowercase()

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
        else -> SearchResultEntryType.Unknown
    }
}
