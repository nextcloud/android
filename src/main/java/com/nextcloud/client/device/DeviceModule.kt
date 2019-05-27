package com.nextcloud.client.device

import android.content.Context
import android.os.PowerManager
import dagger.Module
import dagger.Provides

@Module
class DeviceModule {

    @Provides
    fun powerManagementService(context: Context): PowerManagementService {
        val platformPowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return PowerManagementServiceImpl(
            powerManager = platformPowerManager,
            deviceInfo = DeviceInfo()
        )
    }
}
