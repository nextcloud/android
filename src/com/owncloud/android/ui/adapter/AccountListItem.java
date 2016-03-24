package com.owncloud.android.ui.adapter;

import android.accounts.Account;

/**
 * Container implementation to add {@link Account}s and the add action to the list.
 */
public class AccountListItem {
    public static final int TYPE_ACCOUNT = 0;
    public static final int TYPE_ACTION_ADD = 1;

    private Account mAccount;
    private int mType;

    /**
     * creates an account list item containing an {@link Account}.
     *
     * @param account the account
     */
    public AccountListItem(Account account) {
        mAccount = account;
        mType = TYPE_ACCOUNT;
    }

    /**
     * creates an account list item flagged as add-action.
     */
    public AccountListItem() {
        mType = TYPE_ACTION_ADD;
    }

    public Account getAccount() {
        return mAccount;
    }

    public int getType() {
        return mType;
    }
}
