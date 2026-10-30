/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Jens Mueller <tschenser@gmx.de>
 * SPDX-FileCopyrightText: 2016 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package org.nextcloud.providers.cursors

import android.content.Context
import android.database.MatrixCursor
import android.provider.DocumentsContract.Root.COLUMN_AVAILABLE_BYTES
import android.provider.DocumentsContract.Root.COLUMN_DOCUMENT_ID
import android.provider.DocumentsContract.Root.COLUMN_FLAGS
import android.provider.DocumentsContract.Root.COLUMN_ICON
import android.provider.DocumentsContract.Root.COLUMN_ROOT_ID
import android.provider.DocumentsContract.Root.COLUMN_SUMMARY
import android.provider.DocumentsContract.Root.COLUMN_TITLE
import android.provider.DocumentsContract.Root.FLAG_SUPPORTS_CREATE
import android.provider.DocumentsContract.Root.FLAG_SUPPORTS_IS_CHILD
import android.provider.DocumentsContract.Root.FLAG_SUPPORTS_RECENTS
import android.provider.DocumentsContract.Root.FLAG_SUPPORTS_SEARCH
import com.owncloud.android.R
import com.owncloud.android.providers.documentstorage.model.Document

class RootCursor(projection: Array<String>?) : MatrixCursor(projection ?: DEFAULT_ROOT_PROJECTION) {

    fun addRoot(document: Document, context: Context) {
        val accountName = document.user.accountName

        newRow()
            .add(COLUMN_ROOT_ID, accountName)
            .add(COLUMN_DOCUMENT_ID, document.documentId)
            .add(COLUMN_SUMMARY, accountName)
            .add(COLUMN_TITLE, context.getString(R.string.app_name))
            .add(COLUMN_ICON, R.mipmap.ic_launcher)
            .add(COLUMN_FLAGS, ROOT_FLAGS)
    }

    companion object {
        private const val ROOT_FLAGS = FLAG_SUPPORTS_CREATE or
            FLAG_SUPPORTS_RECENTS or
            FLAG_SUPPORTS_SEARCH or
            FLAG_SUPPORTS_IS_CHILD

        private val DEFAULT_ROOT_PROJECTION = arrayOf(
            COLUMN_ROOT_ID,
            COLUMN_FLAGS,
            COLUMN_ICON,
            COLUMN_TITLE,
            COLUMN_DOCUMENT_ID,
            COLUMN_AVAILABLE_BYTES,
            COLUMN_SUMMARY,
            COLUMN_FLAGS
        )
    }
}
