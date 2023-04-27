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
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.DeleteMethod

/**
 * class to delete the download limit for the link share
 * this has to be executed when user has toggled off the download limit
 *
 *
 * API : //DELETE to /ocs/v2.php/apps/files_downloadlimit/{share_token}/limit
 */
class DeleteShareDownloadLimitRemoteOperation(private val shareToken: String) :
    RemoteOperation<DownloadLimitResponse>() {
    override fun run(client: OwnCloudClient): RemoteOperationResult<DownloadLimitResponse> {
        var result: RemoteOperationResult<DownloadLimitResponse>
        val status: Int
        var deleteMethod: DeleteMethod? = null
        try {
            // Post Method
            deleteMethod = DeleteMethod(
                client.baseUri.toString() + ShareDownloadLimitUtils.getDownloadLimitApiPath(
                    shareToken
                )
            )
            deleteMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE)
            status = client.executeMethod(deleteMethod)
            if (isSuccess(status)) {
                val response = deleteMethod.responseBodyAsString
                Log_OC.d(TAG, "Delete Download Limit response: $response")
                val parser = DownloadLimitXMLParser()
                result = parser.parse(true, response)
                if (result.isSuccess) {
                    return result
                }
            } else {
                result = RemoteOperationResult<DownloadLimitResponse>(false, deleteMethod)
            }
        } catch (e: Exception) {
            result = RemoteOperationResult<DownloadLimitResponse>(e)
            Log_OC.e(TAG, "Exception while deleting share download limit", e)
        } finally {
            deleteMethod?.releaseConnection()
        }
        return result
    }

    private fun isSuccess(status: Int): Boolean {
        return status == HttpStatus.SC_OK || status == HttpStatus.SC_BAD_REQUEST
    }

    companion object {
        private val TAG = DeleteShareDownloadLimitRemoteOperation::class.java.simpleName
    }
}