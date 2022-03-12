/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2018 Mario Danic
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

import android.content.Intent;

import com.google.android.gms.security.ProviderInstaller;
import com.owncloud.android.MainApp;

public class SecurityUtils implements ProviderInstaller.ProviderInstallListener {
    public SecurityUtils() {
        ProviderInstaller.installIfNeededAsync(MainApp.getAppContext(), this);
    }

    @Override
    public void onProviderInstalled() {
        // Does nothing
    }

    @Override
    public void onProviderInstallFailed(int i, Intent intent) {
        // Does nothing
    }
}
