/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.migrations

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.nextcloud.client.appinfo.AppInfo
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.migrations.MigrationsManager.Status

internal class MigrationsManagerImpl(
    private val appInfo: AppInfo,
    private val migrationsDb: MigrationsDb,
    private val asyncRunner: AsyncRunner,
    private val migrations: Collection<Migrations.Step>
) : MigrationsManager {

    override val status: LiveData<Status> = MutableLiveData(Status.UNKNOWN)

    override val info: List<MigrationInfo> get() {
        val applied = migrationsDb.getAppliedMigrations()
        return migrations.map {
            MigrationInfo(id = it.id, description = it.description, applied = applied.contains(it.id))
        }
    }

    @Throws(MigrationError::class)
    @Suppress("ReturnCount")
    override fun startMigration(): Int {
        if (migrationsDb.isFailed) {
            (status as MutableLiveData<Status>).value = Status.FAILED
            return 0
        }
        if (migrationsDb.lastMigratedVersion >= appInfo.versionCode) {
            (status as MutableLiveData<Status>).value = Status.APPLIED
            return 0
        }
        val applied = migrationsDb.getAppliedMigrations()
        val toApply = migrations.filter { !applied.contains(it.id) }
        if (toApply.isEmpty()) {
            onMigrationSuccess()
            return 0
        }
        (status as MutableLiveData<Status>).value = Status.RUNNING
        asyncRunner.postQuickTask(
            task = { asyncApplyMigrations(toApply) },
            onResult = { onMigrationSuccess() },
            onError = { onMigrationFailed(it) }
        )
        return toApply.size
    }

    /**
     * This method calls all pending migrations which can execute long-blocking code.
     * It should be run in a background thread.
     */
    private fun asyncApplyMigrations(migrations: Collection<Migrations.Step>) {
        migrations.forEach {
            @Suppress("TooGenericExceptionCaught") // migration code is free to throw anything
            try {
                it.run.invoke(it)
                migrationsDb.addAppliedMigration(it.id)
            } catch (t: Throwable) {
                if (it.mandatory) {
                    throw MigrationError(id = it.id, message = t.message ?: t.javaClass.simpleName)
                }
            }
        }
    }

    @MainThread
    private fun onMigrationFailed(error: Throwable) {
        val id = when (error) {
            is MigrationError -> error.id
            else -> -1
        }
        migrationsDb.setFailed(id, error.message ?: error.javaClass.simpleName)
        (status as MutableLiveData<Status>).value = Status.FAILED
    }

    @MainThread
    private fun onMigrationSuccess() {
        migrationsDb.lastMigratedVersion = appInfo.versionCode
        (status as MutableLiveData<Status>).value = Status.APPLIED
    }
}
