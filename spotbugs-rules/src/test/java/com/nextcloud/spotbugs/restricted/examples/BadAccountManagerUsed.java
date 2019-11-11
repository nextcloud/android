package com.nextcloud.spotbugs.restricted.examples;

import android.accounts.AccountManager;

public class BadAccountManagerUsed {
    private static final AccountManager sAccountManager = new AccountManager();
    private AccountManager mAccountManager = new AccountManager();

    public void useStatic() {
        sAccountManager.getAccounts();
    }

    public void useMember() {
        mAccountManager.getAccounts();
    }

    public void getFromContext() {
        AccountManager am = AccountManager.get();
        am.getAccounts();
    }
}
