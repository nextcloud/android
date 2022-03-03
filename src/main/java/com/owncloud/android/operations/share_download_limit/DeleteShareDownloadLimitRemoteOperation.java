/**
 * ownCloud Android client application
 *
 * @author TSI-mc Copyright (C) 2021 TSI-mc
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License version 2, as published by the Free Software Foundation.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If not, see
 * <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.operations.share_download_limit;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;

/**
 * class to delete the download limit for the link share
 * this has to be executed when user has toggled off the download limit
 * <p>
 * API : //DELETE to /ocs/v2.php/apps/files_downloadlimit/{share_token}/limit
 */
public class DeleteShareDownloadLimitRemoteOperation extends RemoteOperation {

    private static final String TAG = DeleteShareDownloadLimitRemoteOperation.class.getSimpleName();

    private final String shareToken;

    public DeleteShareDownloadLimitRemoteOperation(String shareToken) {
        this.shareToken = shareToken;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        int status;

        DeleteMethod deleteMethod = null;

        try {
            // Post Method
            deleteMethod = new DeleteMethod(client.getBaseUri() + ShareDownloadLimitUtils.INSTANCE.getDownloadLimitApiPath(shareToken));

            deleteMethod.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            status = client.executeMethod(deleteMethod);

            if (isSuccess(status)) {
                String response = deleteMethod.getResponseBodyAsString();

                Log_OC.d(TAG, "Delete Download Limit response: " + response);

                DownloadLimitXMLParser parser = new DownloadLimitXMLParser();
                result = parser.parse(true, response);

                if (result.isSuccess()) {
                    return result;
                }

            } else {
                result = new RemoteOperationResult<>(false, deleteMethod);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Exception while deleting share download limit", e);

        } finally {
            if (deleteMethod != null) {
                deleteMethod.releaseConnection();
            }
        }
        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_OK || status == HttpStatus.SC_BAD_REQUEST;
    }

}
