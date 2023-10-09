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
package com.owncloud.android.ui.activities

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
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.Date

class ActivitiesPresenterTest {
    @Mock
    private val filesRepository: FilesRepository? = null

    @Mock
    private val view: ActivitiesContract.View? = null

    @Mock
    private val activitiesRepository: ActivitiesRepository? = null

    @Mock
    private val baseActivity: BaseActivity? = null

    @Mock
    private val nextcloudClient: NextcloudClient? = null

    @Mock
    private val ocFile: OCFile? = null

    @Captor
    private val readRemoteFileCallbackArgumentCaptor: ArgumentCaptor<ReadRemoteFileCallback>? = null

    @Captor
    private val loadActivitiesCallbackArgumentCaptor: ArgumentCaptor<LoadActivitiesCallback>? = null
    private var activitiesPresenter: ActivitiesPresenter? = null
    private var activitiesList: MutableList<Any>? = null
    @Before
    fun setupActivitiesPresenter() {
        MockitoAnnotations.initMocks(this)
        activitiesPresenter = ActivitiesPresenter(activitiesRepository!!, filesRepository!!, view!!)
        activitiesList = ArrayList()
        activitiesList?.add(
            Activity(
                2,
                Date(),
                Date(),
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
                ArrayList(),
                RichElement()
            )
        )
    }

    @Test
    fun loadInitialActivitiesFromRepositoryIntoView() {
        // When loading activities from repository is requested from presenter...
        activitiesPresenter!!.loadActivities(-1)
        // empty list view is hidden in view
        Mockito.verify(view)!!.showLoadingMessage()
        // Repository starts retrieving activities from server
        Mockito.verify(activitiesRepository)
            ?.getActivities(ArgumentMatchers.eq(-1), loadActivitiesCallbackArgumentCaptor!!.capture())
        // Repository returns data
        loadActivitiesCallbackArgumentCaptor?.value?.onActivitiesLoaded(activitiesList, nextcloudClient, -1)
        // Progress indicator is hidden
        Mockito.verify(view)?.setProgressIndicatorState(ArgumentMatchers.eq(false))
        // List of activities is shown in view.
        Mockito.verify(view)?.showActivities(
            ArgumentMatchers.eq<List<Any>?>(activitiesList),
            ArgumentMatchers.eq(nextcloudClient),
            ArgumentMatchers.eq(-1)
        )
    }

    @Test
    fun loadFollowUpActivitiesFromRepositoryIntoView() {
        // When loading activities from repository is requested from presenter...
        activitiesPresenter!!.loadActivities(1)
        // Progress indicator is shown in view
        Mockito.verify(view)?.setProgressIndicatorState(ArgumentMatchers.eq(true))
        // Repository starts retrieving activities from server
        Mockito.verify(activitiesRepository)
            ?.getActivities(ArgumentMatchers.eq(1), loadActivitiesCallbackArgumentCaptor!!.capture())
        // Repository returns data
        loadActivitiesCallbackArgumentCaptor!!.value.onActivitiesLoaded(activitiesList, nextcloudClient, 1)
        // Progress indicator is hidden
        Mockito.verify(view)!!.setProgressIndicatorState(ArgumentMatchers.eq(false))
        // List of activities is shown in view.
        Mockito.verify(view)!!.showActivities(
            ArgumentMatchers.eq<List<Any>?>(activitiesList),
            ArgumentMatchers.eq(nextcloudClient),
            ArgumentMatchers.eq(1)
        )
    }

    @Test
    fun loadActivitiesFromRepositoryShowError() {
        // When loading activities from repository is requested from presenter...
        activitiesPresenter!!.loadActivities(-1)
        // Repository starts retrieving activities from server
        Mockito.verify(activitiesRepository)!!
            .getActivities(ArgumentMatchers.eq(-1), loadActivitiesCallbackArgumentCaptor!!.capture())
        // Repository returns data
        loadActivitiesCallbackArgumentCaptor.value.onActivitiesLoadedError("error")
        // Correct error is shown in view
        Mockito.verify(view)!!.showActivitiesLoadError(ArgumentMatchers.eq("error"))
    }

    @Test
    fun loadRemoteFileFromRepositoryShowDetailUI() {
        // When retrieving remote file from repository...
        activitiesPresenter!!.openActivity("null", baseActivity)
        // Progress indicator is shown in view
        Mockito.verify(view)!!.setProgressIndicatorState(ArgumentMatchers.eq(true))
        // Repository retrieves remote file
        Mockito.verify(filesRepository)!!.readRemoteFile(
            ArgumentMatchers.eq("null"), ArgumentMatchers.eq(baseActivity),
            readRemoteFileCallbackArgumentCaptor!!.capture()
        )
        // Repository returns valid file object
        readRemoteFileCallbackArgumentCaptor.value.onFileLoaded(ocFile)
        // Progress indicator is hidden
        Mockito.verify(view)!!.setProgressIndicatorState(ArgumentMatchers.eq(false))
        // File detail UI is shown
        Mockito.verify(view)!!.showActivityDetailUI(ArgumentMatchers.eq(ocFile))
    }

    @Test
    fun loadRemoteFileFromRepositoryShowEmptyFile() {
        // When retrieving remote file from repository...
        activitiesPresenter!!.openActivity("null", baseActivity)
        // Progress indicator is shown in view
        Mockito.verify(view)!!.setProgressIndicatorState(ArgumentMatchers.eq(true))
        // Repository retrieves remote file
        Mockito.verify(filesRepository)!!.readRemoteFile(
            ArgumentMatchers.eq("null"), ArgumentMatchers.eq(baseActivity),
            readRemoteFileCallbackArgumentCaptor!!.capture()
        )
        // Repository returns an valid but Null value file object.
        readRemoteFileCallbackArgumentCaptor.value.onFileLoaded(null)
        // Progress indicator is hidden
        Mockito.verify(view)!!.setProgressIndicatorState(ArgumentMatchers.eq(false))
        // Returned file is null. Inform user.
        Mockito.verify(view)!!.showActivityDetailUIIsNull()
    }

    @Test
    fun loadRemoteFileFromRepositoryShowError() {
        // When retrieving remote file from repository...
        activitiesPresenter!!.openActivity("null", baseActivity)
        // Progress indicator is shown in view
        Mockito.verify(view)!!.setProgressIndicatorState(ArgumentMatchers.eq(true))
        // Repository retrieves remote file
        Mockito.verify(filesRepository)!!.readRemoteFile(
            ArgumentMatchers.eq("null"), ArgumentMatchers.eq(baseActivity),
            readRemoteFileCallbackArgumentCaptor!!.capture()
        )
        // Repository returns valid file object
        readRemoteFileCallbackArgumentCaptor.value.onFileLoadError("error")
        // Progress indicator is hidden
        Mockito.verify(view)!!.setProgressIndicatorState(ArgumentMatchers.eq(false))
        // Error message is shown to the user.
        Mockito.verify(view)!!.showActivityDetailError(ArgumentMatchers.eq("error"))
    }
}