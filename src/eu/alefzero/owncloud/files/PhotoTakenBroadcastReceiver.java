/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
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

package eu.alefzero.owncloud.files;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.files.services.InstantUploadService;
import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images.Media;
import android.util.Log;

public class PhotoTakenBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = "PhotoTakenBroadcastReceiver";
    private static final String[] CONTENT_PROJECTION = { Media.DATA, Media.DISPLAY_NAME, Media.MIME_TYPE, Media.SIZE };
    
    private static String NEW_PHOTO_ACTION = "com.android.camera.NEW_PICTURE";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_uploading", false)) {
            Log.d(TAG, "Instant upload disabled, abording uploading");
            return;
        }
        if (!intent.getAction().equals(NEW_PHOTO_ACTION)) {
            Log.e(TAG, "Incorrect intent sent: " + intent.getAction());
            return;
        }
        Account account = AccountUtils.getCurrentOwnCloudAccount(context);
        if (account == null) {
            Log.w(TAG, "No owncloud account found for instant upload, abording");
            return;
        }

        Cursor c = context.getContentResolver().query(intent.getData(), CONTENT_PROJECTION, null, null, null);
        
        if (!c.moveToFirst()) {
            Log.e(TAG, "Couldn't resolve given uri!");
            return;
        }
        
        String file_path = c.getString(c.getColumnIndex(Media.DATA));
        String file_name = c.getString(c.getColumnIndex(Media.DISPLAY_NAME));
        String mime_type = c.getString(c.getColumnIndex(Media.MIME_TYPE));
        long file_size = c.getLong(c.getColumnIndex(Media.SIZE));

        c.close();
        
        Intent upload_intent = new Intent(context, InstantUploadService.class);
        upload_intent.putExtra(InstantUploadService.KEY_ACCOUNT, account);
        upload_intent.putExtra(InstantUploadService.KEY_FILE_PATH, file_path);
        upload_intent.putExtra(InstantUploadService.KEY_DISPLAY_NAME, file_name);
        upload_intent.putExtra(InstantUploadService.KEY_FILE_SIZE, file_size);
        upload_intent.putExtra(InstantUploadService.KEY_MIME_TYPE, mime_type);
        
        context.startService(upload_intent);
    }

}
