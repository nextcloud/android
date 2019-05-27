package com.nextcloud.client.device

import android.os.Build

class DeviceInfo {
    val vendor: String = Build.MANUFACTURER.toLowerCase()
    val apiLevel: Int = Build.VERSION.SDK_INT
}
