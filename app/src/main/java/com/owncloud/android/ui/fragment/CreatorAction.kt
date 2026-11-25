/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.owncloud.android.R
import com.owncloud.android.lib.common.Creator
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.theme.ViewThemeUtils

data class CreatorAction(val text: String, val icon: Drawable?, val action: () -> Unit, val type: CreatorType) {
    companion object {
        private val DIRECT_EDITING_CREATORS_OFFICE_IDS = setOf("document", "spreadsheet", "presentation")

        fun getNewDocument(context: Context, action: () -> Unit): CreatorAction =
            office(context, R.string.create_document, R.drawable.file_doc, action)

        fun getNewSpreadsheet(context: Context, action: () -> Unit): CreatorAction =
            office(context, R.string.create_spreadsheet, R.drawable.file_xls, action)

        fun getNewPresentation(context: Context, action: () -> Unit): CreatorAction =
            office(context, R.string.create_presentation, R.drawable.file_ppt, action)

        fun getAction(
            context: Context,
            viewThemeUtils: ViewThemeUtils,
            creator: Creator,
            action: () -> Unit
        ): CreatorAction = CreatorAction(
            text = creator.name.replaceFirstChar(Char::titlecase),
            icon = MimeTypeUtil.getFileTypeIcon(creator.mimetype, creator.extension, context, viewThemeUtils),
            action = action,
            type = if (creator.id in DIRECT_EDITING_CREATORS_OFFICE_IDS) CreatorType.Office else CreatorType.Other
        )

        private fun office(
            context: Context,
            @StringRes textRes: Int,
            @DrawableRes iconRes: Int,
            action: () -> Unit
        ): CreatorAction = CreatorAction(
            text = context.getString(textRes),
            icon = AppCompatResources.getDrawable(context, iconRes),
            action = action,
            type = CreatorType.Office
        )
    }
}
