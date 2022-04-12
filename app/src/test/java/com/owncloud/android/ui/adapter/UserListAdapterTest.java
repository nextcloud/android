/*
 *   Nextcloud Android client application
 *
 *   @author Nick Antoniou
 *   Copyright (C) 2019 Nick Antoniou
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

import com.owncloud.android.R;
import com.owncloud.android.ui.activity.ManageAccountsActivity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class used to test the AccountList Adapter
 */
public class UserListAdapterTest {

    private UserListAdapter userListAdapter;
    private ManageAccountsActivity manageAccountsActivity;

    /**
     * Setting up and mocking the manageAccountsActivity class, and then mocking the method calls in
     * the construction of the object
     */
    @Before
    public void setup() {
        manageAccountsActivity = mock(ManageAccountsActivity.class, Mockito.RETURNS_DEEP_STUBS);
        when(manageAccountsActivity.getResources().getDimension(R.dimen.list_item_avatar_icon_radius))
            .thenReturn(new Float(0.1));
    }

    /**
     * Testing the getItemCount method, in the case of an empty List
     */
    @Test
    public void test_getItemCountEmptyList() {
        userListAdapter = new UserListAdapter(manageAccountsActivity,
                                              null,
                                              new ArrayList<>(),
                                              null,
                                              true,
                                              true);
        assertEquals(0, userListAdapter.getItemCount());
    }

    /**
     * Testing the getItemCount method, in a normal case, of having two accounts
     */
    @Test
    public void test_getItemCountNormalCase() {
        List<UserListItem> accounts = new ArrayList<>();
        accounts.add(new UserListItem());
        accounts.add(new UserListItem());

        userListAdapter = new UserListAdapter(manageAccountsActivity,
                                              null,
                                              accounts,
                                              null,
                                              true,
                                              true);

        assertEquals(2, userListAdapter.getItemCount());
    }

    /**
     * Testing a normal case of the getItem method
     */
    @Test
    public void test_getItem() {
        ManageAccountsActivity manageAccountsActivity = mock(ManageAccountsActivity.class, Mockito.RETURNS_DEEP_STUBS);
        when(manageAccountsActivity.getResources().getDimension(R.dimen.list_item_avatar_icon_radius))
            .thenReturn(new Float(0.1));

        List<UserListItem> accounts = new ArrayList<>();
        userListAdapter = new UserListAdapter(manageAccountsActivity,
                                              null,
                                              accounts,
                                              null,
                                              true,
                                              true);

        UserListItem userListItem1 = new UserListItem();
        UserListItem userListItem2 = new UserListItem();
        accounts.add(userListItem1);
        accounts.add(userListItem2);

        assertEquals(userListItem2, userListAdapter.getItem(1));
    }
}
