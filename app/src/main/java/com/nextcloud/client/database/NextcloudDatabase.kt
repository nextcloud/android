/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.database

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nextcloud.client.core.Clock
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.client.database.dao.ArbitraryDataDao
import com.nextcloud.client.database.dao.FileDao
import com.nextcloud.client.database.dao.OfflineOperationDao
import com.nextcloud.client.database.entity.ArbitraryDataEntity
import com.nextcloud.client.database.entity.CapabilityEntity
import com.nextcloud.client.database.entity.ExternalLinkEntity
import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.database.entity.FilesystemEntity
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.client.database.entity.ShareEntity
import com.nextcloud.client.database.entity.SyncedFolderEntity
import com.nextcloud.client.database.entity.UploadEntity
import com.nextcloud.client.database.entity.VirtualEntity
import com.nextcloud.client.database.migrations.DatabaseMigrationUtil
import com.nextcloud.client.database.migrations.Migration67to68
import com.nextcloud.client.database.migrations.RoomMigration
import com.nextcloud.client.database.migrations.addLegacyMigrations
import com.owncloud.android.db.ProviderMeta

@Database(
    entities = [
        ArbitraryDataEntity::class,
        CapabilityEntity::class,
        ExternalLinkEntity::class,
        FileEntity::class,
        FilesystemEntity::class,
        ShareEntity::class,
        SyncedFolderEntity::class,
        UploadEntity::class,
        VirtualEntity::class,
        OfflineOperationEntity::class
    ],
    version = ProviderMeta.DB_VERSION,
    autoMigrations = [
        AutoMigration(from = 65, to = 66),
        AutoMigration(from = 66, to = 67),
        AutoMigration(from = 68, to = 69),
        AutoMigration(from = 69, to = 70),
        AutoMigration(from = 70, to = 71, spec = DatabaseMigrationUtil.ResetCapabilitiesPostMigration::class),
        AutoMigration(from = 71, to = 72),
        AutoMigration(from = 72, to = 73),
        AutoMigration(from = 73, to = 74, spec = DatabaseMigrationUtil.ResetCapabilitiesPostMigration::class),
        AutoMigration(from = 74, to = 75),
        AutoMigration(from = 75, to = 76),
        AutoMigration(from = 76, to = 77),
        AutoMigration(from = 77, to = 78),
        AutoMigration(from = 78, to = 79, spec = DatabaseMigrationUtil.ResetCapabilitiesPostMigration::class),
        AutoMigration(from = 79, to = 80),
        AutoMigration(from = 80, to = 81),
        AutoMigration(from = 81, to = 82),
        AutoMigration(from = 82, to = 83),
        AutoMigration(from = 83, to = 84)
    ],
    exportSchema = true
)
@Suppress("Detekt.UnnecessaryAbstractClass") // needed by Room
abstract class NextcloudDatabase : RoomDatabase() {

    abstract fun arbitraryDataDao(): ArbitraryDataDao
    abstract fun fileDao(): FileDao
    abstract fun offlineOperationDao(): OfflineOperationDao

    companion object {
        const val FIRST_ROOM_DB_VERSION = 65
        private var instance: NextcloudDatabase? = null

        @JvmStatic
        @Suppress("DeprecatedCallableAddReplaceWith")
        @Deprecated("Here for legacy purposes, inject this class or use getInstance(context, clock) instead")
        fun getInstance(context: Context): NextcloudDatabase {
            return getInstance(context, ClockImpl())
        }

        @JvmStatic
        fun getInstance(context: Context, clock: Clock): NextcloudDatabase {
            if (instance == null) {
                instance = Room
                    .databaseBuilder(context, NextcloudDatabase::class.java, ProviderMeta.DB_NAME)
                    .allowMainThreadQueries()
                    .addLegacyMigrations(clock, context)
                    .addMigrations(RoomMigration())
                    .addMigrations(Migration67to68())
                    .fallbackToDestructiveMigration()
                    .build()
            }
            return instance!!
        }
    }
}
