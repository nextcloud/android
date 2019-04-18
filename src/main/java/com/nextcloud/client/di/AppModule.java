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

import android.accounts.AccountManager;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserAccountManagerImpl;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.ui.activities.data.activities.ActivitiesRepository;
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApi;
import com.owncloud.android.ui.activities.data.activities.ActivitiesServiceApiImpl;
import com.owncloud.android.ui.activities.data.activities.RemoteActivitiesRepository;
import com.owncloud.android.ui.activities.data.files.FilesRepository;
import com.owncloud.android.ui.activities.data.files.FilesServiceApiImpl;
import com.owncloud.android.ui.activities.data.files.RemoteFilesRepository;

import dagger.Module;
import dagger.Provides;

@Module(includes = {ComponentsModule.class, VariantComponentsModule.class})
class AppModule {

    @Provides
    AccountManager accountManager(Application application) {
        return (AccountManager)application.getSystemService(Context.ACCOUNT_SERVICE);
    }

    @Provides
    Context context(Application application) {
        return application;
    }

    @Provides
    Resources resources(Application application) {
        return application.getResources();
    }

    @Provides
    AppPreferences preferences(Application application) {
        return AppPreferencesImpl.fromContext(application);
    }

    @Provides
    UserAccountManager userAccountManager(
        Context context,
        AccountManager accountManager
    ) {
        return new UserAccountManagerImpl(context, accountManager);
    }

    @Provides
    ArbitraryDataProvider arbitraryDataProvider(Context context) {
        final ContentResolver resolver = context.getContentResolver();
        return new ArbitraryDataProvider(resolver);
    }

    @Provides
    ActivitiesServiceApi activitiesServiceApi(UserAccountManager accountManager) {
        return new ActivitiesServiceApiImpl(accountManager);
    }

    @Provides
    ActivitiesRepository activitiesRepository(ActivitiesServiceApi api) {
        return new RemoteActivitiesRepository(api);
    }

    @Provides
    FilesRepository filesRepository(UserAccountManager accountManager) {
        return new RemoteFilesRepository(new FilesServiceApiImpl(accountManager));
    }

    @Provides
    UploadsStorageManager uploadsStorageManager(Context context,
                                                CurrentAccountProvider currentAccountProvider) {
        return new UploadsStorageManager(currentAccountProvider, context.getContentResolver());
    }

    @Provides CurrentAccountProvider currentAccountProvider(UserAccountManager accountManager) {
        return accountManager;
    }
}
