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
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
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

    private lateinit var activitiesPresenter: ActivitiesPresenter
    private lateinit var activitiesList: List<Any>

    @Before
    fun setupActivitiesPresenter() {
        MockitoAnnotations.openMocks(this)
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
        val captor = argumentCaptor<LoadActivitiesCallback>()

        activitiesPresenter.loadActivities(lifecycleScope, lastGiven)
        verify(view).showLoadingMessage()
        verify(activitiesRepository).getActivities(
            eq(lifecycleScope),
            eq(lastGiven),
            captor.capture()
        )
        captor.firstValue.onActivitiesLoaded(activitiesList, nextcloudClient, lastGiven)
        verify(view).setProgressIndicatorState(eq(false))
        verify(view).showActivities(eq(activitiesList), eq(nextcloudClient), eq(lastGiven))
    }

    @Test
    fun loadFollowUpActivitiesFromRepositoryIntoView() {
        val lastGiven = 1L
        val captor = argumentCaptor<LoadActivitiesCallback>()

        activitiesPresenter.loadActivities(lifecycleScope, lastGiven)
        verify(view).setProgressIndicatorState(eq(true))
        verify(activitiesRepository).getActivities(
            eq(lifecycleScope),
            eq(lastGiven),
            captor.capture()
        )
        captor.firstValue.onActivitiesLoaded(activitiesList, nextcloudClient, lastGiven)
        verify(view).setProgressIndicatorState(eq(false))
        verify(view).showActivities(eq(activitiesList), eq(nextcloudClient), eq(lastGiven))
    }

    @Test
    fun loadActivitiesFromRepositoryShowError() {
        val lastGiven = -1L
        val captor = argumentCaptor<LoadActivitiesCallback>()

        activitiesPresenter.loadActivities(lifecycleScope, lastGiven)
        verify(activitiesRepository).getActivities(
            eq(lifecycleScope),
            eq(lastGiven),
            captor.capture()
        )
        captor.firstValue.onActivitiesLoadedError("error")
        verify(view).showActivitiesLoadError(eq("error"))
    }

    @Test
    fun loadRemoteFileFromRepositoryShowDetailUI() {
        val captor = argumentCaptor<ReadRemoteFileCallback>()

        activitiesPresenter.openActivity("null", baseActivity)
        verify(view).setProgressIndicatorState(eq(true))
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity), captor.capture())
        captor.firstValue.onFileLoaded(ocFile)
        verify(view).setProgressIndicatorState(eq(false))
        verify(view).showActivityDetailUI(eq(ocFile))
    }

    @Test
    fun loadRemoteFileFromRepositoryShowEmptyFile() {
        val captor = argumentCaptor<ReadRemoteFileCallback>()

        activitiesPresenter.openActivity("null", baseActivity)
        verify(view).setProgressIndicatorState(eq(true))
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity), captor.capture())
        captor.firstValue.onFileLoaded(null)
        verify(view).setProgressIndicatorState(eq(false))
        verify(view).showActivityDetailUIIsNull()
    }

    @Test
    fun loadRemoteFileFromRepositoryShowError() {
        val captor = argumentCaptor<ReadRemoteFileCallback>()

        activitiesPresenter.openActivity("null", baseActivity)
        verify(view).setProgressIndicatorState(eq(true))
        verify(filesRepository).readRemoteFile(eq("null"), eq(baseActivity), captor.capture())
        captor.firstValue.onFileLoadError("error")
        verify(view).setProgressIndicatorState(eq(false))
        verify(view).showActivityDetailError(eq("error"))
    }
}
