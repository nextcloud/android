package com.owncloud.android.operations.share_download_limit

object ShareDownloadLimitUtils {

    private const val SHARE_TOKEN_PATH = "{share_token}"

    //ocs route
    //replace the {share_token}
    private const val SHARE_DOWNLOAD_LIMIT_API_PATH = "/ocs/v2.php/apps/files_downloadlimit/$SHARE_TOKEN_PATH/limit"

    fun getDownloadLimitApiPath(shareToken: String) : String{
        return SHARE_DOWNLOAD_LIMIT_API_PATH.replace(SHARE_TOKEN_PATH, shareToken)
    }
}