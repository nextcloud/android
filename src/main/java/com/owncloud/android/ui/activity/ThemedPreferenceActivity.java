/*
 * Nextcloud Android client application
 *
 * @author Daniel Bailey
 * Copyright (C) 2019 Daniel Bailey
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.DarkMode;

import javax.inject.Inject;

import androidx.annotation.Nullable;

public class ThemedPreferenceActivity extends PreferenceActivity {

    /**
     * Tracks whether the activity should be recreate()'d after a theme change
     */
    private boolean themeChangePending;
    private boolean paused;

    @Inject AppPreferences preferences;

    private AppPreferences.Listener onThemeChangedListener = new AppPreferences.Listener() {
        @Override
        public void onDarkThemeModeChanged(DarkMode mode) {
            preferences.setDarkThemeMode(mode);

            if (paused) {
                themeChangePending = true;
                return;
            }
            recreate();
        }
    };

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        preferences.addListener(onThemeChangedListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        preferences.removeListener(onThemeChangedListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;

        if (themeChangePending) {
            recreate();
        }
    }
}
