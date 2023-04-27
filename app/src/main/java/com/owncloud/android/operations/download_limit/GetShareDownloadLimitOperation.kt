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

import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.operations.RemoteOperation
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.download_limit.ShareDownloadLimitUtils.getDownloadLimitApiPath
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.GetMethod

/**
 * class to fetch the download limit for the link share it requires share token to fetch the data
 *
 *
 * API : //GET to /ocs/v2.php/apps/files_downloadlimit/{share_token}/limit
 */
class GetShareDownloadLimitOperation(
    //share token from OCShare
    private val shareToken: String
) : RemoteOperation<DownloadLimitResponse>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<DownloadLimitResponse> {
        var result: RemoteOperationResult<DownloadLimitResponse>
        var status = -1
        var get: GetMethod? = null
        try {
            // Get Method
            get = GetMethod(
                client.baseUri.toString() + getDownloadLimitApiPath(
                    shareToken
                )
            )
            get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
            status = client.executeMethod(get)
            if (isSuccess(status)) {
                val response = get.responseBodyAsString
                Log_OC.d(TAG, "Get Download Limit response: $response")
                val parser = DownloadLimitXMLParser()
                result = parser.parse(true, response)
                if (result.isSuccess) {
                    Log_OC.d(TAG, "Got " + result.resultData + " Response")
                }
            } else {
                result = RemoteOperationResult<DownloadLimitResponse>(false, get)
            }
        } catch (e: Exception) {
            result = RemoteOperationResult<DownloadLimitResponse>(e)
            Log_OC.e(TAG, "Exception while getting share download limit", e)
        } finally {
            get?.releaseConnection()
        }
        return result
    }

    private fun isSuccess(status: Int): Boolean {
        return status == HttpStatus.SC_OK
    }

    companion object {
        private val TAG = GetShareDownloadLimitOperation::class.java.simpleName
    }
}