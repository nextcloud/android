package com.owncloud.android.repository;

import android.accounts.Account;

import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;

import java.util.concurrent.Executor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class UserInfoRepository {
    private final Executor executor;
    private FileDataStorageManager storageManager;
    MutableLiveData<com.owncloud.android.datamodel.UserInfo> test = new MutableLiveData<>();

    public UserInfoRepository(Executor executor, FileDataStorageManager storageManager) {
        this.executor = executor;
        this.storageManager = storageManager;
    }

    public LiveData<com.owncloud.android.datamodel.UserInfo> getUserInfo(Account account) {
        refreshUserInfo(account);

        return test;
    }

    private void refreshUserInfo(Account account) {
        executor.execute(() -> {
            // TODO load from server only if force refresh or older than 5min
            com.owncloud.android.datamodel.UserInfo oldUserInfo = storageManager.getUserInfo(account.name);

            if (!oldUserInfo.isEmpty()) {
                test.postValue(oldUserInfo);
            }

            Log_OC.d(this, "Start refresh user info");

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            RemoteOperation getRemoteUserInfoOperation = new GetUserInfoRemoteOperation();

            // TODO how to get client better
            Invoker invoker = new Invoker();
            RemoteOperationResult result = invoker.invoke(account, getRemoteUserInfoOperation);

            if (result.isSuccess() && result.getData() != null) {
                Log_OC.d(this, "new refresh user info");

                com.owncloud.android.datamodel.UserInfo userInfo = parseUserInfo((UserInfo) result.getData().get(0),
                                                                                 account);
                test.postValue(userInfo);

                storageManager.saveUserInfo(userInfo);
            }
            // TODO error handling if fetch fails
        });
    }

    private com.owncloud.android.datamodel.UserInfo parseUserInfo(UserInfo remoteUserInfo, Account account) {
        com.owncloud.android.datamodel.UserInfo userInfo = new com.owncloud.android.datamodel.UserInfo();

        userInfo.account = account.name;
        userInfo.displayName = remoteUserInfo.displayName;
        userInfo.email = remoteUserInfo.email;
        userInfo.phone = remoteUserInfo.phone;
        userInfo.address = remoteUserInfo.address;
        userInfo.website = remoteUserInfo.website;
        userInfo.twitter = remoteUserInfo.twitter;
        userInfo.groups = remoteUserInfo.groups;
        userInfo.quota = remoteUserInfo.quota;

        return userInfo;
    }
}
