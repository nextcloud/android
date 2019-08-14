package com.owncloud.android.repository;

import android.accounts.Account;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;

class Invoker {
    public RemoteOperationResult invoke(Account account, RemoteOperation remoteOperation) {
        return remoteOperation.execute(account, MainApp.getAppContext());
    }
}
