/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel

import android.content.ContentResolver
import android.content.ContentValues
import android.database.Cursor
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.lib.common.ExternalLink
import com.owncloud.android.lib.common.ExternalLinkType
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ExternalLinksProvider(private val contentResolver: ContentResolver) {
    private val ioScope = CoroutineScope(Dispatchers.IO)

    fun storeExternalLink(externalLink: ExternalLink) {
        ioScope.launch {
            Log_OC.v(TAG, "Adding " + externalLink.name)
            val cv = createContentValuesFromExternalLink(externalLink)
            contentResolver.insert(ProviderMeta.ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS, cv)
                ?: Log_OC.e(TAG, "Failed to insert ${externalLink.name} into external link db.")
        }
    }

    fun deleteAllExternalLinks() {
        ioScope.launch {
            contentResolver.delete(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS,
                null,
                null
            )
        }
    }

    fun getExternalLink(type: ExternalLinkType, onComplete: (List<ExternalLink>) -> Unit) {
        ioScope.launch {
            val cursor = contentResolver.query(
                ProviderMeta.ProviderTableMeta.CONTENT_URI_EXTERNAL_LINKS,
                null,
                "type = ?",
                arrayOf(type.toString()),
                null
            ) ?: run {
                Log_OC.e(TAG, "DB error restoring externalLinks.")
                withContext(Dispatchers.Main) {
                    onComplete(emptyList())
                }
                return@launch
            }

            val result = cursor.use { c ->
                generateSequence { if (c.moveToNext()) c else null }
                    .map { createExternalLinkFromCursor(it) }
                    .toList()
            }

            return@launch withContext(Dispatchers.Main) {
                onComplete(result)
            }
        }
    }

    private fun createContentValuesFromExternalLink(externalLink: ExternalLink): ContentValues = ContentValues().apply {
        put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_ICON_URL, externalLink.iconUrl)
        put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE, externalLink.language)
        put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TYPE, externalLink.type.toString())
        put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_NAME, externalLink.name)
        put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_URL, externalLink.url)
        put(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT, externalLink.redirect)
    }

    private fun createExternalLinkFromCursor(cursor: Cursor): ExternalLink {
        fun col(name: String) = cursor.getColumnIndexOrThrow(name)
        val typeStr = cursor.getString(col(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TYPE))

        return ExternalLink(
            id = cursor.getInt(col(ProviderMeta.ProviderTableMeta._ID)),
            iconUrl = cursor.getString(col(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_ICON_URL)),
            language = cursor.getString(col(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_LANGUAGE)),
            type = when (typeStr) {
                "link" -> ExternalLinkType.LINK
                "settings" -> ExternalLinkType.SETTINGS
                "quota" -> ExternalLinkType.QUOTA
                else -> ExternalLinkType.UNKNOWN
            },
            name = cursor.getString(col(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_NAME)),
            url = cursor.getString(col(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_URL)),
            redirect = cursor.getInt(col(ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT)) == 1
        )
    }

    fun cleanup() {
        ioScope.cancel()
    }

    companion object {
        private val TAG: String = ExternalLinksProvider::class.java.getSimpleName()
    }
}
