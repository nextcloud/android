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
    private const val LICENSE_KEY = "eTa/V1k8ZE3z+yK0iA1KK1oQVwHgsy" +
        "JtZ9eaJeNpaGjqvRY2+4IZq8uVY/wU" +
        "xzHz4h64P9B5vPTQz9eERr2rWAQylq" +
        "OkioHEGRZ9HsLW+NPnixQv88JOZ3fX" +
        "UzP7rBuRLxkyy7RKuNo/FHwmV31zOf" +
        "JjdP3faauKIcb5BgLY/SFJ/1MsotK2" +
        "JiOIYw5/cj/FkLq37WkeJR+QkD17vJ" +
        "GaOVZHv/HRS9xj2QUZXFzRcFp/c9yF" +
        "FUAZrui1CeBBfHA9uyO0ke4hBzNb9M" +
        "VpXSM/cNt654T06jOSiSkGjB52ejNN" +
        "eF61DwKNKpVFXV27DUsqBsMaOEMSb2" +
        "U5iqCEkwWTuQ==\nU2NhbmJvdFNESw" +
        "pjb20udF9zeXN0ZW1zLmFuZHJvaWQu" +
        "d2ViZGF2CjE2NTA5MzExOTkKMTE1NT" +
        "Y3OAoy\n"

    //beta license key will be valid for application id: com.t_systems.android.webdav.beta
    private const val BETA_LICENSE_KEY = "jniyLJLmVDKsiftoxYhOOv64Y0SSFm" +
        "JYfEzhNn49ALAfc5/rfNi2OrNEYSzi" +
        "GIkGfdhPU8X/xlRr70rp1r59/hwp6o" +
        "fwtvDEviaIDb+VstFfdgUoucVErpKM" +
        "AttZeOqAiD7eFbmPtge7pR336CgPd3" +
        "BvulxezG17I+95qmjwsZPmGxxRLDwE" +
        "biBDwKXsWgSqaAOvg5BqtHK3tvmLwy" +
        "vhvPEHysOIA8GrVbd2yYhfeOE/XBWv" +
        "cKXvLtfBqKToEa/+3iSd6yS00nDnjK" +
        "9c0pKUprP0ZD9klGY2xrFMKKkBX5pZ" +
        "zzKrWU4ix18RtZheqyj3hn98uiH98l" +
        "T69324NUnlLA==\nU2NhbmJvdFNESw" +
        "pjb20udF9zeXN0ZW1zLmFuZHJvaWQu" +
        "d2ViZGF2LmJldGEKMTY1MDkzMTE5OQ" +
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