/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.utils

import android.content.ContentResolver
import com.google.gson.Gson
import com.nextcloud.client.account.User
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.DirectEditing
import com.owncloud.android.lib.common.Editor

object EditorUtils {
    @JvmStatic
    fun isEditorAvailable(contentResolver: ContentResolver?, user: User?, mimeType: String?): Boolean {
        return getEditor(contentResolver, user, mimeType) != null
    }

    @JvmStatic
    fun getEditor(contentResolver: ContentResolver?, user: User?, mimeType: String?): Editor? {
        val json = ArbitraryDataProvider(contentResolver).getValue(user, ArbitraryDataProvider.DIRECT_EDITING)
        if (json.isEmpty()) {
            return null
        }
        val directEditing = Gson().fromJson(json, DirectEditing::class.java)
        val editors = directEditing.editors.values
        return editors.firstOrNull { it.mimetypes.contains(mimeType) || it.optionalMimetypes.contains(mimeType) }
    }
}
