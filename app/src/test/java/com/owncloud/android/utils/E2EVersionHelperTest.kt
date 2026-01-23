/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils

import com.google.gson.reflect.TypeToken
import com.nextcloud.utils.e2ee.E2EVersionHelper
import com.owncloud.android.datamodel.e2e.v1.encrypted.EncryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.lib.resources.status.E2EVersion
import io.mockk.every
import io.mockk.mockk
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test

class E2EVersionHelperTest {

    @Before
    fun setup() {
        io.mockk.mockkStatic(EncryptionUtils::class)
    }

    @After
    fun teardown() {
        io.mockk.unmockkAll()
    }

    @Test
    fun `isV2orAbove returns true for V2 versions`() {
        TestCase.assertTrue(E2EVersionHelper.isV2orAbove(E2EVersion.V2_0))
        TestCase.assertTrue(E2EVersionHelper.isV2orAbove(E2EVersion.V2_1))
    }

    @Test
    fun `isV2orAbove returns false for non V2 versions`() {
        TestCase.assertFalse(E2EVersionHelper.isV2orAbove(E2EVersion.V1_0))
        TestCase.assertFalse(E2EVersionHelper.isV2orAbove(E2EVersion.V1_1))
        TestCase.assertFalse(E2EVersionHelper.isV2orAbove(E2EVersion.V1_2))
        TestCase.assertFalse(E2EVersionHelper.isV2orAbove(E2EVersion.UNKNOWN))
    }

    @Test
    fun `isV1 returns true for all V1 versions`() {
        TestCase.assertTrue(E2EVersionHelper.isV1(E2EVersion.V1_0))
        TestCase.assertTrue(E2EVersionHelper.isV1(E2EVersion.V1_1))
        TestCase.assertTrue(E2EVersionHelper.isV1(E2EVersion.V1_2))
    }

    @Test
    fun `isV1 returns false for non V1 versions`() {
        TestCase.assertFalse(E2EVersionHelper.isV1(E2EVersion.V2_0))
        TestCase.assertFalse(E2EVersionHelper.isV1(E2EVersion.V2_1))
        TestCase.assertFalse(E2EVersionHelper.isV1(E2EVersion.UNKNOWN))
    }

    @Test
    fun `getLatestE2EVersion returns latest V2 when isV2 is true`() {
        assertEquals(E2EVersion.V2_1, E2EVersionHelper.getLatestE2EVersion(true))
    }

    @Test
    fun `getLatestE2EVersion returns latest V1 when isV2 is false`() {
        assertEquals(E2EVersion.V1_2, E2EVersionHelper.getLatestE2EVersion(false))
    }

    @Test
    fun `determineE2EVersion returns V1_0`() {
        mockV1("1.0")
        assertEquals(E2EVersion.V1_0, E2EVersionHelper.determineE2EVersion("meta"))
    }

    @Test
    fun `determineE2EVersion returns V1_1`() {
        mockV1("1.1")
        assertEquals(E2EVersion.V1_1, E2EVersionHelper.determineE2EVersion("meta"))
    }

    @Test
    fun `determineE2EVersion returns V1_2`() {
        mockV1("1.2")
        assertEquals(E2EVersion.V1_2, E2EVersionHelper.determineE2EVersion("meta"))
    }

    @Test
    fun `determineE2EVersion returns V2_0 for 2_0 or 2`() {
        mockV1Throw()
        mockV2("2.0")
        assertEquals(E2EVersion.V2_0, E2EVersionHelper.determineE2EVersion("meta"))

        mockV2("2")
        assertEquals(E2EVersion.V2_0, E2EVersionHelper.determineE2EVersion("meta"))
    }

    @Test
    fun `determineE2EVersion returns V2_1`() {
        mockV1Throw()
        mockV2("2.1")
        assertEquals(E2EVersion.V2_1, E2EVersionHelper.determineE2EVersion("meta"))
    }

    @Test
    fun `determineE2EVersion returns UNKNOWN for unknown V2 version`() {
        mockV1Throw()
        mockV2("3.0")
        assertEquals(E2EVersion.UNKNOWN, E2EVersionHelper.determineE2EVersion("meta"))
    }

    @Test
    fun `determineE2EVersion returns UNKNOWN when both deserializations fail`() {
        every {
            EncryptionUtils.deserializeJSON<Any>(any(), any<TypeToken<Any>>())
        } throws RuntimeException()

        assertEquals(E2EVersion.UNKNOWN, E2EVersionHelper.determineE2EVersion("meta"))
    }

    private fun mockV1(version: String) {
        val v1 = mockk<EncryptedFolderMetadataFileV1> {
            every { metadata.version } returns version.toDouble()
        }

        every {
            EncryptionUtils.deserializeJSON(
                any(),
                ofType<TypeToken<EncryptedFolderMetadataFileV1>>()
            )
        } returns v1
    }

    private fun mockV1Throw() {
        every {
            EncryptionUtils.deserializeJSON(
                any(),
                ofType<TypeToken<EncryptedFolderMetadataFileV1>>()
            )
        } throws RuntimeException()
    }

    private fun mockV2(version: String) {
        val v2 = mockk<EncryptedFolderMetadataFile> {
            every { this@mockk.version } returns version
        }

        every {
            EncryptionUtils.deserializeJSON(
                any(),
                ofType<TypeToken<EncryptedFolderMetadataFile>>()
            )
        } returns v2
    }
}
