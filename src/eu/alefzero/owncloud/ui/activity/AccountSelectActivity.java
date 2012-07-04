package eu.alefzero.owncloud.ui.activity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.R;

public class AccountSelectActivity extends SherlockListActivity implements
        AccountManagerCallback<Boolean> {

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSherlock().getMenuInflater();
        inflater.inflate(eu.alefzero.owncloud.R.menu.account_picker, menu);
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
        Intent i = new Intent(this, FileDisplayActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
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

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item
                .getMenuInfo();
        int index = info.position;
        HashMap<String, String> map = (HashMap<String, String>) getListAdapter()
                .getItem(index);
        String accountName = map.get("NAME");
        AccountManager am = (AccountManager) getSystemService(ACCOUNT_SERVICE);
        Account accounts[] = am
                .getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
        for (Account a : accounts) {
            if (a.name.equals(accountName)) {
                am.removeAccount(a, this, mHandler);
            }
        }

        return false;
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
