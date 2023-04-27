/*
 *
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2023 TSI-mc
 * Copyright (C) 2023 Nextcloud GmbH
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

package com.owncloud.android.operations.download_limit

object ShareDownloadLimitUtils {

    private const val SHARE_TOKEN_PATH = "{share_token}"

    //ocs route
    //replace the {share_token}
    private const val SHARE_DOWNLOAD_LIMIT_API_PATH = "/ocs/v2.php/apps/files_downloadlimit/$SHARE_TOKEN_PATH/limit"

    fun getDownloadLimitApiPath(shareToken: String): String {
        return SHARE_DOWNLOAD_LIMIT_API_PATH.replace(SHARE_TOKEN_PATH, shareToken)
    }
}