/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.files

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activities.data.files.FilesRepository.ReadRemoteFileCallback
import com.owncloud.android.ui.activities.data.files.FilesServiceApi.FilesServiceCallback
import com.owncloud.android.ui.activity.BaseActivity
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq

class RemoteFilesRepositoryTest {

    @Mock private lateinit var serviceApi: FilesServiceApi

    @Mock private lateinit var mockedReadRemoteFileCallback: ReadRemoteFileCallback

    @Mock private lateinit var baseActivity: BaseActivity

    @Mock private lateinit var ocFile: OCFile

    private lateinit var filesRepository: FilesRepository

    @Before
    fun setUpFilesRepository() {
        MockitoAnnotations.openMocks(this)
        filesRepository = RemoteFilesRepository(serviceApi)
    }

    @Test
    fun readRemoteFileReturnSuccess() {
        val captor = argumentCaptor<FilesServiceCallback<OCFile>>()

        filesRepository.readRemoteFile("path", baseActivity, mockedReadRemoteFileCallback)
        verify(serviceApi).readRemoteFile(eq("path"), eq(baseActivity), captor.capture())
        captor.firstValue.onLoaded(ocFile)
        verify(mockedReadRemoteFileCallback).onFileLoaded(eq(ocFile))
    }

    @Test
    fun readRemoteFileReturnError() {
        val captor = argumentCaptor<FilesServiceCallback<OCFile>>()

        filesRepository.readRemoteFile("path", baseActivity, mockedReadRemoteFileCallback)
        verify(serviceApi).readRemoteFile(eq("path"), eq(baseActivity), captor.capture())
        captor.firstValue.onError("error")
        verify(mockedReadRemoteFileCallback).onFileLoadError(eq("error"))
    }
}
