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
    private int limit;
    private int count;

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
