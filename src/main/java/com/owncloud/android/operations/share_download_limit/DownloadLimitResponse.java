package com.owncloud.android.operations.share_download_limit;

/**
 * response from the Get download limit api
 * <?xml version="1.0"?>
 * <ocs>
 *     <meta>
 *         <status>ok</status>
 *         <statuscode>200</statuscode>
 *         <message>OK</message>
 *     </meta>
 *     <data>
 *         <limit>5</limit>
 *         <count>0</count>
 *     </data>
 * </ocs>
 */
public class DownloadLimitResponse {
    private long limit;
    private long count;

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
