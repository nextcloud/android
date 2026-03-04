/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Philipp Hasper <vcs@hasper.info>
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.di

import androidx.activity.result.contract.ActivityResultContract
import com.nextcloud.client.documentscan.AppScanOptionalFeature
import dagger.Module
import dagger.Provides
import dagger.Reusable

@Module
internal class VariantModule {
    /**
     * Using reflection to determine whether the ScanPageContract class from the appscan project is available.
     * If yes, an instance of it is returned. If not, a stub is returned indicating the feature is not available.
     *
     * To make it available for your specific variant, make sure it is included in your build.gradle,
     * e.g.: `"qaImplementation"(project(":appscan"))`
     */
    @Provides
    @Reusable
    fun scanOptionalFeature(): AppScanOptionalFeature = try {
        // Try to load the ScanPageContract class only if the appscan project is present
        val clazz = Class.forName("com.nextcloud.appscan.ScanPageContract")

        @Suppress("UNCHECKED_CAST")
        val contractInstance =
            clazz.getDeclaredConstructor().newInstance() as ActivityResultContract<Unit, String?>
        object : AppScanOptionalFeature() {
            override fun getScanContract(): ActivityResultContract<Unit, String?> = contractInstance
        }
    } catch (_: ClassNotFoundException) {
        // appscan module is not present in this variant
        AppScanOptionalFeature.Stub
    } catch (_: Exception) {
        // Any reflection/instantiation error -> be safe and use stub
        AppScanOptionalFeature.Stub
    }
}
