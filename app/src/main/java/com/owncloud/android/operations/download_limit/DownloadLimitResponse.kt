package com.owncloud.android.operations.download_limit

/**
 * response from the Get download limit api
 *
 * <ocs>
 * <meta></meta>
 * <status>ok</status>
 * <statuscode>200</statuscode>
 * <message>OK</message>
 *
 * <data>
 * <limit>5</limit>
 * <count>0</count>
</data> *
</ocs> *
 */
data class DownloadLimitResponse(var limit: Long = 0, var count: Long = 0)