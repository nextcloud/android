/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.model

import com.owncloud.android.R

enum class SearchResultEntryType {
    CalendarEvent,
    Folder,
    Note,
    Contact,
    Deck,
    Unknown;

    fun iconId(): Int = when (this) {
        CalendarEvent -> R.drawable.file_calendar
        Folder -> R.drawable.folder
        Note -> R.drawable.ic_edit
        Contact -> R.drawable.file_vcard
        Deck -> R.drawable.ic_deck
        Unknown -> R.drawable.ic_find_in_page
    }
}
