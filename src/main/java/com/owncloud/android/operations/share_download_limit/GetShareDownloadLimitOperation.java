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
import com.owncloud.android.lib.resources.shares.ShareToRemoteOperationResultParser;
import com.owncloud.android.lib.resources.shares.ShareXMLParser;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;

/**
 * class to fetch the download limit for the link share it requires share token to fetch the data
 * <p>
 * API : //GET to /ocs/v2.php/apps/files_downloadlimit/{share_token}/limit
 */
public class GetShareDownloadLimitOperation extends RemoteOperation {

    private static final String TAG = GetShareDownloadLimitOperation.class.getSimpleName();

    //share token from OCShare
    private final String shareToken;

    public GetShareDownloadLimitOperation(String shareToken) {
        this.shareToken = shareToken;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result = null;
        int status = -1;

        GetMethod get = null;

        try {
            // Get Method
            get = new GetMethod(client.getBaseUri() + ShareDownloadLimitUtils.INSTANCE.getDownloadLimitApiPath(shareToken));

            get.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            status = client.executeMethod(get);

            if (isSuccess(status)) {
                String response = get.getResponseBodyAsString();

                Log_OC.d(TAG, "Get Download Limit response: " + response);

                // Parse xml response and obtain the list of shares
                ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
                    new ShareXMLParser()
                );
                parser.setServerBaseUri(client.getBaseUri());
                result = parser.parse(response);

                if (result.isSuccess()) {
                    Log_OC.d(TAG, "Got " + result.getData().size() + " shares");
                }

            } else {
                result = new RemoteOperationResult(false, get);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log_OC.e(TAG, "Exception while getting share download limit", e);

        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
        return result;
    }

    private boolean isSuccess(int status) {
        return (status == HttpStatus.SC_OK);
    }

}
