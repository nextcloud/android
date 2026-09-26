/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.di

import com.nextcloud.client.documentscan.AppScanOptionalFeature
import dagger.Component
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit test for [VariantModule] that tests the reflection-based approach
 * to conditionally load the ScanPageContract.
 */
class VariantModuleTest {

    private lateinit var component: TestVariantComponent

    @Before
    fun setup() {
        component = DaggerVariantModuleTest_TestVariantComponent.create()
    }

    @Test
    fun testAppScanWhenAvailableShouldReturnContract() {
        val feature = component.appScanOptionalFeature()

        assertTrue(feature.isAvailable)
        assertNotEquals(AppScanOptionalFeature.Stub, feature)

        assertNotNull(feature.getScanContract())
    }

    @Component(modules = [VariantModule::class])
    interface TestVariantComponent {
        fun appScanOptionalFeature(): AppScanOptionalFeature
    }
}
