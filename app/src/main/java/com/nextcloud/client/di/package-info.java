/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

/**
 * This package contains application Dependency Injection code, based on Dagger 2.
 * <p>
 * To enable dependency injection for a component, such as {@link android.app.Activity},
 * {@link androidx.fragment.app.Fragment} or {@link android.app.Service}, the component must be
 * first registered in {@link com.nextcloud.client.di.ComponentsModule} class.
 * <p>
 * {@link com.nextcloud.client.di.ComponentsModule} will be used by Dagger compiler to
 * create an injector for a given class.
 *
 * @see com.nextcloud.client.di.InjectorNotFoundException
 * @see dagger.android.AndroidInjection
 * @see dagger.android.support.AndroidSupportInjection
 */
package com.nextcloud.client.di;
