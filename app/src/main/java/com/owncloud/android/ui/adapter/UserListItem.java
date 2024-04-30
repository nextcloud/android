/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
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
