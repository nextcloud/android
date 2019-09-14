package com.nextcloud.client.jobs

import android.content.ContentResolver
import android.content.Context
import android.os.Build
import androidx.work.WorkerParameters
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.preferences.AppPreferences
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import javax.inject.Provider

class BackgroundJobFactoryTest {

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var params: WorkerParameters

    @Mock
    private lateinit var contentResolver: ContentResolver

    @Mock
    private lateinit var preferences: AppPreferences

    @Mock
    private lateinit var powerManagementService: PowerManagementService

    @Mock
    private lateinit var backgroundJobManager: BackgroundJobManager

    @Mock
    private lateinit var deviceInfo: DeviceInfo

    private lateinit var factory: BackgroundJobFactory

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        factory = BackgroundJobFactory(
            preferences,
            contentResolver,
            powerManagementService,
            Provider { backgroundJobManager },
            deviceInfo
        )
    }

    @Test
    fun `worker is created on api level 24+`() {
        // GIVEN
        //      api level is > 24
        //      content URI trigger is supported
        whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.N)

        // WHEN
        //      factory is called to create content observer worker
        val worker = factory.createWorker(context, ContentObserverWork::class.java.name, params)

        // THEN
        //      factory creates a worker compatible with API level
        assertNotNull(worker)
    }

    @Test
    fun `worker is not created below api level 24`() {
        // GIVEN
        //      api level is < 24
        //      content URI trigger is not supported
        whenever(deviceInfo.apiLevel).thenReturn(Build.VERSION_CODES.M)

        // WHEN
        //      factory is called to create content observer worker
        val worker = factory.createWorker(context, ContentObserverWork::class.java.name, params)

        // THEN
        //      factory does not create a worker incompatible with API level
        assertNull(worker)
    }
}
