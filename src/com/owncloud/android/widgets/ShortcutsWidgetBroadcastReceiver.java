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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.Toast;

public class ShortcutsWidgetBroadcastReceiver extends BroadcastReceiver {

	private static int clickCount = 0;

	@Override
	public void onReceive(Context context, Intent intent) {
		if(intent.getAction().equals(ShortcutsWidget.ACTION_APPICON_CLICK)){
            Toast.makeText(context, ShortcutsWidget.ACTION_APPICON_CLICK, Toast.LENGTH_SHORT).show();
		}else if (intent.getAction().equals(ShortcutsWidget.ACTION_UPLOAD_CLICK)) {
            Toast.makeText(context, ShortcutsWidget.ACTION_UPLOAD_CLICK, Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(ShortcutsWidget.ACTION_NEW_CLICK)) {
            Toast.makeText(context, ShortcutsWidget.ACTION_NEW_CLICK, Toast.LENGTH_SHORT).show();
        } else if (intent.getAction().equals(ShortcutsWidget.ACTION_REFRESH_CLICK)) {
            Toast.makeText(context, ShortcutsWidget.ACTION_REFRESH_CLICK, Toast.LENGTH_SHORT).show();
        }
	}

}
