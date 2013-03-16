/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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

import java.io.File;

import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo.State;
import android.preference.PreferenceManager;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.db.DbHandler;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.utils.FileStorageUtils;

public class InstantUploadBroadcastReceiver extends BroadcastReceiver {

    private static String TAG = "PhotoTakenBroadcastReceiver";
    private static final String[] CONTENT_PROJECTION = { Media.DATA, Media.DISPLAY_NAME, Media.MIME_TYPE, Media.SIZE };
    private static String NEW_PHOTO_ACTION = "com.android.camera.NEW_PICTURE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received: " + intent.getAction());
        if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
            handleConnectivityAction(context, intent);
        } else if (intent.getAction().equals(NEW_PHOTO_ACTION)) {
            handleNewPhotoAction(context, intent);
        } else if (intent.getAction().equals(FileUploader.UPLOAD_FINISH_MESSAGE)) {
            handleUploadFinished(context, intent);
        } else {
            Log.e(TAG, "Incorrect intent sent: " + intent.getAction());
        }
    }

    private void handleUploadFinished(Context context, Intent intent) {
        // remove successfull uploading, ignore rest for reupload on reconnect
        if (intent.getBooleanExtra(FileUploader.EXTRA_UPLOAD_RESULT, false)) {
            DbHandler db = new DbHandler(context);
            String localPath = intent.getStringExtra(FileUploader.EXTRA_OLD_FILE_PATH);
            if (!db.removeIUPendingFile(localPath)) {
                Log.w(TAG, "Tried to remove non existing instant upload file " + localPath);
            }
            db.close();
        }
    }

    private void handleNewPhotoAction(Context context, Intent intent) {
        if (!instantUploadEnabled(context)) {
            Log.d(TAG, "Instant upload disabled, abording uploading");
            return;
        }

        Account account = AccountUtils.getCurrentOwnCloudAccount(context);
        if (account == null) {
            Log.w(TAG, "No owncloud account found for instant upload, aborting");
            return;
        }

        Cursor c = context.getContentResolver().query(intent.getData(), CONTENT_PROJECTION, null, null, null);

        if (!c.moveToFirst()) {
            Log.e(TAG, "Couldn't resolve given uri: " + intent.getDataString());
            return;
        }

        String file_path = c.getString(c.getColumnIndex(Media.DATA));
        String file_name = c.getString(c.getColumnIndex(Media.DISPLAY_NAME));
        String mime_type = c.getString(c.getColumnIndex(Media.MIME_TYPE));

        c.close();
        Log.e(TAG, file_path + "");

        // same always temporally the picture to upload
        DbHandler db = new DbHandler(context);
        db.putFileForLater(file_path, account.name, null);
        db.close();

        if (!isOnline(context) || (instantUploadViaWiFiOnly(context) && !isConnectedViaWiFi(context))) {
            return;
        }

        // register for upload finishe message
        // there is a litte problem with android API, we can register for
        // particular
        // intent in registerReceiver but we cannot unregister from precise
        // intent
        // we can unregister from entire listenings but thats suck a bit.
        // On the other hand this might be only for dynamicly registered
        // broadcast receivers, needs investigation.
        IntentFilter filter = new IntentFilter(FileUploader.UPLOAD_FINISH_MESSAGE);
        context.getApplicationContext().registerReceiver(this, filter);

        Intent i = new Intent(context, FileUploader.class);
        i.putExtra(FileUploader.KEY_ACCOUNT, account);
        i.putExtra(FileUploader.KEY_LOCAL_FILE, file_path);
        i.putExtra(FileUploader.KEY_REMOTE_FILE, FileStorageUtils.getInstantUploadFilePath(file_name));
        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
        i.putExtra(FileUploader.KEY_MIME_TYPE, mime_type);
        i.putExtra(FileUploader.KEY_INSTANT_UPLOAD, true);
        context.startService(i);

    }

    private void handleConnectivityAction(Context context, Intent intent) {
        if (!instantUploadEnabled(context)) {
            Log.d(TAG, "Instant upload disabled, abording uploading");
            return;
        }

        if (!intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
                && isOnline(context)
                && (!instantUploadViaWiFiOnly(context) || (instantUploadViaWiFiOnly(context) == isConnectedViaWiFi(context) == true))) {
            DbHandler db = new DbHandler(context);
            Cursor c = db.getAwaitingFiles();
            if (c.moveToFirst()) {
                IntentFilter filter = new IntentFilter(FileUploader.UPLOAD_FINISH_MESSAGE);
                context.getApplicationContext().registerReceiver(this, filter);
                do {
                    String account_name = c.getString(c.getColumnIndex("account"));
                    String file_path = c.getString(c.getColumnIndex("path"));
                    File f = new File(file_path);
                    if (f.exists()) {
                        Account account = new Account(account_name, AccountAuthenticator.ACCOUNT_TYPE);

                        String mimeType = null;
                        try {
                            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                    f.getName().substring(f.getName().lastIndexOf('.') + 1));

                        } catch (Throwable e) {
                            Log.e(TAG, "Trying to find out MIME type of a file without extension: " + f.getName());
                        }
                        if (mimeType == null)
                            mimeType = "application/octet-stream";

                        Intent i = new Intent(context, FileUploader.class);
                        i.putExtra(FileUploader.KEY_ACCOUNT, account);
                        i.putExtra(FileUploader.KEY_LOCAL_FILE, file_path);
                        i.putExtra(FileUploader.KEY_REMOTE_FILE, FileStorageUtils.getInstantUploadFilePath(f.getName()));
                        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
                        i.putExtra(FileUploader.KEY_INSTANT_UPLOAD, true);
                        context.startService(i);

                    } else {
                        Log.w(TAG, "Instant upload file " + f.getAbsolutePath() + " dont exist anymore");
                    }
                } while (c.moveToNext());
            }
            c.close();
            db.close();
        }

    }

    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    public static boolean isConnectedViaWiFi(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm != null && cm.getActiveNetworkInfo() != null
                && cm.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_WIFI
                && cm.getActiveNetworkInfo().getState() == State.CONNECTED;
    }

    public static boolean instantUploadEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_uploading", false);
    }

    public static boolean instantUploadViaWiFiOnly(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("instant_upload_on_wifi", false);
    }
}
