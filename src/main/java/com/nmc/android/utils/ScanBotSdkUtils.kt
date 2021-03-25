package com.nmc.android.utils

import android.app.Activity
import com.owncloud.android.lib.common.utils.Log_OC
import io.scanbot.sdk.ScanbotSDK

object ScanBotSdkUtils {
    private val TAG = ScanBotSdkUtils::class.java.simpleName

    @JvmStatic
    fun isScanBotLicenseValid(activity: Activity): Boolean {
        // Check the license status:
        val licenseInfo = ScanbotSDK(activity).licenseInfo
        Log_OC.d(TAG, "License status: ${licenseInfo.status}")
        Log_OC.d(TAG, "License isValid: ${licenseInfo.isValid}")

        // Making your call into ScanbotSDK API is safe now.
        // e.g. start barcode scanner
        return licenseInfo.isValid
    }
}