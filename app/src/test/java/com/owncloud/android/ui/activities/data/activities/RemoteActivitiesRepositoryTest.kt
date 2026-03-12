/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.activities

import androidx.lifecycle.LifecycleCoroutineScope
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository.LoadActivitiesCallback
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApi.ActivitiesServiceCallback
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

class RemoteActivitiesRepositoryTest {

    @Mock
    private lateinit var serviceApi: ActivitiesServiceApi

    @Mock
    private lateinit var mockedLoadActivitiesCallback: LoadActivitiesCallback

    @Mock
    private lateinit var nextcloudClient: NextcloudClient

    @Mock
    private lateinit var lifecycleScope: LifecycleCoroutineScope

    @Captor
    private lateinit var activitiesServiceCallbackCaptor: ArgumentCaptor<ActivitiesServiceCallback<List<Any>>>

    private lateinit var activitiesRepository: ActivitiesRepository
    private lateinit var activitiesList: List<Any>

    @Before
    fun setUpActivitiesRepository() {
        MockitoAnnotations.openMocks(this)
        activitiesRepository = RemoteActivitiesRepository(serviceApi)
        activitiesList = mutableListOf()
    }

    @Test
    fun loadActivitiesReturnSuccess() {
        val lastGiven = -1L

        activitiesRepository.getActivities(lifecycleScope, lastGiven, mockedLoadActivitiesCallback)
        verify(serviceApi).getAllActivities(
            eq(lifecycleScope),
            eq(lastGiven),
            activitiesServiceCallbackCaptor.capture()
        )
        activitiesServiceCallbackCaptor.value.onLoaded(activitiesList, nextcloudClient, lastGiven)
        verify(mockedLoadActivitiesCallback).onActivitiesLoaded(eq(activitiesList), eq(nextcloudClient), eq(lastGiven))
    }

    @Test
    fun loadActivitiesReturnError() {
        val lastGiven = -1L

        activitiesRepository.getActivities(lifecycleScope, lastGiven, mockedLoadActivitiesCallback)
        verify(serviceApi).getAllActivities(
            eq(lifecycleScope),
            eq(lastGiven),
            activitiesServiceCallbackCaptor.capture()
        )
        activitiesServiceCallbackCaptor.value.onError("error")
        verify(mockedLoadActivitiesCallback).onActivitiesLoadedError(eq("error"))
    }
}
