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

package com.owncloud.android.extensions;

import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.owncloud.android.utils.OwnCloudVersion;


import android.R;
import android.app.ListActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.SimpleAdapter;

public class ExtensionsListActivity extends ListActivity {

    private static final String packages_url = "http://alefzero.eu/a/packages.php";

    private Thread mGetterThread;
    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mGetterThread = new Thread(new JsonGetter());
        mGetterThread.start();
    }

    public void done(JSONArray a) {
        LinkedList<HashMap<String, String>> ll = new LinkedList<HashMap<String, String>>();
        for (int i = 0; i < a.length(); ++i) {
            try {
                ExtensionApplicationEntry ela = new ExtensionApplicationEntry(
                        ((JSONObject) a.get(i)));
                HashMap<String, String> ss = new HashMap<String, String>();
                ss.put("NAME", ela.getName());
                ss.put("DESC", ela.getDescription());
                ll.add(ss);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        setListAdapter(new SimpleAdapter(this, ll, R.layout.simple_list_item_2,
                new String[] { "NAME", "DESC" }, new int[] {
                        android.R.id.text1, android.R.id.text2 }));

    }

    private class JsonGetter implements Runnable {

        @Override
        public void run() {
            HttpClient hc = new HttpClient();
            GetMethod gm = new GetMethod(packages_url);
            final JSONArray ar;
            try {
                hc.executeMethod(gm);
                Log.e("ASD", gm.getResponseBodyAsString() + "");
                ar = new JSONObject(gm.getResponseBodyAsString())
                        .getJSONArray("apps");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    done(ar);
                }
            });

        }

    }

    private class ExtensionApplicationEntry {
        private static final String APP_NAME = "name";
        private static final String APP_VERSION = "version";
        private static final String APP_DESC = "description";
        private static final String APP_ICON = "icon";
        private static final String APP_URL = "download";
        private static final String APP_PLAYID = "play_id";

        private String mName, mDescription, mIcon, mDownload, mPlayId;
        private OwnCloudVersion mVersion;

        public ExtensionApplicationEntry(JSONObject appentry) {
            try {
                mName = appentry.getString(APP_NAME);
                mDescription = appentry.getString(APP_DESC);
                mIcon = appentry.getString(APP_ICON);
                mDownload = appentry.getString(APP_URL);
                mPlayId = appentry.getString(APP_PLAYID);
                mVersion = new OwnCloudVersion(appentry.getString(APP_VERSION));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public String getName() {
            return mName;
        }

        public String getDescription() {
            return mDescription;
        }

        @SuppressWarnings("unused")
        public String getIcon() {
            return mIcon;
        }

        @SuppressWarnings("unused")
        public String getDownload() {
            return mDownload;
        }

        @SuppressWarnings("unused")
        public String getPlayId() {
            return mPlayId;
        }

        @SuppressWarnings("unused")
        public OwnCloudVersion getVersion() {
            return mVersion;
        }
    }
    
}
