/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage.sync

import com.owncloud.android.providers.documentstorage.model.Document
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.owncloud.android.R

internal class DocumentNotifier(private val context: Context) {

    fun notifyChanged(document: Document) {
        context.contentResolver.notifyChange(notifyUri(document), null, NO_NOTIFY_FLAGS)
    }

    fun notifyUri(document: Document): Uri = DocumentsContract.buildDocumentUri(
        context.getString(R.string.document_provider_authority),
        document.documentId
    )

    companion object {
        private const val NO_NOTIFY_FLAGS = 0
    }
}
