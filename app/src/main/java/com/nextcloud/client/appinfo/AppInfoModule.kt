/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.appinfo

import dagger.Module
import dagger.Provides

@Module
class AppInfoModule {
    @Provides
    fun appInfo(): AppInfo {
        return AppInfoImpl()
    }
}
