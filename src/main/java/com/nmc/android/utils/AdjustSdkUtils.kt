package com.nmc.android.utils

import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustEvent

object AdjustSdkUtils {
    private val TAG = AdjustSdkUtils::class.java.simpleName

    const val EVENT_LOGIN = "magentacloud-app.login"
    const val EVENT_FILE_BROWSER_SHARING = "magentacloud-app.filebrowser.sharing"
    const val EVENT_CREATE_SHARING_LINK = "magentacloud-app.sharing.create"

    /* event names to be tracked on clicking of FAB button which opens BottomSheet to select options */
    const val EVENT_FAB_BOTTOM_FILE_UPLOAD = "magentacloud-app.plus.fileupload"
    const val EVENT_FAB_BOTTOM_PHOTO_VIDEO_UPLOAD = "magentacloud-app.plus.fotovideoupload"
    const val EVENT_FAB_BOTTOM_DOCUMENT_SCAN = "magentacloud-app.plus.documentscan"
    const val EVENT_FAB_BOTTOM_CAMERA_UPLOAD = "magentacloud-app.plus.cameraupload"

    /* events for settings screen */
    const val EVENT_SETTINGS_LOGOUT = "magentacloud-app.settings.logout"
    const val EVENT_SETTINGS_AUTO_UPLOAD_ON = "magentacloud-app.settings.autoupload-on"
    const val EVENT_SETTINGS_AUTO_UPLOAD_OFF = "magentacloud-app.settings.autoupload-off"

    const val EVENT_BACKUP_MANUAL = "magentacloud-app.backup.manual"
    const val EVENT_BACKUP_AUTO = "magentacloud-app.backup.auto"

    @JvmStatic
    fun trackEvent(eventToken: String) {
        val adjustEvent = AdjustEvent(eventToken)
        Adjust.trackEvent(adjustEvent)
    }
}
