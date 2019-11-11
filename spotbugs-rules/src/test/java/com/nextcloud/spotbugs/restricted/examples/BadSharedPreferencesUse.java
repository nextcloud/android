/*
 * Nextcloud Android SpotBugs Plugin
 *
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2019 Chris Narkiewicz
 * Copyright (C) 2019 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.spotbugs.restricted.examples;

import android.content.SharedPreferences;
import android.content.SharedPreferencesImpl;

public class BadSharedPreferencesUse {

    private static final SharedPreferences sPrefs = new SharedPreferencesImpl();
    private SharedPreferences mPrefs = new SharedPreferencesImpl();

    void staticFieldCall() {
        SharedPreferences.Editor editor = sPrefs.edit();
        editor.commit();
    }

    void memberFieldCall() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.commit();
    }

    void localVariableCall() {
        SharedPreferences prefs = new SharedPreferencesImpl();
        SharedPreferences.Editor editor = prefs.edit();
        editor.commit();
    }
}
