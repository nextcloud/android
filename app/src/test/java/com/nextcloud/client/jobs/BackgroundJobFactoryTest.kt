/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs

import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.res.Resources
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.Clock
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.dao.FileDao
import com.nextcloud.client.device.DeviceInfo
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.documentscan.GeneratePDFUseCase
import com.nextcloud.client.integrations.deck.DeckApi
import com.nextcloud.client.logger.Logger
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.SyncedFolderProvider
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.utils.theme.ViewThemeUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.greenrobot.eventbus.EventBus
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

class BackgroundJobFactoryTest {

    private val context = mockk<Context>(relaxed = true)

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

    @Mock
    private lateinit var viewThemeUtils: ViewThemeUtils

    @Mock
    private lateinit var localBroadcastManager: LocalBroadcastManager

    @Mock
    private lateinit var generatePDFUseCase: GeneratePDFUseCase

    @Mock
    private lateinit var syncedFolderProvider: SyncedFolderProvider

    @Mock
    private lateinit var db: NextcloudDatabase

    @Mock private lateinit var fileDao: FileDao

    private lateinit var factory: BackgroundJobFactory

    @Before
    fun setUp() {
        mockkStatic(MainApp::class)
        every { MainApp.getAppContext() } returns context

        MockitoAnnotations.openMocks(this)

        whenever(db.fileDao()).thenReturn(fileDao)

        factory = BackgroundJobFactory(
            logger,
            preferences,
            contentResolver,
            clock,
            powerManagementService,
            { backgroundJobManager },
            accountManager,
            resources,
            dataProvider,
            uploadsStorageManager,
            connectivityService,
            notificationManager,
            eventBus,
            deckApi,
            { viewThemeUtils },
            { localBroadcastManager },
            generatePDFUseCase,
            syncedFolderProvider,
            db
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
}
