package com.owncloud.android.repository;

import android.accounts.Account;
import android.content.Context;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.concurrent.Executor;

import androidx.lifecycle.LiveData;

import static java.util.Collections.singletonList;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserInfoRepositoryTest {

    @Mock
    private Executor executor;
    @Mock
    private Context context;
    @Mock
    private Invoker invoker;
    @Mock
    private FileDataStorageManager storageManager;

    @Test
    public void getUserInfo() {
        // init
        Account accountMock = mock(Account.class);
        // when(accountMock.name).thenReturn("accountName");

        RemoteOperationResult remoteOperationResult = new RemoteOperationResult(RemoteOperationResult.ResultCode.OK);

        UserInfo userInfoMock = new UserInfo();
        userInfoMock.displayName = "displayName";

        // LiveData userInfoResult = mock(LiveData.class);
        // when(userInfoDao.load(any())).thenReturn(userInfoResult);

        remoteOperationResult.setData(new ArrayList<>(singletonList(userInfoMock)));
        when(invoker.invoke(any(), any())).thenReturn(remoteOperationResult);

        UserInfoRepository sut = new UserInfoRepository(executor, storageManager);

        // test
        LiveData<com.owncloud.android.datamodel.UserInfo> userInfo = sut.getUserInfo(accountMock);

        // verify result
        assertTrue(userInfo != null);

        // verify local dao call
        ArgumentCaptor<com.owncloud.android.datamodel.UserInfo> userInfoArgumentCaptor = ArgumentCaptor.forClass(com.owncloud.android.datamodel.UserInfo.class);
        // TODO re-enable verify(userInfoDao).save(userInfoArgumentCaptor.capture());

        assertThat(userInfoArgumentCaptor.getValue().getDisplayName(), is("displayName"));
        // verify(userInfoDao).load("accountName");

        // verify remote lib call
        ArgumentCaptor<Account> accountArgumentCaptor = ArgumentCaptor.forClass(Account.class);
        ArgumentCaptor<RemoteOperation> remoteOperationArgumentCaptor = ArgumentCaptor.forClass(RemoteOperation.class);
        verify(invoker.invoke(accountArgumentCaptor.capture(), remoteOperationArgumentCaptor.capture()));

        assertSame(accountArgumentCaptor.getValue(), accountMock);
        assertThat(remoteOperationArgumentCaptor.getValue(), is(instanceOf(GetUserInfoRemoteOperation.class)));
    }
}
