/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UploadFolderResolverTest {
    private val root = folder("/")
    private val children = mutableMapOf<Pair<OCFile, String>, OCFile>()
    private val refreshed = mutableListOf<OCFile>()
    private val created = mutableListOf<Pair<OCFile, String>>()

    @Test
    fun discoversEncryptedChildOfOrdinaryRootFromFreshListing() {
        val ordinary = folder("/A/")
        val encrypted = folder("/A/B/", encrypted = true)
        val resolver = resolver { parent ->
            if (parent === root) children[root to "A"] = ordinary
            if (parent === ordinary) children[ordinary to "B"] = encrypted
        }

        assertSame(encrypted, resolver.resolve("/A/B/"))
        assertEquals(listOf(root, ordinary, encrypted), refreshed)
        assertTrue(created.isEmpty())
    }

    @Test
    fun createsEveryMissingLevelInsideEncryptedParentInOrder() {
        val encrypted = folder("/private/")
        children[root to "private"] = encrypted
        val resolver = resolver()

        resolver.resolve("/private/2026/11/照片/")

        assertEquals(listOf("2026", "11", "照片"), created.map { it.second })
        assertSame(encrypted, created.first().first)
        assertSame(refreshed[2], created[1].first)
        assertSame(refreshed[3], created[2].first)
    }

    @Test
    fun usesLogicalNamesForChildrenWithCiphertextPaths() {
        val encrypted = folder("/private/")
        val child = folder("/private/randomCiphertext/", "/private/旅行/", true)
        children[root to "private"] = encrypted
        children[encrypted to "旅行"] = child

        assertSame(child, resolver().resolve("/private/旅行/"))
        assertTrue(created.isEmpty())
    }

    @Test
    fun backslashInAndroidFolderNameIsNotTreatedAsAPathSeparator() {
        val child = folder("/cipher/", "/back\\slash/", true)
        children[root to "back\\slash"] = child

        assertSame(child, resolver().resolve("/back\\slash/"))
        assertTrue(created.isEmpty())
    }

    @Test
    fun recreatesDeletedFoldersUsingFreshListingInsteadOfStaleCache() {
        val stale = folder("/A/")
        children[root to "A"] = stale
        val resolver = resolver { if (it === root) children.remove(root to "A") }

        val recreated = resolver.resolve("/A/B/")

        assertEquals(listOf("A", "B"), created.map { it.second })
        assertEquals("/A/B/", recreated.decryptedRemotePath)
    }

    @Test
    fun keyOrMetadataFailureStopsBeforeCreatingOrResolvingFurtherChildren() {
        val encrypted = folder("/private/", encrypted = true)
        children[root to "private"] = encrypted
        val failure = IllegalStateException("Keys unavailable")
        val resolver = resolver { if (it === encrypted) throw failure }

        assertSame(failure, assertThrows(IllegalStateException::class.java) { resolver.resolve("/private/new/") })
        assertTrue(created.isEmpty())
    }

    @Test
    fun rejectsInvalidPathsBeforeAnyRemoteOperation() {
        listOf("relative/path/", "/a/../b/", "/./").forEach { path ->
            assertThrows(IllegalArgumentException::class.java) { resolver().resolve(path) }
        }
        assertTrue(refreshed.isEmpty())
        assertTrue(created.isEmpty())
    }

    private fun resolver(onRefresh: (OCFile) -> Unit = {}) = UploadFolderResolver(
        root = { root },
        refresh = {
            refreshed.add(it)
            onRefresh(it)
        },
        findChild = { parent, name -> children[parent to name] },
        createChild = { parent, name ->
            created.add(parent to name)
            folder(parent.decryptedRemotePath + name + "/").also { children[parent to name] = it }
        }
    )

    private fun folder(path: String, logicalPath: String = path, encrypted: Boolean = false): OCFile =
        mock<OCFile>().also {
            whenever(it.remotePath).thenReturn(path)
            whenever(it.decryptedRemotePath).thenReturn(logicalPath)
            whenever(it.isFolder).thenReturn(true)
            whenever(it.isEncrypted).thenReturn(encrypted)
        }
}
