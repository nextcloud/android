/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.files

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.Utf8PostMethod
import org.json.JSONObject

@Suppress("TooGenericExceptionCaught")
class StreamMediaFileOperation(private val fileID: Long) : RemoteOperation<ArrayList<String>>() {

    @Deprecated("Deprecated in Java")
    override fun run(client: OwnCloudClient): RemoteOperationResult<ArrayList<String>> {
        val postMethod = Utf8PostMethod(client.baseUri.toString() + STREAM_MEDIA_URL + JSON_FORMAT)

        return try {
            postMethod.apply {
                setParameter("fileId", fileID.toString())
                addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
                params.soTimeout = SYNC_READ_TIMEOUT
            }

            val status = client.executeMethod(postMethod, SYNC_READ_TIMEOUT, SYNC_CONNECTION_TIMEOUT)

            if (status == HttpStatus.SC_OK) {
                val response = postMethod.getResponseBodyAsString()
                val url = JSONObject(response)
                    .getJSONObject(NODE_OCS)
                    .getJSONObject(NODE_DATA)
                    .getString(NODE_URL)

                RemoteOperationResult<ArrayList<String>>(true, postMethod).also {
                    it.data = arrayListOf(url)
                }
            } else {
                client.exhaustResponse(postMethod.getResponseBodyAsStream())
                RemoteOperationResult(false, postMethod)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "Get stream url for file with id $fileID failed: ${e.message}", e)
            RemoteOperationResult(e)
        } finally {
            postMethod.releaseConnection()
        }
    }

    companion object {
        private val TAG = StreamMediaFileOperation::class.java.simpleName
        private const val SYNC_READ_TIMEOUT = 120_000
        private const val SYNC_CONNECTION_TIMEOUT = 15_000
        private const val STREAM_MEDIA_URL = "/ocs/v2.php/apps/dav/api/v1/direct"
        private const val NODE_OCS = "ocs"
        private const val NODE_DATA = "data"
        private const val NODE_URL = "url"
        private const val JSON_FORMAT = "?format=json"
    }
}
