/* ownCloud Android client application
 *   Copyright (C) 2011 Bartek Przybylski
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

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.ui.adapter.LandingScreenAdapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.Toast;
import com.owncloud.android.R;

/**
 * This activity is used as a landing page when the user first opens this app.
 * 
 * @author Lennart Rosam
 * 
 */
public class LandingActivity extends SherlockFragmentActivity implements
        OnClickListener, OnItemClickListener {

    public static final int DIALOG_SETUP_ACCOUNT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Fill the grid view of the landing screen with icons
        GridView landingScreenItems = (GridView) findViewById(R.id.homeScreenGrid);
        landingScreenItems.setAdapter(new LandingScreenAdapter(this));
        landingScreenItems.setOnItemClickListener(this);

        // Check, if there are ownCloud accounts
        if (!accountsAreSetup()) {
            showDialog(DIALOG_SETUP_ACCOUNT);
        } else {
            // Start device tracking service
            Intent locationServiceIntent = new Intent();
            locationServiceIntent
                    .setAction("com.owncloud.android.location.LocationLauncher");
            sendBroadcast(locationServiceIntent);
        }

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // Check, if there are ownCloud accounts
        if (!accountsAreSetup()) {
            showDialog(DIALOG_SETUP_ACCOUNT);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Check, if there are ownCloud accounts
        if (!accountsAreSetup()) {
            showDialog(DIALOG_SETUP_ACCOUNT);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        Dialog dialog;
        switch (id) {
        case DIALOG_SETUP_ACCOUNT:
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.main_tit_accsetup);
            builder.setMessage(R.string.main_wrn_accsetup);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.common_ok, this);
            builder.setNegativeButton(R.string.common_cancel, this);
            dialog = builder.create();
            break;
        default:
            dialog = null;
        }

        return dialog;
    }

    public void onClick(DialogInterface dialog, int which) {
        // In any case - we won't need it anymore
        dialog.dismiss();
        switch (which) {
        case DialogInterface.BUTTON_POSITIVE:
            Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
            intent.putExtra("authorities",
                    new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
            startActivity(intent);
            break;
        case DialogInterface.BUTTON_NEGATIVE:
            finish();
        }

    }

    @Override
    /**
     * Start an activity based on the selection
     * the user made
     */
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        Intent intent;
        intent = (Intent) parent.getAdapter().getItem(position);
        if (intent != null) {
            startActivity(intent);
        } else {
            // TODO: Implement all of this and make this text go away ;-)
            Toast toast = Toast.makeText(this, "Not yet implemented!",
                    Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    /**
     * Checks, whether or not there are any ownCloud accounts setup.
     * 
     * @return true, if there is at least one account.
     */
    private boolean accountsAreSetup() {
        AccountManager accMan = AccountManager.get(this);
        Account[] accounts = accMan
                .getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);
        return accounts.length > 0;
    }

}
