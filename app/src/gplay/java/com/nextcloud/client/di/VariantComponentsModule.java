/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.di;

import com.owncloud.android.authentication.ModifiedAuthenticatorActivity;
import com.owncloud.android.services.firebase.NCFirebaseMessagingService;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
abstract class VariantComponentsModule {
    @ContributesAndroidInjector
    abstract NCFirebaseMessagingService nCFirebaseMessagingService();

    @ContributesAndroidInjector
    abstract ModifiedAuthenticatorActivity modifiedAuthenticatorActivity();
}
