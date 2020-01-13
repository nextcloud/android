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
package com.owncloud.android.ui.activities.data.activities;

import com.nextcloud.common.NextcloudClient;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class RemoteActivitiesRepositoryTest {

    @Mock
    ActivitiesServiceApi serviceApi;

    @Mock
    ActivitiesRepository.LoadActivitiesCallback mockedLoadActivitiesCallback;

    @Mock
    NextcloudClient nextcloudClient;

    @Captor
    private ArgumentCaptor<ActivitiesServiceApi.ActivitiesServiceCallback> activitiesServiceCallbackCaptor;

    private ActivitiesRepository mActivitiesRepository;

    private List<Object> activitiesList;

    @Before
    public void setUpActivitiesRepository() {
        MockitoAnnotations.initMocks(this);
        mActivitiesRepository = new RemoteActivitiesRepository(serviceApi);
        activitiesList = new ArrayList<>();
    }

    @Test
    public void loadActivitiesReturnSuccess() {
        mActivitiesRepository.getActivities(-1, mockedLoadActivitiesCallback);
        verify(serviceApi).getAllActivities(eq(-1), activitiesServiceCallbackCaptor.capture());
        activitiesServiceCallbackCaptor.getValue().onLoaded(activitiesList, nextcloudClient, -1);
        verify(mockedLoadActivitiesCallback).onActivitiesLoaded(eq(activitiesList), eq(nextcloudClient), eq(-1));
    }

    @Test
    public void loadActivitiesReturnError() {
        mActivitiesRepository.getActivities(-1, mockedLoadActivitiesCallback);
        verify(serviceApi).getAllActivities(eq(-1), activitiesServiceCallbackCaptor.capture());
        activitiesServiceCallbackCaptor.getValue().onError("error");
        verify(mockedLoadActivitiesCallback).onActivitiesLoadedError(eq("error"));
    }

}
