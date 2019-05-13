package com.owncloud.android.ui.adapter;

import com.owncloud.android.R;
import com.owncloud.android.ui.activity.ManageAccountsActivity;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class used to test the AccountList Adapter
 */
public class AccountListAdapterTest {

    private AccountListAdapter accountListAdapter;
    private ManageAccountsActivity manageAccountsActivity;

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
        accountListAdapter = new AccountListAdapter(manageAccountsActivity, null,
                                                    new ArrayList<>(), null);
        assertEquals(0, accountListAdapter.getItemCount());
    }

    /**
     * Testing the getItemCount method, in a normal case, of having two accounts
     */
    @Test
    public void test_getItemCountNormalCase() {
        List<AccountListItem> accounts = new ArrayList<>();
        accounts.add(new AccountListItem());
        accounts.add(new AccountListItem());

        accountListAdapter = new AccountListAdapter(manageAccountsActivity, null, accounts, null);

        assertEquals(2, accountListAdapter.getItemCount());
    }

    /**
     * Testing a normal case of the getItem method
     */
    @Test
    public void test_getItem() {
        ManageAccountsActivity manageAccountsActivity = mock(ManageAccountsActivity.class, Mockito.RETURNS_DEEP_STUBS);
        when(manageAccountsActivity.getResources().getDimension(R.dimen.list_item_avatar_icon_radius))
            .thenReturn(new Float(0.1));

        List<AccountListItem> accounts = new ArrayList<>();
        accountListAdapter = new AccountListAdapter(manageAccountsActivity, null, accounts, null);

        AccountListItem accountListItem1 = new AccountListItem();
        AccountListItem accountListItem2 = new AccountListItem();
        accounts.add(accountListItem1);
        accounts.add(accountListItem2);

        assertEquals(accountListItem2, accountListAdapter.getItem(1));
    }
}
