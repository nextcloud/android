/*
 *   Nextcloud Android client application
 *
 *   @author Andy Scherzinger
 *   @author Chris Narkiewicz <hello@ezaquarii.com>
 *
 *   Copyright (C) 2016 Andy Scherzinger
 *   Copyright (C) 2016 ownCloud Inc.
 *   Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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

import com.nextcloud.client.account.User;

/**
 * Container implementation to add {@link Account}s and the add action to the list.
 */
public class UserListItem {
    static final int TYPE_ACCOUNT = 0;
    static final int TYPE_ACTION_ADD = 1;

    private User user;
    private int type;
    private boolean enabled;

    /**
     * creates an account list item containing an {@link Account}.
     *
     * @param user the account
     */
    public UserListItem(User user) {
        this.user = user;
        this.type = TYPE_ACCOUNT;
        this.enabled = true;
    }

    public UserListItem(User user, boolean enabled) {
        this.user = user;
        this.type = TYPE_ACCOUNT;
        this.enabled = enabled;
    }

    /**
     * creates an account list item flagged as add-action.
     */
    public UserListItem() {
        type = TYPE_ACTION_ADD;
    }

    public User getUser() {
        return user;
    }

    public int getType() {
        return type;
    }

    public void setEnabled(boolean bool) {
        enabled = bool;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
