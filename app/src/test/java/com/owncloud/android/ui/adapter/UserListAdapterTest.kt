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
package com.owncloud.android.ui.adapter

import com.owncloud.android.R
import com.owncloud.android.ui.activity.ManageAccountsActivity
import com.owncloud.android.utils.theme.ViewThemeUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

/**
 * Class used to test the AccountList Adapter
 */
class UserListAdapterTest {
    private var userListAdapter: UserListAdapter? = null
    private var manageAccountsActivity: ManageAccountsActivity? = null

    @Mock
    private val viewThemeUtils: ViewThemeUtils? = null

    /**
     * Setting up and mocking the manageAccountsActivity class, and then mocking the method calls in the construction of
     * the object
     */
    @Before
    fun setup() {
        manageAccountsActivity = Mockito.mock(ManageAccountsActivity::class.java, Mockito.RETURNS_DEEP_STUBS)
        Mockito.`when`(manageAccountsActivity?.getResources()?.getDimension(R.dimen.list_item_avatar_icon_radius))
            .thenReturn(0.1F)
    }

    /**
     * Testing the getItemCount method, in the case of an empty List
     */
    @Test
    fun test_getItemCountEmptyList() {
        userListAdapter = UserListAdapter(
            manageAccountsActivity,
            null,
            ArrayList(),
            null,
            true,
            true,
            true,
            viewThemeUtils
        )
        Assert.assertEquals(0, userListAdapter!!.itemCount.toLong())
    }

    /**
     * Testing the getItemCount method, in a normal case, of having two accounts
     */
    @Test
    fun test_getItemCountNormalCase() {
        val accounts: MutableList<UserListItem> = ArrayList()
        accounts.add(UserListItem())
        accounts.add(UserListItem())
        userListAdapter = UserListAdapter(
            manageAccountsActivity,
            null,
            accounts,
            null,
            true,
            true,
            true,
            viewThemeUtils
        )
        Assert.assertEquals(2, userListAdapter!!.itemCount.toLong())
    }

    /**
     * Testing a normal case of the getItem method
     */
    @Test
    fun test_getItem() {
        val manageAccountsActivity = Mockito.mock(
            ManageAccountsActivity::class.java, Mockito.RETURNS_DEEP_STUBS
        )
        Mockito.`when`(manageAccountsActivity.resources.getDimension(R.dimen.list_item_avatar_icon_radius))
            .thenReturn(0.1F)
        val accounts: MutableList<UserListItem> = ArrayList()
        userListAdapter = UserListAdapter(
            manageAccountsActivity,
            null,
            accounts,
            null,
            true,
            true,
            true,
            viewThemeUtils
        )
        val userListItem1 = UserListItem()
        val userListItem2 = UserListItem()
        accounts.add(userListItem1)
        accounts.add(userListItem2)
        Assert.assertEquals(userListItem2, userListAdapter!!.getItem(1))
    }
}