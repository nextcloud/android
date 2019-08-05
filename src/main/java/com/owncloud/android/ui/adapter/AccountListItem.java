/*
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 ownCloud Inc.
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

package com.owncloud.android.ui.adapter;

import android.accounts.Account;

/**
 * Container implementation to add {@link Account}s and the add action to the list.
 */
public class AccountListItem {
    static final int TYPE_ACCOUNT = 0;
    static final int TYPE_ACTION_ADD = 1;

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
