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
package com.owncloud.android.ui.activities.data.files;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.BaseActivity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.eq;
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
