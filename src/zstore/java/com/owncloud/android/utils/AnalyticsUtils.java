/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.utils;

import android.app.Activity;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.owncloud.android.MainApp;

public class AnalyticsUtils {
    public static void setCurrentScreenName(Activity activity, String s, String s1) {
        // do nothing
    }

    public static void disableAnalytics() {
        FirebaseAnalytics.getInstance(MainApp.getAppContext()).setAnalyticsCollectionEnabled(false);
    }
}
