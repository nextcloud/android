/**
 *   ownCloud Android client application
 *
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

package com.owncloud.android.widgets;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncRequest;
import android.os.Bundle;
import android.widget.Toast;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;

public class ShortcutsWidgetBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // get the account
        Account account = null;
        String accountName = intent.getStringExtra(ShortcutsWidget.EXTRA_ACCOUNT_NAME);
        AccountManager accountManager = AccountManager.get(context);
        Account[] accounts = accountManager.getAccountsByType(MainApp.getAccountType());
        for(Account a: accounts){
            if (a.name.equals(accountName)){
                account = a;
            }
        }

        Intent appIntent = null;
        if(intent.getAction().equals(ShortcutsWidget.ACTION_APPICON_CLICK)){
            // launch the app
            appIntent = new Intent(context, FileDisplayActivity.class);

        } else if (intent.getAction().equals(ShortcutsWidget.ACTION_UPLOAD_CLICK)) {
            // Open uploader
            appIntent = new Intent(context, FileDisplayActivity.class);
            appIntent.putExtra(FileDisplayActivity.EXTRA_UPLOAD_FROM_WIDGET, true);

        } else if (intent.getAction().equals(ShortcutsWidget.ACTION_NEW_CLICK)) {
            // Create new folder
            appIntent = new Intent(context, FileDisplayActivity.class);
            appIntent.putExtra(FileDisplayActivity.EXTRA_NEW_FROM_WIDGET, true);

        } else if (intent.getAction().equals(ShortcutsWidget.ACTION_REFRESH_CLICK)) {
            String message = context.getString(R.string.appwidget_start_sync, accountName);
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

            // Start synchronization
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.KITKAT) {
                ContentResolver.cancelSync(null, MainApp.getAuthority());
                // cancel the current synchronizations of any ownCloud account
                Bundle bundle = new Bundle();
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
                bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
                ContentResolver.requestSync(
                        account,
                        MainApp.getAuthority(), bundle);
            } else {
                SyncRequest.Builder builder = new SyncRequest.Builder();
                builder.setSyncAdapter(account, MainApp.getAuthority());
                builder.setExpedited(true);
                builder.setManual(true);
                builder.syncOnce();

                // Fix bug in Android Lollipop when you click on refresh the whole account
                Bundle extras = new Bundle();
                builder.setExtras(extras);

                SyncRequest request = builder.build();
                ContentResolver.requestSync(request);
            }
        }

        if (appIntent != null) {
            appIntent.putExtra(FileActivity.EXTRA_ACCOUNT, account);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appIntent);
        }
    }

}
