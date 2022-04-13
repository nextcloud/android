/**
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * @author Tobias Kaminsky
 *
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2018 Tobias Kaminsky
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.media

import android.os.AsyncTask
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient

internal class LoadUrlTask(
    private val client: OwnCloudClient,
    private val fileId: String,
    private val onResult: (String?) -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg args: Void): String? {
        val operation = StreamMediaFileOperation(fileId)
        val result = operation.execute(client)
        return when (result.isSuccess) {
            true -> result.data[0] as String
            false -> null
        }
    }

    override fun onPostExecute(url: String?) {
        if (!isCancelled) {
            onResult(url)
        }
    }
}
