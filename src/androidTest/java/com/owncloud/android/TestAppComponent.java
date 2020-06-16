/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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

package com.owncloud.android;

import android.app.Application;

import com.nextcloud.client.appinfo.AppInfoModule;
import com.nextcloud.client.device.DeviceModule;
import com.nextcloud.client.di.AppModule;
import com.nextcloud.client.di.ViewModelModule;
import com.nextcloud.client.jobs.JobsModule;
import com.nextcloud.client.network.NetworkModule;
import com.nextcloud.client.onboarding.OnboardingModule;
import com.nextcloud.client.preferences.PreferencesModule;

import javax.inject.Singleton;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.support.AndroidSupportInjectionModule;

@Singleton
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
    ActivitiesModuleMock.class
})

public interface TestAppComponent {

    void inject(TestApp app);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);

        TestAppComponent build();
    }
}
