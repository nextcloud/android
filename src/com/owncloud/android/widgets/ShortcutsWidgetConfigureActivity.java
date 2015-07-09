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

import android.app.Activity;
import android.app.Fragment;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.ui.dialog.AccountsDialogFragment;


/**
 * The configuration screen for the {@link ShortcutsWidget ShortcutsWidget} AppWidget.
 */
public class ShortcutsWidgetConfigureActivity extends Activity
        implements AccountsDialogFragment.AccountDialogFragmentListener{

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private static final String PREFS_NAME = "com.owncloud.android.widgets.ShortcutsWidget";
    private static final String PREF_PREFIX_KEY = "appwidget_";

    public ShortcutsWidgetConfigureActivity() {
        super();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED);

          // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);

        }

        // If this activity was started with an intent without an app widget ID,
        // finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        // Show dialog with the accounts
        AccountsDialogFragment accountsDialogFragment = new AccountsDialogFragment().getInstance();
        accountsDialogFragment.setOnItemClickListener(this);
        Fragment fr = getFragmentManager().findFragmentByTag(AccountsDialogFragment.TAG);
        if (fr == null) {
            accountsDialogFragment.show(getFragmentManager(), AccountsDialogFragment.TAG);
        }

    }


    // Write the prefix to the SharedPreferences object for this widget
    static void saveAccountPref(Context context, int appWidgetId, String text) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.putString(PREF_PREFIX_KEY + appWidgetId, text);
        prefs.commit();
    }

    // Read the prefix from the SharedPreferences object for this widget.
    // If there is no preference saved, get the default from a resource
    static String loadAccountPref(Context context, int appWidgetId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, 0);
        String userValue = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null);
        if (userValue != null) {
            return userValue;
        } else {
            return context.getString(R.string.username);
        }
    }

    static void deleteAccountPref(Context context, int appWidgetId) {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, 0).edit();
        prefs.remove(PREF_PREFIX_KEY + appWidgetId);
        prefs.commit();
    }

    private void configureResult(){

        Context context = ShortcutsWidgetConfigureActivity.this;

        // It is the responsibility of the configuration activity to update the app widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ShortcutsWidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }

    @Override
    public void onAccountSelected(String accountName) {
        Context context = ShortcutsWidgetConfigureActivity.this;

        saveAccountPref(context, mAppWidgetId, accountName);
        configureResult();
    }

    @Override
    public void onNoAccount() {
        Toast.makeText(getApplicationContext(),
                String.format(
                    getString(R.string.uploader_wrn_no_account_text), getString(R.string.app_name)),
                Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onAccountSelectionCancel() {
        finish();
    }
}

