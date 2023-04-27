/**
 * ownCloud Android client application
 *
 * @author TSI-mc Copyright (C) 2021 TSI-mc
 *
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation.
 *
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 *
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <http:></http:>//www.gnu.org/licenses/>.
 */
package com.owncloud.android.operations.download_limit

import android.util.Pair
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.download_limit.ShareDownloadLimitUtils.getDownloadLimitApiPath
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.PutMethod
import org.apache.commons.httpclient.methods.StringRequestEntity

/**
 * class to update the download limit for the link share
 *
 *
 * API : //PUT to /ocs/v2.php/apps/files_downloadlimit/{share_token}/limit
 *
 *
 * Body: {"token" : "Bpd4oEAgPqn3AbG", "limit" : 5}
 */
class UpdateShareDownloadLimitRemoteOperation(private val shareToken: String, private val downloadLimit: Long) :
    RemoteOperation<DownloadLimitResponse>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<DownloadLimitResponse> {
        var result: RemoteOperationResult<DownloadLimitResponse>
        val status: Int
        var put: PutMethod? = null
        try {
            // Post Method
            put = PutMethod(
                client.baseUri.toString() + getDownloadLimitApiPath(
                    shareToken
                )
            )
            put.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
            val parametersToUpdate: MutableList<Pair<String, String>> = ArrayList()
            parametersToUpdate.add(Pair(PARAM_TOKEN, shareToken))
            parametersToUpdate.add(Pair(PARAM_LIMIT, downloadLimit.toString()))
            for (parameter in parametersToUpdate) {
                put.requestEntity = StringRequestEntity(
                    parameter.first + "=" + parameter.second,
                    ENTITY_CONTENT_TYPE,
                    ENTITY_CHARSET
                )
            }
            status = client.executeMethod(put)
            if (isSuccess(status)) {
                val response = put.responseBodyAsString
                Log_OC.d(TAG, "Download Limit response: $response")
                val parser = DownloadLimitXMLParser()
                result = parser.parse(true, response)
                if (result.isSuccess) {
                    return result
                }
            } else {
                result = RemoteOperationResult<DownloadLimitResponse>(false, put)
            }
        } catch (e: Exception) {
            result = RemoteOperationResult<DownloadLimitResponse>(e)
            Log_OC.e(TAG, "Exception while updating share download limit", e)
        } finally {
            put?.releaseConnection()
        }
        return result
    }

    private fun isSuccess(status: Int): Boolean {
        return status == HttpStatus.SC_OK || status == HttpStatus.SC_BAD_REQUEST
    }

    companion object {
        private val TAG = UpdateShareDownloadLimitRemoteOperation::class.java.simpleName
        private const val PARAM_TOKEN = "token"
        private const val PARAM_LIMIT = "limit"
        private const val ENTITY_CONTENT_TYPE = "application/x-www-form-urlencoded"
        private const val ENTITY_CHARSET = "UTF-8"
    }
}