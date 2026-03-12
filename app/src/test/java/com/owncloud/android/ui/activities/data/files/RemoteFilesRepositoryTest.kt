/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activities.data.files;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.BaseActivity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

public class RemoteFilesRepositoryTest {

    @Mock
    private FilesServiceApi serviceApi;

    @Mock
    private FilesRepository.ReadRemoteFileCallback mockedReadRemoteFileCallback;

    @Mock
    private BaseActivity baseActivity;

    @Captor
    private ArgumentCaptor<FilesServiceApi.FilesServiceCallback> filesServiceCallbackCaptor;

    private FilesRepository mFilesRepository;

    private OCFile mOCFile = null;

    @Before
    public void setUpFilesRepository() {
        MockitoAnnotations.initMocks(this);
        mFilesRepository = new RemoteFilesRepository(serviceApi);
    }

    @Test
    public void readRemoteFileReturnSuccess() {
        mFilesRepository.readRemoteFile("path", baseActivity, mockedReadRemoteFileCallback);
        verify(serviceApi).readRemoteFile(eq("path"), eq(baseActivity), filesServiceCallbackCaptor.capture());
        filesServiceCallbackCaptor.getValue().onLoaded(mOCFile);
        verify(mockedReadRemoteFileCallback).onFileLoaded(eq(mOCFile));
    }

    @Test
    public void readRemoteFileReturnError() {
        mFilesRepository.readRemoteFile("path", baseActivity, mockedReadRemoteFileCallback);
        verify(serviceApi).readRemoteFile(eq("path"), eq(baseActivity), filesServiceCallbackCaptor.capture());
        filesServiceCallbackCaptor.getValue().onError("error");
        verify(mockedReadRemoteFileCallback).onFileLoadError(eq("error"));
    }
}
