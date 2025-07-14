/*
 * Nextcloud - Android Client
 *
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

    fun getEditor(user: User?, mimeType: String?): Editor? {
        val json = arbitraryDataProvider.getValue(user, ArbitraryDataProvider.DIRECT_EDITING)
        if (json.isEmpty()) {
            return null
        }
        val editors = Gson().fromJson(json, DirectEditing::class.java).editors.values
        return editors.firstOrNull { mimeType in it.mimetypes }
            ?: editors.firstOrNull { mimeType in it.optionalMimetypes }
    }

    fun isEditorAvailable(user: User?, mimeType: String?): Boolean = getEditor(user, mimeType) != null
}
