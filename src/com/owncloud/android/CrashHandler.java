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

package com.owncloud.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedList;
import java.util.List;

import com.owncloud.android.authenticator.AccountAuthenticator;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.util.Log;

public class CrashHandler implements UncaughtExceptionHandler {

    public static final String KEY_CRASH_FILENAME = "KEY_CRASH_FILENAME";
    
    private Context mContext;
    private static final String TAG = "CrashHandler";
    private static final String crash_filename_template = "crash";
    private static List<String> TAGS;
    private UncaughtExceptionHandler defaultUEH;
    
    // TODO: create base activity which will register for crashlog tag automaticly
    static {
        TAGS = new LinkedList<String>();
        TAGS.add("AccountAuthenticator");
        TAGS.add("AccountAuthenticator");
        TAGS.add("ConnectionCheckerRunnable");
        TAGS.add("EasySSLSocketFactory");
        TAGS.add("FileDataStorageManager");
        TAGS.add("PhotoTakenBroadcastReceiver");
        TAGS.add("InstantUploadService");
        TAGS.add("FileDownloader");
        TAGS.add("FileUploader");
        TAGS.add("LocationUpdateService");
        TAGS.add("FileSyncAdapter");
        TAGS.add("AuthActivity");
        TAGS.add("OwnCloudPreferences");
        TAGS.add("FileDetailFragment");
        TAGS.add("FileListFragment");
        TAGS.add("ownCloudUploader");
        TAGS.add("WebdavClient");
    }
    
    public CrashHandler(Context context) {
        mContext = context;
        defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
    }
    
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        final Writer writer = new StringWriter();
        final PrintWriter printwriter = new PrintWriter(writer);
        ex.printStackTrace(printwriter);
        final String startrace = writer.toString();
        printwriter.close();
        File ocdir = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), "owncloud");
        ocdir.mkdirs();

        String crash_filename = crash_filename_template + System.currentTimeMillis() + ".txt";
        File crashfile = new File(ocdir, crash_filename);
        try {
            PackageInfo pi = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            String header = String.format("Model: %s, SDK: %d, Current net: %s AppVersion: %s\n\n",
                                          android.os.Build.MODEL,
                                          android.os.Build.VERSION.SDK_INT,
                                          cm.getActiveNetworkInfo() != null ? cm.getActiveNetworkInfo().getTypeName() : "NONE",
                                          pi.versionName);
            Account account = AccountUtils.getCurrentOwnCloudAccount(mContext);
            AccountManager am = AccountManager.get(mContext);
            String header2 = String.format("Account: %s, OCUrl: %s, OCVersion: %s\n\n",
                                           account.name,
                                           am.getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL),
                                           am.getUserData(account, AccountAuthenticator.KEY_OC_VERSION));
            
            crashfile.createNewFile();
            FileWriter fw = new FileWriter(crashfile);
            fw.write(header);
            fw.write(header2);
            fw.write(startrace);
            fw.write("\n\n");
            
            String logcat = "logcat -d *:S ";
            
            for (String s : TAGS)
                logcat += s + ":V ";
            
            Process process = Runtime.getRuntime().exec(logcat);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String logline;
            while ((logline = br.readLine()) != null)
                fw.write(logline+"\n");
            
            br.close();
            fw.close();
            
            Intent dataintent = new Intent(mContext, CrashlogSendActivity.class);
            dataintent.putExtra(KEY_CRASH_FILENAME, crashfile.getAbsolutePath());
            PendingIntent intent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                intent = PendingIntent.getActivity(mContext.getApplicationContext(), 0, dataintent, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            } else {
                intent = PendingIntent.getActivity(mContext.getApplicationContext(), 0, dataintent, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            AlarmManager mngr = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
            if (mngr == null) {
                Log.e(TAG, "Couldn't retrieve alarm manager!");
                defaultUEH.uncaughtException(thread, ex);
                return;
            }
            mngr.set(AlarmManager.RTC, System.currentTimeMillis(), intent);
            System.exit(2);
        } catch (Exception e1) {
            Log.e(TAG, "Crash handler failed!");
            Log.e(TAG, e1.toString());
            defaultUEH.uncaughtException(thread, ex);
            return;
        }
    }
}
