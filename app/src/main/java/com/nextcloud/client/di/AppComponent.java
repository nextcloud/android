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

import com.nextcloud.appReview.InAppReviewModule;
import com.nextcloud.client.appinfo.AppInfoModule;
import com.nextcloud.client.database.DatabaseModule;
import com.nextcloud.client.device.DeviceModule;
import com.nextcloud.client.integrations.IntegrationsModule;
import com.nextcloud.client.jobs.JobsModule;
import com.nextcloud.client.network.NetworkModule;
import com.nextcloud.client.onboarding.OnboardingModule;
import com.nextcloud.client.preferences.PreferencesModule;
import com.owncloud.android.MainApp;
import com.owncloud.android.media.MediaControlView;
import com.owncloud.android.ui.ThemeableSwitchPreference;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;
import com.owncloud.android.utils.FilesUploadHelper;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

@Component(modules = {
    AndroidSupportInjectionModule.class,
    AppModule.class,
    PreferencesModule.class,
    AppInfoModule.class,
    NetworkModule.class,
    DeviceModule.class,
    OnboardingModule.class,
    ViewModelModule.class,
    JobsModule.class,
    IntegrationsModule.class,
    InAppReviewModule.class,
    ThemeModule.class,
    DatabaseModule.class,
    DispatcherModule.class,
    VariantModule.class
})
@Singleton
public interface AppComponent {

    void inject(MainApp app);

    void inject(MediaControlView mediaControlView);

    void inject(ThemeableSwitchPreference switchPreference);

    void inject(FilesUploadHelper filesUploadHelper);

    void inject(ProgressIndicator progressIndicator);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);

        AppComponent build();
    }
}
