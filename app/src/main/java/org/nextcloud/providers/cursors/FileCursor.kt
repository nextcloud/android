/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Jens Mueller <tschenser@gmx.de>
 * SPDX-FileCopyrightText: 2016 Iskra Delta <iskradelta@no-reply.github.com>
 * SPDX-FileCopyrightText: 2015 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package org.nextcloud.providers.cursors

import android.database.MatrixCursor
import android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME
import android.provider.DocumentsContract.Document.COLUMN_DOCUMENT_ID
import android.provider.DocumentsContract.Document.COLUMN_FLAGS
import android.provider.DocumentsContract.Document.COLUMN_ICON
import android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED
import android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
import android.provider.DocumentsContract.Document.COLUMN_SIZE
import android.provider.DocumentsContract.Document.FLAG_DIR_SUPPORTS_CREATE
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_COPY
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_DELETE
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_MOVE
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_REMOVE
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_RENAME
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_THUMBNAIL
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_WRITE
import android.provider.DocumentsContract.Document.MIME_TYPE_DIR
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.providers.documentstorage.model.Document
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.Job

class FileCursor(projection: Array<String>?) : MatrixCursor(projection ?: DEFAULT_DOCUMENT_PROJECTION) {

    var loadingTask: Job? = null

    override fun close() {
        super.close()
        loadingTask?.cancel()
    }

    fun addFile(document: Document, file: OCFile) {
        newRow()
            .add(COLUMN_DOCUMENT_ID, document.documentId)
            .add(COLUMN_DISPLAY_NAME, file.fileName)
            .add(COLUMN_LAST_MODIFIED, file.modificationTimestamp)
            .add(COLUMN_SIZE, file.fileLength)
            .add(COLUMN_FLAGS, documentFlags(file))
            .add(COLUMN_ICON, MimeTypeUtil.getFileTypeIconId(file.mimeType, file.fileName))
            .add(COLUMN_MIME_TYPE, if (file.isFolder) MIME_TYPE_DIR else file.mimeType)
    }

    private fun documentFlags(file: OCFile): Int {
        val thumbnailFlag = if (MimeTypeUtil.isImage(file)) FLAG_SUPPORTS_THUMBNAIL else 0
        val createFlag = if (file.isFolder) FLAG_DIR_SUPPORTS_CREATE else 0
        return BASE_DOCUMENT_FLAGS or thumbnailFlag or createFlag
    }

    companion object {
        private const val BASE_DOCUMENT_FLAGS = FLAG_SUPPORTS_RENAME or
            FLAG_SUPPORTS_DELETE or
            FLAG_SUPPORTS_WRITE or
            FLAG_SUPPORTS_COPY or
            FLAG_SUPPORTS_MOVE or
            FLAG_SUPPORTS_REMOVE

        private val DEFAULT_DOCUMENT_PROJECTION = arrayOf(
            COLUMN_DOCUMENT_ID,
            COLUMN_DISPLAY_NAME,
            COLUMN_MIME_TYPE,
            COLUMN_SIZE,
            COLUMN_FLAGS,
            COLUMN_LAST_MODIFIED
        )
    }
}
