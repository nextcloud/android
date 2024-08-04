/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.migrations

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData

/**
 * This component allows starting and monitoring of application state migrations.
 * Migrations are intended to upgrade any existing, persisted application state
 * after upgrade to new version, similarly to database migrations.
 */
interface MigrationsManager {

    enum class Status {
        /**
         * Application migration was not evaluated yet. This is the default
         * state just after [android.app.Application] start
         */
        UNKNOWN,

        /**
         * All migrations applied successfully.
         */
        APPLIED,

        /**
         * Migration in progress.
         */
        RUNNING,

        /**
         * Migration failed. Application is in undefined state.
         */
        FAILED
    }

    /**
     * Listenable migration progress.
     */
    val status: LiveData<Status>

    /**
     * Information about all pending and applied migrations
     */
    val info: List<MigrationInfo>

    /**
     * Starts application state migration. Migrations will be run in background thread.
     * Callers can use [status] to monitor migration progress.
     *
     * Although the migration process is run in background, status is updated
     * immediately and can be accessed immediately after start.
     *
     * @return Number of migration steps enqueued; 0 if no migrations were started.
     */
    @Throws(MigrationError::class)
    @MainThread
    fun startMigration(): Int
}
