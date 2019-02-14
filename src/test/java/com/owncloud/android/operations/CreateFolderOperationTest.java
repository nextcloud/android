package com.owncloud.android.operations;

import com.owncloud.android.FileDataStorageManagerLocal;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CreateFolderOperationTest {
    private FileDataStorageManager fileDataStorageManager = new FileDataStorageManagerLocal();

    @Mock OwnCloudClient client;

    @Mock CreateFolderRemoteOperation createFolderRemoteOperation;
    @Mock ReadFolderRemoteOperation readFolderRemoteOperation;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCreateFolder() {
        // setup
        String path = "/testFolder/";
        CreateFolderOperation sut = spy(new CreateFolderOperation(path, true));

        when(createFolderRemoteOperation.execute(any()))
            .thenReturn(new RemoteOperationResult(RemoteOperationResult.ResultCode.OK));

        RemoteOperationResult result = new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);
        ArrayList<Object> list = new ArrayList<>();
        list.add(new RemoteFile("/testFolder"));

        result.setData(list);

        when(readFolderRemoteOperation.execute(any(OwnCloudClient.class))).thenReturn(result);
        doReturn(createFolderRemoteOperation).
            when(sut).createCreateFolderRemoteOperation(any(String.class), any(Boolean.class));
        doReturn(readFolderRemoteOperation)
            .when(sut).createReadFolderRemoteOperation(any(String.class));

        // tests
        // folder does not exist yet
        assertNull(fileDataStorageManager.getFileByPath(path));

        // run
        RemoteOperationResult syncResult = sut.execute(client, fileDataStorageManager);

        // verify
        assertTrue(syncResult.isSuccess());

        // folder exists
        assertTrue(fileDataStorageManager.getFileByPath(path).isFolder());
    }
}
