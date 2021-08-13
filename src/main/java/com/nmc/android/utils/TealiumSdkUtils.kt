package com.nmc.android.utils

import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig
import com.tealium.library.Tealium

object TealiumSdkUtils {

    //tealium instance name
    const val INSTANCE_NAME = "tealium_main"

    //Live Version of the app (published in app stores)
    private const val PROD_ENV = "prod"

    //Quality System
    private const val QA_ENV = "qa"

    //Staging System (Development System)
    private const val DEV_ENV = "dev"

    const val EVENT_SUCCESSFUL_LOGIN = "magentacloud-app.login.successful"
    const val EVENT_FILE_BROWSER_SHARING = "magentacloud-app.filebrowser.sharing"
    const val EVENT_CREATE_SHARING_LINK = "magentacloud-app.sharing.create"

    /* event names to be tracked on clicking of FAB button which opens BottomSheet to select options */
    const val EVENT_FAB_BOTTOM_DOCUMENT_SCAN = "magentacloud-app.plus.documentscan"
    const val EVENT_FAB_BOTTOM_PHOTO_VIDEO_UPLOAD = "magentacloud-app.plus.fotovideoupload"
    const val EVENT_FAB_BOTTOM_FILE_UPLOAD = "magentacloud-app.plus.fileupload"
    const val EVENT_FAB_BOTTOM_CAMERA_UPLOAD = "magentacloud-app.plus.cameraupload"

    /* events for settings screen */
    const val EVENT_SETTINGS_LOGOUT = "magentacloud-app.settings.logout"
    const val EVENT_SETTINGS_RESET = "magentacloud-app.settings.reset"
    const val EVENT_SETTINGS_AUTO_UPLOAD_ON = "magentacloud-app.settings.autoupload-on"
    const val EVENT_SETTINGS_AUTO_UPLOAD_OFF = "magentacloud-app.settings.autoupload-off"

    const val EVENT_BACKUP_MANUAL = "magentacloud-app.backup.manual"
    const val EVENT_BACKUP_AUTO = "magentacloud-app.backup.auto"

    /* Screen View Names to be tracked */
    const val SCREEN_VIEW_LOGIN = "magentacloud-app.login"
    const val SCREEN_VIEW_FILE_BROWSER = "magentacloud-app.filebrowser"
    const val SCREEN_VIEW_FAB_PLUS = "magentacloud-app.plus"
    const val SCREEN_VIEW_SHARING = "magentacloud-app.sharing"
    const val SCREEN_VIEW_SETTINGS = "magentacloud-app.settings"
    const val SCREEN_VIEW_BACKUP = "magentacloud-app.backup"

    /**
     * method to return the tealium sdk environment
     */
    @JvmStatic
    fun getTealiumEnvironment(): String {
        //if flavour is qa then return the qa environment
        if (BuildConfig.FLAVOR == "qa") {
            return QA_ENV
        }

        //if flavour is versionDev or the build has debug mode then return dev environment
        if (BuildConfig.FLAVOR == "versionDev" || BuildConfig.DEBUG) {
            return DEV_ENV
        }

        //for release build to play store return prod environment
        return PROD_ENV
    }

    /**
     * method to track events
     * tracking event only if data analysis is enabled else don't track it
     */
    @JvmStatic
    fun trackEvent(eventName: String, appPreferences: AppPreferences?) {
        if (appPreferences?.isDataAnalysisEnabled == true) {
            Tealium.getInstance(INSTANCE_NAME).trackEvent(eventName, null)
        }
    }

    /**
     * method to track view
     * tracking view only if data analysis is enabled else don't track it
     */
    @JvmStatic
    fun trackView(viewName: String, appPreferences: AppPreferences?) {
        if (appPreferences?.isDataAnalysisEnabled == true) {
            Tealium.getInstance(INSTANCE_NAME).trackView(viewName, null)
        }
    }
}