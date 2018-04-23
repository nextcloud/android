package com.owncloud.android.ui.activities.data.activities;

import com.owncloud.android.lib.common.OwnCloudClient;

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
    OwnCloudClient ownCloudClient;

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
    public void loadActivitesReturnSuccess() {
        mActivitiesRepository.getActivities("null", mockedLoadActivitiesCallback);
        verify(serviceApi).getAllActivities(eq("null"), activitiesServiceCallbackCaptor.capture());
        activitiesServiceCallbackCaptor.getValue().onLoaded(activitiesList, ownCloudClient, "nextPageUrl");
        verify(mockedLoadActivitiesCallback).onActivitiesLoaded(eq(activitiesList), eq(ownCloudClient), eq("nextPageUrl"));
    }

    @Test
    public void loadActivitesReturnError() {
        mActivitiesRepository.getActivities("null", mockedLoadActivitiesCallback);
        verify(serviceApi).getAllActivities(eq("null"), activitiesServiceCallbackCaptor.capture());
        activitiesServiceCallbackCaptor.getValue().onError("error");
        verify(mockedLoadActivitiesCallback).onActivitiesLoadedError(eq("error"));
    }

}
