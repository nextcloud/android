/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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

    fun isEditorAvailable(user: User?, mimeType: String?): Boolean {
        return getEditor(user, mimeType) != null
    }
}
