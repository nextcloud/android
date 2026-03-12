/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities

import androidx.lifecycle.LifecycleCoroutineScope
import com.nextcloud.common.NextcloudClient
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.activities.model.Activity
import com.owncloud.android.lib.resources.activities.model.RichElement
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository.LoadActivitiesCallback
import com.owncloud.android.ui.activities.data.files.FilesRepository
import com.owncloud.android.ui.activities.data.files.FilesRepository.ReadRemoteFileCallback
import com.owncloud.android.ui.activity.BaseActivity
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import java.util.Date

class ActivitiesPresenterTest {

    @Mock
    private lateinit var filesRepository: FilesRepository

    @Mock
    private lateinit var view: ActivitiesContract.View

    @Mock
    private lateinit var activitiesRepository: ActivitiesRepository

    @Mock
    private lateinit var baseActivity: BaseActivity

    @Mock
    private lateinit var nextcloudClient: NextcloudClient

    @Mock
    private lateinit var ocFile: OCFile

    @Mock
    private lateinit var lifecycleScope: LifecycleCoroutineScope

    @Captor
    private lateinit var readRemoteFileCallbackCaptor: ArgumentCaptor<ReadRemoteFileCallback>

    @Captor
    private lateinit var loadActivitiesCallbackCaptor: ArgumentCaptor<LoadActivitiesCallback>

    private lateinit var activitiesPresenter: ActivitiesPresenter
    private lateinit var activitiesList: List<Any>

    @Before
    fun setupActivitiesPresenter() {
        MockitoAnnotations.initMocks(this)
        activitiesPresenter = ActivitiesPresenter(activitiesRepository, filesRepository, view)

        activitiesList = mutableListOf(
            Activity(
                2, Date(), Date(),
                "comments", "comments",
                "user1", "user1",
                "admin commented", "test2",
                "icon", "link", "files", "1", "/text.txt",
                ArrayList(), RichElement()
            )
        )
    }

    @Test
    fun loadInitialActivitiesFromRepositoryIntoView() {
        val lastGiven = -1L

        // When loading activities from repository is requested from presenter...
        activitiesPresenter.loadActivities(lifecycleScope, lastGiven)
        // Empty list view is hidden in view
        verify(view).showLoadingMessage()
        // Repository starts retrieving activities from server
        verify(activitiesRepository).getActivities(
            eq(lifecycleScope),
            eq(lastGiven),
            loadActivitiesCallbackCaptor.capture()
        )
        // Repository returns data
        loadActivitiesCallbackCaptor.value.onActivitiesLoaded(
            activitiesList,
            nextcloudClient,
            lastGiven
        )
        // Progress indicator is hidden
        verify(view).setProgressIndicatorState(eq(false))
        // List of activities is shown in view
        verify(view).showActivities(eq(activitiesList), eq(nextcloudClient), eq(lastGiven))
    }

    @Test
    fun loadFollowUpActivitiesFromRepositoryIntoView() {
        val lastGiven = 1L

        // When loading activities from repository is requested from presenter...
        activitiesPresenter.loadActivities(lifecycleScope, lastGiven)
        // Progress indicator is shown in view
        verify(view).setProgressIndicatorState(eq(true))
        // Repository starts retrieving activities from server
        verify(activitiesRepository).getActivities(
            eq(lifecycleScope),
            eq(lastGiven),
            loadActivitiesCallbackCaptor.capture()
        )
        // Repository returns data
        loadActivitiesCallbackCaptor.value.onActivitiesLoaded(activitiesList, nextcloudClient, lastGiven)
        // Progress indicator is hidden
        verify(view).setProgressIndicatorState(eq(false))
        // List of activities is shown in view
        verify(view).showActivities(eq(activitiesList), eq(nextcloudClient), eq(lastGiven))
    }

    @Test
    fun loadActivitiesFromRepositoryShowError() {
        val lastGiven = -1L

        // When loading activities from repository is requested from presenter...
        activitiesPresenter.loadActivities(lifecycleScope, lastGiven)
        // Repository starts retrieving activities from server
        verify(activitiesRepository).getActivities(
            eq(lifecycleScope),
            eq(lastGiven),
            loadActivitiesCallbackCaptor.capture()
        )
        // Repository returns an error
        loadActivitiesCallbackCaptor.value.onActivitiesLoadedError("error")
        // Correct error is shown in view
        verify(view).showActivitiesLoadError(eq("error"))
    }

    @Test
    fun loadRemoteFileFromRepositoryShowDetailUI() {
        // When retrieving remote file from repository...
        activitiesPresenter.openActivity("null", baseActivity)
        verify(view).setProgressIndicatorState(eq(true))
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity), readRemoteFileCallbackCaptor.capture())

        // Repository returns a valid file object
        readRemoteFileCallbackCaptor.value.onFileLoaded(ocFile)
        verify(view).setProgressIndicatorState(eq(false))
        verify(view).showActivityDetailUI(eq(ocFile))
    }

    @Test
    fun loadRemoteFileFromRepositoryShowEmptyFile() {
        // When retrieving remote file from repository...
        activitiesPresenter.openActivity("null", baseActivity)
        verify(view).setProgressIndicatorState(eq(true))
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity), readRemoteFileCallbackCaptor.capture())

        // Repository returns a valid but null file object
        readRemoteFileCallbackCaptor.value.onFileLoaded(null)
        verify(view).setProgressIndicatorState(eq(false))
        verify(view).showActivityDetailUIIsNull()
    }

    @Test
    fun loadRemoteFileFromRepositoryShowError() {
        // When retrieving remote file from repository...
        activitiesPresenter.openActivity("null", baseActivity)
        verify(view).setProgressIndicatorState(eq(true))
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity), readRemoteFileCallbackCaptor.capture())

        // Repository returns an error
        readRemoteFileCallbackCaptor.value.onFileLoadError("error")
        verify(view).setProgressIndicatorState(eq(false))
        verify(view).showActivityDetailError(eq("error"))
    }
}
