/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.ArbitraryDataProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@Suppress("SpreadOperator")
class EncryptedFileComparisonCacheTest {
    private val values = mutableMapOf<Pair<String, String>, String>()
    private val provider: ArbitraryDataProvider = mock()
    private val cache = EncryptedFileComparisonCache(provider, "account-A")

    @Before
    fun setUp() {
        whenever(provider.getValue(any<String>(), any())).thenAnswer {
            values[it.getArgument<String>(0) to it.getArgument<String>(1)].orEmpty()
        }
        doAnswer {
            values[it.getArgument<String>(0) to it.getArgument<String>(1)] = it.getArgument(2)
            null
        }.whenever(provider).storeOrUpdateKeyValue(any<String>(), any(), any<String>())
    }

    @Test
    fun unchangedVersionReusesVerifiedEquality() {
        cache.write("file", "version-A", true, 42)
        assertEquals(true to 42L, cache.read("file", "version-A"))
    }

    @Test
    fun changedRemoteOrLocalVersionRequiresNewVerification() {
        cache.write("file", "version-A", true, 42)
        assertNull(cache.read("file", "version-B"))
    }

    @Test
    fun verifiedDifferenceIsAlsoReused() {
        cache.write("file", "version-A", false, 42)
        assertEquals(false to 42L, cache.read("file", "version-A"))
    }

    @Test
    fun recordsCannotBeReusedAcrossAccountsOrPaths() {
        cache.write("file", "version-A", true, 42)
        assertNull(EncryptedFileComparisonCache(provider, "account-B").read("file", "version-A"))
        assertNull(cache.read("another-file", "version-A"))
    }

    @Test
    fun everyLocalAndRemoteVersionFieldInvalidatesTheFingerprint() {
        val fields = arrayOf("size", "mtime", "etag", "remote-id", "cipher-size", "remote-mtime", "key", "nonce")
        val original = EncryptedFileComparisonCache.fingerprint(*fields)
        fields.indices.forEach { index ->
            val changed = fields.copyOf().also { it[index] += "-changed" }
            assertNotEquals(original, EncryptedFileComparisonCache.fingerprint(*changed))
        }
    }
}
