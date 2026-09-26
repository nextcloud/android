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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.After
import org.junit.Before
import org.junit.Test

@Suppress("TooManyFunctions")
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
        assertTrue(E2EVersionHelper.isV2Plus(E2EVersion.V2_0))
        assertTrue(E2EVersionHelper.isV2Plus(E2EVersion.V2_1))
    }

    @Test
    fun `isV2orAbove returns false for non V2 versions`() {
        assertFalse(E2EVersionHelper.isV2Plus(E2EVersion.V1_0))
        assertFalse(E2EVersionHelper.isV2Plus(E2EVersion.V1_1))
        assertFalse(E2EVersionHelper.isV2Plus(E2EVersion.V1_2))
        assertFalse(E2EVersionHelper.isV2Plus(E2EVersion.UNKNOWN))
    }

    @Test
    fun `isV1 returns true for all V1 versions`() {
        assertTrue(E2EVersionHelper.isV1(E2EVersion.V1_0))
        assertTrue(E2EVersionHelper.isV1(E2EVersion.V1_1))
        assertTrue(E2EVersionHelper.isV1(E2EVersion.V1_2))
    }

    @Test
    fun `isV1 returns false for non V1 versions`() {
        assertFalse(E2EVersionHelper.isV1(E2EVersion.V2_0))
        assertFalse(E2EVersionHelper.isV1(E2EVersion.V2_1))
        assertFalse(E2EVersionHelper.isV1(E2EVersion.UNKNOWN))
    }

    @Test
    fun `getLatestE2EVersion returns latest V2 when isV2 is true`() {
        assertEquals(E2EVersion.V2_1, E2EVersion.max())
    }

    @Test
    fun `determineE2EVersion returns V1_0`() {
        mockV1("1.0")
        assertEquals(E2EVersion.V1_0, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion via double returns V1_0`() {
        mockV1Double(1.0)
        assertEquals(E2EVersion.V1_0, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion via second double returns V1_0`() {
        mockV1Double(1.00)
        assertEquals(E2EVersion.V1_0, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion via third double returns V1_1`() {
        mockV1Double(1.10)
        assertEquals(E2EVersion.V1_1, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion via fourth double returns V1_2`() {
        mockV1Double(1.2)
        assertEquals(E2EVersion.V1_2, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion returns V1_1`() {
        mockV1("1.1")
        assertEquals(E2EVersion.V1_1, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion returns V1_2`() {
        mockV1("1.2")
        assertEquals(E2EVersion.V1_2, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion returns V2_0 for 2_0 or 2`() {
        mockV1Throw()
        mockV2("2.0")
        assertEquals(E2EVersion.V2_0, E2EVersionHelper.fromMetadata("meta"))

        mockV2("2")
        assertEquals(E2EVersion.V2_0, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion returns V2_1`() {
        mockV1Throw()
        mockV2("2.1")
        assertEquals(E2EVersion.V2_1, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion returns UNKNOWN for unknown V2 version`() {
        mockV1Throw()
        mockV2("3.0")
        assertEquals(E2EVersion.UNKNOWN, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EVersion returns UNKNOWN when both deserializations fail`() {
        every {
            EncryptionUtils.deserializeJSON(
                any(),
                ofType<TypeToken<EncryptedFolderMetadataFileV1>>()
            )
        } throws RuntimeException()

        every {
            EncryptionUtils.deserializeJSON(
                any(),
                ofType<TypeToken<EncryptedFolderMetadataFile>>()
            )
        } throws RuntimeException()

        assertEquals(E2EVersion.UNKNOWN, E2EVersionHelper.fromMetadata("meta"))
    }

    @Test
    fun `determineE2EFromVersionString maps versions correctly`() {
        assertEquals(E2EVersion.V1_0, E2EVersion.fromValue("1.0"))
        assertEquals(E2EVersion.V1_1, E2EVersion.fromValue("1.1"))
        assertEquals(E2EVersion.V1_2, E2EVersion.fromValue("1.2"))
        assertEquals(E2EVersion.V2_0, E2EVersion.fromValue("2"))
        assertEquals(E2EVersion.V2_0, E2EVersion.fromValue("2.0"))
        assertEquals(E2EVersion.V2_1, E2EVersion.fromValue("2.1"))
    }

    @Test
    fun `determineE2EFromVersionString returns UNKNOWN for invalid input`() {
        assertEquals(E2EVersion.UNKNOWN, E2EVersion.fromValue(null))
        assertEquals(E2EVersion.UNKNOWN, E2EVersion.fromValue(""))
        assertEquals(E2EVersion.UNKNOWN, E2EVersion.fromValue("3.0"))
    }

    @Test
    fun `getMaxCompatibleE2EEVersion returns UNKNOWN when given UNKNOWN`() {
        assertEquals(
            E2EVersion.UNKNOWN,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.UNKNOWN)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion returns V1_0 when given V1_0`() {
        assertEquals(
            E2EVersion.V1_0,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_0)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion returns V1_1 when given V1_1`() {
        assertEquals(
            E2EVersion.V1_1,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_1)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion returns V1_2 when given V1_2`() {
        assertEquals(
            E2EVersion.V1_2,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_2)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion returns client V1 max when given V1 above client max`() {
        assertEquals(
            E2EVersion.V1_2,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_2)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion returns V2_0 when given V2_0`() {
        assertEquals(
            E2EVersion.V2_0,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V2_0)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion returns V2_1 when given V2_1`() {
        assertEquals(
            E2EVersion.V2_1,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V2_1)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion does not apply V2 ceiling when given V1_2`() {
        assertEquals(
            E2EVersion.V1_2,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V1_2)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion does not apply V1 ceiling when given V2_0`() {
        assertEquals(
            E2EVersion.V2_0,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.V2_0)
        )
    }

    @Test
    fun `getMaxCompatibleE2EEVersion returns V2_1 when given unsupported V3_0`() {
        assertEquals(
            E2EVersion.V2_1,
            E2EVersionHelper.getMaxCompatibleE2EEVersion(E2EVersion.fromValue("3.0"))
        )
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

    private fun mockV1Double(version: Double) {
        val v1 = mockk<EncryptedFolderMetadataFileV1> {
            every { metadata.version } returns version
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
