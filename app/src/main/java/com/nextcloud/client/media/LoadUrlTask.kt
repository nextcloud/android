/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.media

import android.os.AsyncTask
import com.owncloud.android.files.StreamMediaFileOperation
import com.owncloud.android.lib.common.OwnCloudClient

internal class LoadUrlTask(
    private val client: OwnCloudClient,
    private val fileId: Long,
    private val onResult: (String?) -> Unit
) : AsyncTask<Void, Void, String>() {

    override fun doInBackground(vararg args: Void): String? {
        val operation = StreamMediaFileOperation(fileId)
        val result = operation.execute(client)
        return when (result.isSuccess) {
            true -> result.data?.get(0) as String
            false -> null
        }
    }

    override fun onPostExecute(url: String?) {
        if (!isCancelled) {
            onResult(url)
        }
    }
}
