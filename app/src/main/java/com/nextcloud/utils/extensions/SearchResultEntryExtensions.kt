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
    return if (icon == "icon-folder") {
        SearchResultEntryType.Folder
    } else if (icon.startsWith("icon-note")) {
        SearchResultEntryType.Note
    } else if (icon.startsWith("icon-contacts")) {
        SearchResultEntryType.Contact
    } else if (icon.startsWith("icon-calendar")) {
        SearchResultEntryType.CalendarEvent
    } else if (icon.startsWith("icon-deck")) {
        SearchResultEntryType.Deck
    } else {
        SearchResultEntryType.Unknown
    }
}
