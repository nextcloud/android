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
package com.owncloud.android.ui.activities.data.files

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activities.data.files.FilesRepository.ReadRemoteFileCallback
import com.owncloud.android.ui.activities.data.files.FilesServiceApi.FilesServiceCallback
import com.owncloud.android.ui.activity.BaseActivity
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations

class RemoteFilesRepositoryTest {
    @Mock
    private val serviceApi: FilesServiceApi? = null

    @Mock
    private val mockedReadRemoteFileCallback: ReadRemoteFileCallback? = null

    @Mock
    private val baseActivity: BaseActivity? = null

    @Captor
    private val filesServiceCallbackCaptor: ArgumentCaptor<FilesServiceCallback<*>>? = null
    private var mFilesRepository: FilesRepository? = null
    private val mOCFile: OCFile? = null
    @Before
    fun setUpFilesRepository() {
        MockitoAnnotations.initMocks(this)
        mFilesRepository = RemoteFilesRepository(serviceApi!!)
    }

    @Test
    fun readRemoteFileReturnSuccess() {
        mFilesRepository!!.readRemoteFile("path", baseActivity, mockedReadRemoteFileCallback!!)
        Mockito.verify(serviceApi).readRemoteFile(
            ArgumentMatchers.eq("path"),
            ArgumentMatchers.eq(baseActivity),
            filesServiceCallbackCaptor!!.capture()
        )
        filesServiceCallbackCaptor.value.onLoaded(mOCFile)
        Mockito.verify(mockedReadRemoteFileCallback).onFileLoaded(ArgumentMatchers.eq(mOCFile))
    }

    @Test
    fun readRemoteFileReturnError() {
        mFilesRepository!!.readRemoteFile("path", baseActivity, mockedReadRemoteFileCallback!!)
        Mockito.verify(serviceApi).readRemoteFile(
            ArgumentMatchers.eq("path"),
            ArgumentMatchers.eq(baseActivity),
            filesServiceCallbackCaptor!!.capture()
        )
        filesServiceCallbackCaptor.value.onError("error")
        Mockito.verify(mockedReadRemoteFileCallback).onFileLoadError(ArgumentMatchers.eq("error"))
    }
}