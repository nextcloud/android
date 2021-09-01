/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.jobs

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.integrations.deck.DeckApi
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.nhaarman.mockitokotlin2.whenever
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import org.greenrobot.eventbus.EventBus
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

    @Mock
    private lateinit var clock: Clock

    @Mock
    private lateinit var accountManager: UserAccountManager

    @Mock
    private lateinit var resources: Resources

    @Mock
    private lateinit var dataProvider: ArbitraryDataProvider

    @Mock
    private lateinit var logger: Logger

    @Mock
    private lateinit var uploadsStorageManager: UploadsStorageManager

    @Mock
    private lateinit var connectivityService: ConnectivityService

    @Mock
    private lateinit var notificationManager: NotificationManager

    @Mock
    private lateinit var eventBus: EventBus

    @Mock
    private lateinit var deckApi: DeckApi

    private lateinit var factory: BackgroundJobFactory

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        factory = BackgroundJobFactory(
            logger,
            preferences,
            contentResolver,
            clock,
            powerManagementService,
            Provider { backgroundJobManager },
            deviceInfo,
            accountManager,
            resources,
            dataProvider,
            uploadsStorageManager,
            connectivityService,
            notificationManager,
            eventBus,
            deckApi
        )
    }

    @Test
    fun content_observer_worker_is_created_on_api_level_24() {
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
    fun content_observer_worker_is_not_created_below_api_level_24() {
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
