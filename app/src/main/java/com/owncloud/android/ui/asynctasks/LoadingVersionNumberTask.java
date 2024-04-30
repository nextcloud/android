/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Class for loading the version number
 */
public class LoadingVersionNumberTask extends AsyncTask<String, Void, Integer> {
    private static final String TAG = LoadingVersionNumberTask.class.getSimpleName();

    private VersionDevInterface callback;
    
    public LoadingVersionNumberTask(VersionDevInterface callback) {
        this.callback = callback;
    }
    
    protected Integer doInBackground(String... args) {
        try {
            URL url = new URL(args[0]);
            final Charset charset = Charset.defaultCharset();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), charset))) {
                return Integer.parseInt(in.readLine());

            } catch (IOException e) {
                Log_OC.e(TAG, "Error loading version number", e);
            }
        } catch (MalformedURLException e) {
            Log_OC.e(TAG, "Malformed URL", e);
        }
        return -1;
    }

    @Override
    protected void onPostExecute(Integer latestVersion) {
        callback.returnVersion(latestVersion);
    }

    public interface VersionDevInterface {
        void returnVersion(Integer latestVersion);
    }
}
