/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import com.owncloud.android.R

data class CreatorAction(
    val text: String,
    val icon: Drawable?,
    val action: () -> Unit,
    val type: CreatorType
) {
    companion object {
        fun getNewDocument(context: Context, action: () -> Unit): CreatorAction = CreatorAction(
            text = context.getString(R.string.create_document),
            icon = AppCompatResources.getDrawable(context, R.drawable.file_doc),
            action = action,
            type = CreatorType.Office
        )

        fun getNewSpreadsheet(context: Context, action: () -> Unit): CreatorAction = CreatorAction(
            text = context.getString(R.string.create_spreadsheet),
            icon = AppCompatResources.getDrawable(context, R.drawable.file_xls),
            action = action,
            type = CreatorType.Office
        )

        fun getNewPresentation(context: Context, action: () -> Unit): CreatorAction = CreatorAction(
            text = context.getString(R.string.create_presentation),
            icon = AppCompatResources.getDrawable(context, R.drawable.file_ppt),
            action = action,
            type = CreatorType.Office
        )
    }
}
