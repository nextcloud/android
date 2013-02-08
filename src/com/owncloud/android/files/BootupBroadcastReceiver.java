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

package com.owncloud.android.files;

import com.owncloud.android.files.services.FileObserverService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootupBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = "BootupBroadcastReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Log.wtf(TAG, "Incorrect action sent " + intent.getAction());
            return;
        }
        Log.d(TAG, "Starting file observer service...");
        Intent i = new Intent(context, FileObserverService.class);
        i.putExtra(FileObserverService.KEY_FILE_CMD,
                   FileObserverService.CMD_INIT_OBSERVED_LIST);
        context.startService(i);
        Log.d(TAG, "DONE");
    }

}
