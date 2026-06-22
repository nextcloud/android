/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.utils

import com.google.gson.Gson
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.DirectEditing
import com.owncloud.android.lib.common.Editor
import javax.inject.Inject

class EditorUtils @Inject constructor(private val arbitraryDataProvider: ArbitraryDataProvider) {

    /**
     * Returns only supported mimetypes
     */
    fun getEditor(user: User?, mimeType: String?): Editor? {
        val editors = getEditors(user) ?: return null
        return editors.firstOrNull { mimeType in it.mimetypes }
    }

    /**
     * Returns supported mimetypes along with optional ones
     */
    fun isEditorAvailable(user: User?, mimeType: String?): Boolean {
        val editors = getEditors(user) ?: return false
        return editors.any { mimeType in it.mimetypes || mimeType in it.optionalMimetypes }
    }

    private fun getEditors(user: User?): Collection<Editor>? {
        val json = arbitraryDataProvider.getValue(user, ArbitraryDataProvider.DIRECT_EDITING)
        if (json.isEmpty()) return null
        return Gson().fromJson(json, DirectEditing::class.java).editors.values
    }

    fun usesOfficeUserAgent(editor: Editor?): Boolean = editor?.id in OFFICE_EDITOR_IDS

    companion object {
        private val OFFICE_EDITOR_IDS = setOf("onlyoffice", "eurooffice")
    }
}
