/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.database

import android.content.Context
import com.nextcloud.client.core.Clock
import com.nextcloud.client.database.dao.ArbitraryDataDao
import com.nextcloud.client.database.dao.FileDao
import com.nextcloud.client.database.dao.OfflineOperationDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DatabaseModule {

    @Provides
    @Singleton
    fun database(context: Context, clock: Clock): NextcloudDatabase {
        return NextcloudDatabase.getInstance(context, clock)
    }

    @Provides
    fun arbitraryDataDao(nextcloudDatabase: NextcloudDatabase): ArbitraryDataDao {
        return nextcloudDatabase.arbitraryDataDao()
    }

    @Provides
    fun fileDao(nextcloudDatabase: NextcloudDatabase): FileDao {
        return nextcloudDatabase.fileDao()
    }

    @Provides
    fun offlineOperationsDao(nextcloudDatabase: NextcloudDatabase): OfflineOperationDao {
        return nextcloudDatabase.offlineOperationDao()
    }
}
