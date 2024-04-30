/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Nick Antoniou <nikolasea@windowslive.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import com.owncloud.android.R;
import com.owncloud.android.ui.activity.ManageAccountsActivity;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
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

    @Mock
    private ViewThemeUtils viewThemeUtils;

    /**
     * Setting up and mocking the manageAccountsActivity class, and then mocking the method calls in the construction of
     * the object
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
                                              true,
                                              true,
                                              viewThemeUtils);
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
                                              true,
                                              true,
                                              viewThemeUtils);

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
                                              true,
                                              true,
                                              viewThemeUtils);

        UserListItem userListItem1 = new UserListItem();
        UserListItem userListItem2 = new UserListItem();
        accounts.add(userListItem1);
        accounts.add(userListItem2);

        assertEquals(userListItem2, userListAdapter.getItem(1));
    }
}
