/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.client.di;

import android.app.Application;

import com.nextcloud.appReview.InAppReviewModule;
import com.nextcloud.client.appinfo.AppInfoModule;
import com.nextcloud.client.database.DatabaseModule;
import com.nextcloud.client.device.DeviceModule;
import com.nextcloud.client.integrations.IntegrationsModule;
import com.nextcloud.client.jobs.JobsModule;
import com.nextcloud.client.jobs.download.FileDownloadHelper;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.media.BackgroundPlayerService;
import com.nextcloud.client.media.CustomExoPlayer;
import com.nextcloud.client.network.NetworkModule;
import com.nextcloud.client.onboarding.OnboardingModule;
import com.nextcloud.client.preferences.PreferencesModule;
import com.owncloud.android.MainApp;
import com.owncloud.android.media.MediaControlView;
import com.owncloud.android.ui.ThemeableSwitchPreference;
import com.owncloud.android.ui.whatsnew.ProgressIndicator;

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
    VariantModule.class,
})
@Singleton
public interface AppComponent {

    void inject(MainApp app);

    void inject(MediaControlView mediaControlView);
    void inject(CustomExoPlayer customExoPlayer);
    void inject(BackgroundPlayerService backgroundPlayerService);

    void inject(ThemeableSwitchPreference switchPreference);

    void inject(FileUploadHelper fileUploadHelper);

    void inject(FileDownloadHelper fileDownloadHelper);

    void inject(ProgressIndicator progressIndicator);

    @Component.Builder
    interface Builder {
        @BindsInstance
        Builder application(Application application);

        AppComponent build();
    }
}
