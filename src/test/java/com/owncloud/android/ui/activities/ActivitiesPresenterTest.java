/**
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
package com.owncloud.android.ui.activities;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.activities.models.Activity;
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
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class ActivitiesPresenterTest {

    @Mock
    private FilesRepository mFileRepository;

    @Mock
    private ActivitiesContract.View mView;

    @Mock
    private ActivitiesRepository mActivitiesRepository;

    @Mock
    private BaseActivity mBaseActivity;

    @Mock
    private OwnCloudClient mOwnCloudClient;

    @Mock
    private OCFile mOCFile;

    @Captor
    private ArgumentCaptor<FilesRepository.ReadRemoteFileCallback> mReadRemoteFilleCallbackCaptor;

    @Captor
    private ArgumentCaptor<ActivitiesRepository.LoadActivitiesCallback> mLoadActivitiesCallbackCaptor;

    private ActivitiesPresenter mPresenter;

    private List<Object> activitiesList;


    @Before
    public void setupActivitiesPresenter() {
        MockitoAnnotations.initMocks(this);
        mPresenter = new ActivitiesPresenter(mActivitiesRepository, mFileRepository, mView);

        activitiesList = new ArrayList<>();
        activitiesList.add(new Activity());
    }

    @Test
    public void loadActivitiesFromRepositoryIntoView() {
        // When loading activities from repository is requested from presenter...
        mPresenter.loadActivities(null);
        // Progress indicator is shown in view
        verify(mView).setProgressIndicatorState(eq(true));
        // Repository starts retrieving activities from server
        verify(mActivitiesRepository).getActivities(eq(null), mLoadActivitiesCallbackCaptor.capture());
        // Repository returns data
        mLoadActivitiesCallbackCaptor.getValue().onActivitiesLoaded(activitiesList,
                mOwnCloudClient, null);
        // Progress indicator is hidden
        verify(mView).setProgressIndicatorState(eq(false));
        // List of activities is shown in view.
        verify(mView).showActivities(eq(activitiesList), eq(mOwnCloudClient), eq(null));
    }

    @Test
    public void loadActivitiesFromRepositoryShowError() {
        // When loading activities from repository is requested from presenter...
        mPresenter.loadActivities(null);
        // Progress indicator is shown in view
        verify(mView).setProgressIndicatorState(eq(true));
        // Repository starts retrieving activities from server
        verify(mActivitiesRepository).getActivities(eq(null), mLoadActivitiesCallbackCaptor.capture());
        // Repository returns data
        mLoadActivitiesCallbackCaptor.getValue().onActivitiesLoadedError("error");
        // Progress indicator is hidden
        verify(mView).setProgressIndicatorState(eq(false));
        // Correct error is shown in view
        verify(mView).showActivitiesLoadError(eq("error"));
    }

    @Test
    public void loadRemoteFileFromRepositoryShowDetailUI() {
        // When retrieving remote file from repository...
        mPresenter.openActivity("null", mBaseActivity, true);
        // Progress indicator is shown in view
        verify(mView).setProgressIndicatorState(eq(true));
        // Repository retrieves remote file
        verify(mFileRepository).readRemoteFile(eq("null"), eq(mBaseActivity), eq(true),
                mReadRemoteFilleCallbackCaptor.capture());
        // Repository returns valid file object
        mReadRemoteFilleCallbackCaptor.getValue().onFileLoaded(mOCFile);
        // Progress indicator is hidden
        verify(mView).setProgressIndicatorState(eq(false));
        // File detail UI is shown
        verify(mView).showActivityDetailUI(eq(mOCFile));
    }

    @Test
    public void loadRemoteFileFromRepositoryShowEmptyFile() {
        // When retrieving remote file from repository...
        mPresenter.openActivity("null", mBaseActivity, true);
        // Progress indicator is shown in view
        verify(mView).setProgressIndicatorState(eq(true));
        // Repository retrieves remote file
        verify(mFileRepository).readRemoteFile(eq("null"), eq(mBaseActivity), eq(true),
                mReadRemoteFilleCallbackCaptor.capture());
        // Repository returns an valid but Null value file object.
        mReadRemoteFilleCallbackCaptor.getValue().onFileLoaded(null);
        // Progress indicator is hidden
        verify(mView).setProgressIndicatorState(eq(false));
        // Returned file is null. Inform user.
        verify(mView).showActivityDetailUIIsNull();
    }

    @Test
    public void loadRemoteFileFromRepositoryShowError() {
        // When retrieving remote file from repository...
        mPresenter.openActivity("null", mBaseActivity, true);
        // Progress indicator is shown in view
        verify(mView).setProgressIndicatorState(eq(true));
        // Repository retrieves remote file
        verify(mFileRepository).readRemoteFile(eq("null"), eq(mBaseActivity), eq(true),
                mReadRemoteFilleCallbackCaptor.capture());
        // Repository returns valid file object
        mReadRemoteFilleCallbackCaptor.getValue().onFileLoadError("error");
        // Progress indicator is hidden
        verify(mView).setProgressIndicatorState(eq(false));
        // Error message is shown to the user.
        verify(mView).showActivityDetailError(eq("error"));
    }
}
