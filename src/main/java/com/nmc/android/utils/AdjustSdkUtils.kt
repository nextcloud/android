package com.nmc.android.utils

import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.AdjustEvent
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig

object AdjustSdkUtils {
    private val TAG = AdjustSdkUtils::class.java.simpleName

    const val EVENT_TOKEN_LOGIN = "gb97gb"
    const val EVENT_TOKEN_SUCCESSFUL_LOGIN = "gx6g7g"
    const val EVENT_TOKEN_FILE_BROWSER_SHARING = "fqtiu7"
    const val EVENT_TOKEN_CREATE_SHARING_LINK = "qeyql3"

    /* event names to be tracked on clicking of FAB button which opens BottomSheet to select options */
    const val EVENT_TOKEN_FAB_BOTTOM_FILE_UPLOAD = "4rd8r4"
    const val EVENT_TOKEN_FAB_BOTTOM_PHOTO_VIDEO_UPLOAD = "v1g6ly"
    const val EVENT_TOKEN_FAB_BOTTOM_DOCUMENT_SCAN = "7fec8n"
    const val EVENT_TOKEN_FAB_BOTTOM_CAMERA_UPLOAD = "3czack"

    /* events for settings screen */
    const val EVENT_TOKEN_SETTINGS_LOGOUT = "g6mj9y"
    const val EVENT_TOKEN_SETTINGS_RESET = "zi18r0"
    const val EVENT_TOKEN_SETTINGS_AUTO_UPLOAD_ON = "vwd9yk"
    const val EVENT_TOKEN_SETTINGS_AUTO_UPLOAD_OFF = "e95w5t"

    const val EVENT_TOKEN_BACKUP_MANUAL = "oojr4y"
    const val EVENT_TOKEN_BACKUP_AUTO = "7dkhkx"

    /**
     * method to return the sdk environment for Adjust
     */
    @JvmStatic
    fun getAdjustEnvironment(): String {
        //for qa, beta, debug apk we have to use Sandbox env
        if (BuildConfig.APPLICATION_ID.contains(".beta") || BuildConfig.DEBUG) {
            return AdjustConfig.ENVIRONMENT_SANDBOX
        }

        //for release build apart from qa, beta flavours Prod env is used
        return AdjustConfig.ENVIRONMENT_PRODUCTION
    }

    /**
     * method to track events
     * tracking event only if data analysis is enabled else don't track it
     */
    @JvmStatic
    fun trackEvent(eventToken: String, appPreferences: AppPreferences?) {
        if (appPreferences?.isDataAnalysisEnabled == true) {
            val adjustEvent = AdjustEvent(eventToken)
            Adjust.trackEvent(adjustEvent)
        }
    }
}
