package com.nmc.android.utils

import android.app.Activity
import android.graphics.Bitmap
import com.owncloud.android.BuildConfig
import com.owncloud.android.lib.common.utils.Log_OC
import io.scanbot.sdk.ScanbotSDK
import kotlin.math.roundToInt

object ScanBotSdkUtils {
    private val TAG = ScanBotSdkUtils::class.java.simpleName

    //license key will be valid for application id: com.t_systems.android.webdav
    private const val LICENSE_KEY = "WVIp/YSuAP/KuuMkULIB9Yy8TmDJIl" +
        "HqhEVjaE9RJJvn4ziiIZAcCWjBpaZw" +
        "ApeTzmbG/IX51525WDrHEF3jYgIa0C" +
        "3rba/kj2OyO9WjIZSF097wfAv8XE+7" +
        "lJeN4f1++YxFcvtndgTyZQ+wKfr2Sg" +
        "EZCpEQGemhZRZn3fydU9IO6TFlj4yJ" +
        "j/C0ZutUZDlbZxk726IKi/zaXmpUFL" +
        "Rk6p7hCiuHkj1cuATKVq5FFQSKddbk" +
        "OM9Tf8uEfqZOBrxXj/7b7Mms1lDwbp" +
        "HUrexC6BCs/ri17gP24sM8m7/7cKz8" +
        "uNflgR/We181H5eoK+QfK5UA+dYIp9" +
        "1NNb8i/oCj3g==\nU2NhbmJvdFNESw" +
        "pjb20udF9zeXN0ZW1zLmFuZHJvaWQu" +
        "d2ViZGF2CjE2ODI0NjcxOTkKMTE1NT" +
        "Y3OAoy\n"

    //beta license key will be valid for application id: com.t_systems.android.webdav.beta
    private const val BETA_LICENSE_KEY = "DV6ZbfLNxXPRHWjGHO6+nTCKwnc3aA" +
        "gmW994IgzI18nMNbny7m7D0JheDH1q" +
        "R+UYYTN3VpjuYbc+ZcZEn2kwZ6EFND" +
        "BveVqfshiT75it1SNBZMEigqnatFtq" +
        "K4cgPyJ13dsOifJiYYJkUMWioJJqPG" +
        "MRpB1sg5YLQOMFx4zYumYfmOo6aTbz" +
        "1meOE+DJib4bqWBImJr+oqGR+hsRaZ" +
        "CY0tlbNVZeaOLMGA/g6Q4WsHBxiomH" +
        "GZVlZKgLWA8GAOpYMwWbS6gP6hqEoi" +
        "IX8loOwP2rX6SNNHQ4zApwNP0FUvZU" +
        "Vfr58bTFVS2+knQ+S1aWziRD8tQ1Pe" +
        "CUsKL6AOA8EA==\nU2NhbmJvdFNESw" +
        "pjb20udF9zeXN0ZW1zLmFuZHJvaWQu" +
        "d2ViZGF2LmJldGEKMTY4MjQ2NzE5OQ" +
        "oxMTU1Njc4CjI=\n"

    /**
     * method returns license key base on application id
     */
    @JvmStatic
    fun getScanBotLicenseKey(): String {
        if (BuildConfig.APPLICATION_ID.contains(".beta")) return BETA_LICENSE_KEY
        return LICENSE_KEY
    }

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

    @JvmStatic
    fun resizeForPreview(bitmap: Bitmap): Bitmap {
        val maxW = 1000f
        val maxH = 1000f
        val oldWidth = bitmap.width.toFloat()
        val oldHeight = bitmap.height.toFloat()
        val scaleFactor = if (oldWidth > oldHeight) maxW / oldWidth else maxH / oldHeight
        val scaledWidth = (oldWidth * scaleFactor).roundToInt()
        val scaledHeight = (oldHeight * scaleFactor).roundToInt()
        return Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, false)
    }

}