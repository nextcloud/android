/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities;

import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.activities.model.Activity;
import com.owncloud.android.lib.resources.activities.model.RichElement;
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;
import com.owncloud.android.ui.activities.data.files.FilesRepository;
import com.owncloud.android.ui.activity.BaseActivity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class ActivitiesPresenterTest {

    @Mock
    private FilesRepository filesRepository;

    @Mock
    private ActivitiesContract.View view;

    @Mock
    private ActivitiesRepository activitiesRepository;

    @Mock
    private BaseActivity baseActivity;

    @Mock
    private NextcloudClient nextcloudClient;

    @Mock
    private OCFile ocFile;

    @Captor
    private ArgumentCaptor<FilesRepository.ReadRemoteFileCallback> readRemoteFileCallbackArgumentCaptor;

    @Captor
    private ArgumentCaptor<ActivitiesRepository.LoadActivitiesCallback> loadActivitiesCallbackArgumentCaptor;

    private ActivitiesPresenter activitiesPresenter;

    private List<Object> activitiesList;


    @Before
    public void setupActivitiesPresenter() {
        MockitoAnnotations.initMocks(this);
        activitiesPresenter = new ActivitiesPresenter(activitiesRepository, filesRepository, view);

        activitiesList = new ArrayList<>();
        activitiesList.add(new Activity(
                               2,
                               new Date(),
                               new Date(),
                               "comments",
                               "comments",
                               "user1",
                               "user1",
                               "admin commented",
                               "test2",
                               "icon",
                               "link",
                               "files",
                               "1",
                               "/text.txt",
                               new ArrayList<>(),
                               new RichElement())
                          );
    }

    @Test
    public void loadInitialActivitiesFromRepositoryIntoView() {
        long lastGiven = -1L;

        // When loading activities from repository is requested from presenter...
        activitiesPresenter.loadActivities(lastGiven);
        // empty list view is hidden in view
        verify(view).showLoadingMessage();
        // Repository starts retrieving activities from server
        verify(activitiesRepository).getActivities(eq(lastGiven), loadActivitiesCallbackArgumentCaptor.capture());
        // Repository returns data
        loadActivitiesCallbackArgumentCaptor.getValue().onActivitiesLoaded(activitiesList, nextcloudClient, lastGiven);
        // Progress indicator is hidden
        verify(view).setProgressIndicatorState(eq(false));
        // List of activities is shown in view.
        verify(view).showActivities(eq(activitiesList), eq(nextcloudClient), eq(lastGiven));
    }

    @Test
    public void loadFollowUpActivitiesFromRepositoryIntoView() {
        long lastGiven = 1L;

        // When loading activities from repository is requested from presenter...
        activitiesPresenter.loadActivities(lastGiven);
        // Progress indicator is shown in view
        verify(view).setProgressIndicatorState(eq(true));
        // Repository starts retrieving activities from server
        verify(activitiesRepository).getActivities(eq(lastGiven), loadActivitiesCallbackArgumentCaptor.capture());
        // Repository returns data
        loadActivitiesCallbackArgumentCaptor.getValue().onActivitiesLoaded(activitiesList, nextcloudClient, lastGiven);
        // Progress indicator is hidden
        verify(view).setProgressIndicatorState(eq(false));
        // List of activities is shown in view.
        verify(view).showActivities(eq(activitiesList), eq(nextcloudClient), eq(lastGiven));
    }

    @Test
    public void loadActivitiesFromRepositoryShowError() {
        long lastGiven = -1L;

        // When loading activities from repository is requested from presenter...
        activitiesPresenter.loadActivities(lastGiven);
        // Repository starts retrieving activities from server
        verify(activitiesRepository).getActivities(eq(lastGiven), loadActivitiesCallbackArgumentCaptor.capture());
        // Repository returns data
        loadActivitiesCallbackArgumentCaptor.getValue().onActivitiesLoadedError("error");
        // Correct error is shown in view
        verify(view).showActivitiesLoadError(eq("error"));
    }

    @Test
    public void loadRemoteFileFromRepositoryShowDetailUI() {
        // When retrieving remote file from repository...
        activitiesPresenter.openActivity("null", baseActivity);
        // Progress indicator is shown in view
        verify(view).setProgressIndicatorState(eq(true));
        // Repository retrieves remote file
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity),
                                               readRemoteFileCallbackArgumentCaptor.capture());
        // Repository returns valid file object
        readRemoteFileCallbackArgumentCaptor.getValue().onFileLoaded(ocFile);
        // Progress indicator is hidden
        verify(view).setProgressIndicatorState(eq(false));
        // File detail UI is shown
        verify(view).showActivityDetailUI(eq(ocFile));
    }

    @Test
    public void loadRemoteFileFromRepositoryShowEmptyFile() {
        // When retrieving remote file from repository...
        activitiesPresenter.openActivity("null", baseActivity);
        // Progress indicator is shown in view
        verify(view).setProgressIndicatorState(eq(true));
        // Repository retrieves remote file
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity),
                                               readRemoteFileCallbackArgumentCaptor.capture());
        // Repository returns an valid but Null value file object.
        readRemoteFileCallbackArgumentCaptor.getValue().onFileLoaded(null);
        // Progress indicator is hidden
        verify(view).setProgressIndicatorState(eq(false));
        // Returned file is null. Inform user.
        verify(view).showActivityDetailUIIsNull();
    }

    @Test
    public void loadRemoteFileFromRepositoryShowError() {
        // When retrieving remote file from repository...
        activitiesPresenter.openActivity("null", baseActivity);
        // Progress indicator is shown in view
        verify(view).setProgressIndicatorState(eq(true));
        // Repository retrieves remote file
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity),
                                               readRemoteFileCallbackArgumentCaptor.capture());
        // Repository returns valid file object
        readRemoteFileCallbackArgumentCaptor.getValue().onFileLoadError("error");
        // Progress indicator is hidden
        verify(view).setProgressIndicatorState(eq(false));
        // Error message is shown to the user.
        verify(view).showActivityDetailError(eq("error"));
    }
}
