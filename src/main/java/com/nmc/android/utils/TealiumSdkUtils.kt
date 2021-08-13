package com.nmc.android.utils

import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig
import com.tealium.library.Tealium

object TealiumSdkUtils {
    //Live Version of the app (published in app stores)
    private const val PROD_ENV = "prod"

    //Quality System
    private const val QA_ENV = "qa"

    //Staging System (Development System)
    private const val DEV_ENV = "dev"

    const val INSTANCE_NAME = "tealium_main"

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
    }}