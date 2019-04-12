/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.di;

import android.app.Application;
import android.content.Context;

import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;

import dagger.Module;
import dagger.Provides;

@Module(includes = {ComponentsModule.class})
class AppModule {

    @Provides
    Context context(Application application) {
        return application.getApplicationContext();
    }

    @Provides
    AppPreferences preferences(Application application) {
        return AppPreferencesImpl.fromContext(application);
    }
}
