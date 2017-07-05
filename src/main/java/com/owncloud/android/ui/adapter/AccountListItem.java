/**
 *   ownCloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
    private boolean mEnabled;

    /**
     * creates an account list item containing an {@link Account}.
     *
     * @param account the account
     */
    public AccountListItem(Account account) {
        mAccount = account;
        mType = TYPE_ACCOUNT;
        mEnabled = true;
    }

    public AccountListItem(Account account, boolean enabled) {
        mAccount = account;
        mType = TYPE_ACCOUNT;
        mEnabled = enabled;
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

    public void setEnabled(boolean bool) {
        mEnabled = bool;
    }

    public boolean isEnabled() {
        return mEnabled;
    }
}
