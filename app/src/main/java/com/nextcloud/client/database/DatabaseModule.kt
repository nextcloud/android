/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.client.database

import android.content.Context
import com.nextcloud.client.core.Clock
import com.nextcloud.client.database.dao.ArbitraryDataDao
import com.nextcloud.client.database.dao.FileDao
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
}
