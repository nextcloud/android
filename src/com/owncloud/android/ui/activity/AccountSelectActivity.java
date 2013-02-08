/* ownCloud Android client application
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.ui.activity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;

import com.owncloud.android.R;

public class AccountSelectActivity extends SherlockListActivity implements
        AccountManagerCallback<Boolean> {

    private static final String  TAG = "AccountSelectActivity";
    
    private static final String PREVIOUS_ACCOUNT_KEY = "ACCOUNT";
    
    private final Handler mHandler = new Handler();
    private Account mPreviousAccount = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mPreviousAccount = savedInstanceState.getParcelable(PREVIOUS_ACCOUNT_KEY);
        } else {
            mPreviousAccount = AccountUtils.getCurrentOwnCloudAccount(this);
        }
        
        ActionBar action_bar = getSupportActionBar();
        action_bar.setDisplayShowTitleEnabled(true);
        action_bar.setDisplayHomeAsUpEnabled(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        populateAccountList();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        if (this.isFinishing()) {
            Account current = AccountUtils.getCurrentOwnCloudAccount(this);
            if ((mPreviousAccount == null && current != null) || 
                (mPreviousAccount != null && !mPreviousAccount.equals(current))) {
                /// the account set as default changed since this activity was created 
            
                // trigger synchronization
                ContentResolver.cancelSync(null, AccountAuthenticator.AUTH_TOKEN_TYPE);
                Bundle bundle = new Bundle();
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                ContentResolver.requestSync(AccountUtils.getCurrentOwnCloudAccount(this), AccountAuthenticator.AUTH_TOKEN_TYPE, bundle);
                
                // restart the main activity
                Intent i = new Intent(this, FileDisplayActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
        inflater.inflate(R.menu.account_picker, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.account_picker_long_click, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        String accountName = ((TextView) v.findViewById(android.R.id.text1))
                .getText().toString();
        AccountUtils.setCurrentOwnCloudAccount(this, accountName);
        finish();   // immediate exit
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        if (item.getItemId() == R.id.createAccount) {
            Intent intent = new Intent(
                    android.provider.Settings.ACTION_ADD_ACCOUNT);
            intent.putExtra("authorities",
                    new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
            startActivity(intent);
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        int index = info.position;
        HashMap<String, String> map = null;
        try {
            map = (HashMap<String, String>) getListAdapter().getItem(index);
        } catch (ClassCastException e) {
            Log.wtf(TAG, "getitem(index) from list adapter did not return hashmap, bailing out");
            return false;
        }
        
        String accountName = map.get("NAME");
        AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account accounts[] = am
                .getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
        for (Account a : accounts) {
            if (a.name.equals(accountName)) {
                am.removeAccount(a, this, mHandler);
            }
        }

        return true;
    }

    private void populateAccountList() {
        AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account accounts[] = am
                .getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
        LinkedList<HashMap<String, String>> ll = new LinkedList<HashMap<String, String>>();
        for (Account a : accounts) {
            HashMap<String, String> h = new HashMap<String, String>();
            h.put("NAME", a.name);
            h.put("VER",
                    "ownCloud version: "
                            + am.getUserData(a,
                                    AccountAuthenticator.KEY_OC_VERSION));
            ll.add(h);
        }

        setListAdapter(new AccountCheckedSimpleAdepter(this, ll,
                android.R.layout.simple_list_item_single_choice,
                new String[] { "NAME" }, new int[] { android.R.id.text1 }));
        registerForContextMenu(getListView());
    }

    @Override
    public void run(AccountManagerFuture<Boolean> future) {
        if (future.isDone()) {
            Account a = AccountUtils.getCurrentOwnCloudAccount(this);
            String accountName = "";
            if (a == null) {
                Account[] accounts = AccountManager.get(this)
                        .getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
                if (accounts.length != 0)
                    accountName = accounts[0].name;
                AccountUtils.setCurrentOwnCloudAccount(this, accountName);
            }
            populateAccountList();
        }
    }

    private class AccountCheckedSimpleAdepter extends SimpleAdapter {
        private Account mCurrentAccount;
        private List<? extends Map<String, ?>> mPrivateData;

        public AccountCheckedSimpleAdepter(Context context,
                List<? extends Map<String, ?>> data, int resource,
                String[] from, int[] to) {
            super(context, data, resource, from, to);
            mCurrentAccount = AccountUtils
                    .getCurrentOwnCloudAccount(AccountSelectActivity.this);
            mPrivateData = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            CheckedTextView ctv = (CheckedTextView) v
                    .findViewById(android.R.id.text1);
            if (mPrivateData.get(position).get("NAME")
                    .equals(mCurrentAccount.name)) {
                ctv.setChecked(true);
            }
            return v;
        }

    }

}
