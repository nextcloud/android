/*
 *   Nextcloud Android client application
 *
 *   Copyright (C) 2018 Edvard Holst
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activities.data.activities

import com.nextcloud.common.NextcloudClient
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository.LoadActivitiesCallback
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApi.ActivitiesServiceCallback
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class RemoteActivitiesRepositoryTest {
    @Mock
    var serviceApi: ActivitiesServiceApi? = null

    @Mock
    var mockedLoadActivitiesCallback: LoadActivitiesCallback? = null

    @Mock
    var nextcloudClient: NextcloudClient? = null

    @Captor
    private val activitiesServiceCallbackCaptor: ArgumentCaptor<ActivitiesServiceCallback<*>>? = null
    private var mActivitiesRepository: ActivitiesRepository? = null
    private var activitiesList: List<Any>? = null
    @Before
    fun setUpActivitiesRepository() {
        MockitoAnnotations.initMocks(this)
        mActivitiesRepository = RemoteActivitiesRepository(serviceApi!!)
        activitiesList = ArrayList()
    }

    @Test
    fun loadActivitiesReturnSuccess() {
        mActivitiesRepository!!.getActivities(-1, mockedLoadActivitiesCallback!!)
        Mockito.verify(serviceApi)
            .getAllActivities(ArgumentMatchers.eq(-1), activitiesServiceCallbackCaptor!!.capture())
        activitiesServiceCallbackCaptor.value.onLoaded(activitiesList, nextcloudClient, -1)
        Mockito.verify(mockedLoadActivitiesCallback).onActivitiesLoaded(
            ArgumentMatchers.eq(activitiesList),
            ArgumentMatchers.eq(nextcloudClient),
            ArgumentMatchers.eq(-1)
        )
    }

    @Test
    fun loadActivitiesReturnError() {
        mActivitiesRepository!!.getActivities(-1, mockedLoadActivitiesCallback!!)
        Mockito.verify(serviceApi)
            .getAllActivities(ArgumentMatchers.eq(-1), activitiesServiceCallbackCaptor!!.capture())
        activitiesServiceCallbackCaptor.value.onError("error")
        Mockito.verify(mockedLoadActivitiesCallback).onActivitiesLoadedError(ArgumentMatchers.eq("error"))
    }
}