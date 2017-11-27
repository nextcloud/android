/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Class for loading the version number
 */
public class LoadingVersionNumberTask extends AsyncTask<String, Void, Integer> {
    private static final String TAG = LoadingVersionNumberTask.class.getSimpleName();

    protected Integer doInBackground(String... args) {
        try {
            URL url = new URL(args[0]);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

            Integer latestVersion = Integer.parseInt(in.readLine());
            in.close();

            return latestVersion;

        } catch (IOException e) {
            Log_OC.e(TAG, "Error loading version number", e);
        }
        return -1;
    }
}