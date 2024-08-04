/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.di

import com.nextcloud.test.InjectionTestActivity
import com.nextcloud.test.TestActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Register classes that require dependency injection. This class is used by Dagger compiler only.
 */
@Module
interface BuildTypeComponentsModule {
    @ContributesAndroidInjector
    fun testActivity(): TestActivity?

    @ContributesAndroidInjector
    fun injectionTestActivity(): InjectionTestActivity?
}
