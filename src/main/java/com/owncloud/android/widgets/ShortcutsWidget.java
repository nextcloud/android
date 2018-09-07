/*
 * ownCloud Android client application
 *
 * @author masensio
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.widgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.owncloud.android.R;
import com.owncloud.android.db.PreferenceManager;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in
 * {@link ShortcutsWidgetConfigureActivity ShortcutsWidgetConfigureActivity}
 */
public class ShortcutsWidget extends AppWidgetProvider {

    public static final String ACTION_APPICON_CLICK = "com.owncloud.android.action.appClick";
    public static final String ACTION_UPLOAD_CLICK = "com.owncloud.android.action.uploadClick";
    public static final String ACTION_NEW_CLICK = "com.owncloud.android.action.newClick";
    public static final String ACTION_REFRESH_CLICK = "com.owncloud.android.action.refreshClick";

    public static final String EXTRA_ACCOUNT_NAME = "account_name";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            updateAppWidget(context, appWidgetManager, appWidgetIds[i]);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        final int N = appWidgetIds.length;
        for (int i = 0; i < N; i++) {
            PreferenceManager.deleteAccountPref(context, appWidgetIds[i]);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    public static void updateAppWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId) {

        CharSequence widgetAccount = PreferenceManager.loadAccountPref(context, appWidgetId);
        // Construct the RemoteViews object
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                R.layout.shortcuts_widget);
        remoteViews.setTextViewText(R.id.appwidget_title, context.getString(R.string.app_name));
        remoteViews.setTextViewText(R.id.widget_username, widgetAccount);

        // Add events for the buttons
        remoteViews.setOnClickPendingIntent(R.id.widget_accountLayout,
                getPendingSelfIntent(context, ACTION_APPICON_CLICK, appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.widget_upload,
                getPendingSelfIntent(context, ACTION_UPLOAD_CLICK, appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.widget_create,
                getPendingSelfIntent(context, ACTION_NEW_CLICK, appWidgetId));
        remoteViews.setOnClickPendingIntent(R.id.widget_refresh,
                getPendingSelfIntent(context, ACTION_REFRESH_CLICK, appWidgetId));

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    static protected PendingIntent getPendingSelfIntent(Context context, String action,
                                                        int appWidgetId) {
        Intent intent = new Intent();
        intent.setAction(action);
        String accountName = PreferenceManager.loadAccountPref(context, appWidgetId);
        intent.putExtra(EXTRA_ACCOUNT_NAME, accountName);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}



