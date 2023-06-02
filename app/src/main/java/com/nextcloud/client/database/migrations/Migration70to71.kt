/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2023 Álvaro Brey
 *  Copyright (C) 2023 Nextcloud GmbH
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

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from version 70 to 71.
 *
 * resets capabilities to show groupfolder
 */
@Suppress("MagicNumber")
class Migration70to71 : Migration(70, 71) {
    override fun migrate(database: SupportSQLiteDatabase) {
        DatabaseMigrationUtil.resetCapabilities(database)
    }
}
