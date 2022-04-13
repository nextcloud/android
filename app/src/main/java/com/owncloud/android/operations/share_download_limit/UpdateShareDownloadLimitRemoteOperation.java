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

import android.util.Pair;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * class to update the download limit for the link share
 * <p>
 * API : //PUT to /ocs/v2.php/apps/files_downloadlimit/{share_token}/limit
 * <p>
 * Body: {"token" : "Bpd4oEAgPqn3AbG", "limit" : 5}
 */
public class UpdateShareDownloadLimitRemoteOperation extends RemoteOperation {

    private static final String TAG = UpdateShareDownloadLimitRemoteOperation.class.getSimpleName();

    private static final String PARAM_TOKEN = "token";
    private static final String PARAM_LIMIT = "limit";

    private static final String ENTITY_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String ENTITY_CHARSET = "UTF-8";

    private final String shareToken;
    private final long downloadLimit;

    public UpdateShareDownloadLimitRemoteOperation(String shareToken, long downloadLimit) {
        this.shareToken = shareToken;
        this.downloadLimit = downloadLimit;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        int status;

        PutMethod put = null;

        try {
            // Post Method
            put = new PutMethod(client.getBaseUri() + ShareDownloadLimitUtils.INSTANCE.getDownloadLimitApiPath(shareToken));

            put.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);
            List<Pair<String, String>> parametersToUpdate = new ArrayList<>();
            parametersToUpdate.add(new Pair<>(PARAM_TOKEN, shareToken));
            parametersToUpdate.add(new Pair<>(PARAM_LIMIT, String.valueOf(downloadLimit)));

            for (Pair<String, String> parameter : parametersToUpdate) {
                put.setRequestEntity(new StringRequestEntity(parameter.first + "=" + parameter.second,
                                                             ENTITY_CONTENT_TYPE,
                                                             ENTITY_CHARSET));
            }

            status = client.executeMethod(put);

            if (isSuccess(status)) {
                String response = put.getResponseBodyAsString();

                Log_OC.d(TAG, "Download Limit response: " + response);

                DownloadLimitXMLParser parser = new DownloadLimitXMLParser();
                result = parser.parse(true, response);

                if (result.isSuccess()) {
                    return result;
                }

            } else {
                result = new RemoteOperationResult<>(false, put);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Exception while updating share download limit", e);

        } finally {
            if (put != null) {
                put.releaseConnection();
            }
        }
        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_OK || status == HttpStatus.SC_BAD_REQUEST;
    }

}
