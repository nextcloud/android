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
    Settings,
    PDF,
    Generic,
    SpreadSheet,
    Presentation,
    Form,
    FormTemplate,
    Drawing,
    Document,
    Whiteboard,
    TextVCard,
    TextCode,
    Link,
    Font,
    Unknown;

    fun iconId(): Int = when (this) {
        CalendarEvent -> R.drawable.file_calendar
        Folder -> R.drawable.folder
        Note -> R.drawable.ic_edit
        Contact -> R.drawable.file_vcard
        Deck -> R.drawable.ic_deck
        Settings -> R.drawable.ic_settings
        PDF -> R.drawable.file_pdf
        Generic -> R.drawable.ic_generic_file_type
        SpreadSheet -> R.drawable.file_xls
        Presentation -> R.drawable.file_ppt
        Form -> R.drawable.ic_form
        FormTemplate -> R.drawable.ic_form_template
        Drawing -> R.drawable.ic_drawing
        Document -> R.drawable.file_doc
        Whiteboard -> R.drawable.file_whiteboard
        TextVCard -> R.drawable.file_vcard
        TextCode -> R.drawable.file_code
        Link -> R.drawable.ic_link
        Font -> R.drawable.ic_font
        Unknown -> R.drawable.ic_find_in_page
    }
}
