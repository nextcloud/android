package com.owncloud.android.operations.share_download_limit;

import android.util.Pair;

import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.ShareToRemoteOperationResultParser;
import com.owncloud.android.lib.resources.shares.ShareXMLParser;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.Utf8PostMethod;

import java.util.ArrayList;
import java.util.List;

public class UpdateShareDownloadLimitRemoteOperation extends RemoteOperation {

    private static final String TAG = UpdateShareDownloadLimitRemoteOperation.class.getSimpleName();
    // PUT --> ocs/v2.php/apps/files_downloadlimit/{token}/limit
    //body:
// {
// "token" : "Bpd4oEAgPqn3AbG",
// "limit" : 5
//}
    private static final String PARAM_TOKEN = "token";
    private static final String PARAM_LIMIT = "limit";

    private static final String ENTITY_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String ENTITY_CHARSET = "UTF-8";

    private final String shareToken;
    private final int downloadLimit;

    public UpdateShareDownloadLimitRemoteOperation(String shareToken, int downloadLimit) {
        this.shareToken = shareToken;
        this.downloadLimit = downloadLimit;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperationResult result;
        int status;

        PutMethod post = null;

        try {
            // Post Method
            post = new PutMethod(client.getBaseUri() + ShareDownloadLimitUtils.INSTANCE.getDownloadLimitApiPath(shareToken));

            post.setRequestHeader(CONTENT_TYPE, FORM_URLENCODED);

            List<Pair<String, String>> parametersToUpdate = new ArrayList<>();
                //parametersToUpdate.add(new Pair<>(PARAM_TOKEN, shareToken));
            parametersToUpdate.add(new Pair<>(PARAM_LIMIT, String.valueOf(downloadLimit)));

            for (Pair<String, String> parameter : parametersToUpdate) {
                post.setRequestEntity(new StringRequestEntity(parameter.first + "=" + parameter.second,
                                                              ENTITY_CONTENT_TYPE,
                                                              ENTITY_CHARSET));
            }

            post.addRequestHeader(OCS_API_HEADER, OCS_API_HEADER_VALUE);

            status = client.executeMethod(post);

            if (isSuccess(status)) {
                String response = post.getResponseBodyAsString();

                Log_OC.d(TAG,"Download Limit response: "+response);

                ShareToRemoteOperationResultParser parser = new ShareToRemoteOperationResultParser(
                    new ShareXMLParser()
                );
                parser.setServerBaseUri(client.getBaseUri());
                result = parser.parse(response);

                if (result.isSuccess()) {
                    return result;
                }

            } else {
                result = new RemoteOperationResult<>(false, post);
            }

        } catch (Exception e) {
            result = new RemoteOperationResult<>(e);
            Log_OC.e(TAG, "Exception while updating share download limit", e);

        } finally {
            if (post != null) {
                post.releaseConnection();
            }
        }
        return result;
    }

    private boolean isSuccess(int status) {
        return status == HttpStatus.SC_OK || status == HttpStatus.SC_BAD_REQUEST;
    }

}
