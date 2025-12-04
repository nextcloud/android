/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import com.nextcloud.utils.extensions.getDepth
import org.junit.Test

class OCFileDepthTests {

    @Test
    fun testGetNavStateWhenGivenNullPathShouldReturnNull() {
        assert(null.getDepth() == null)
    }

    @Test
    fun testGetNavStateWhenGivenRootPathShouldReturnRootState() {
        val path = "/"
        val folder = OCFile(path).apply {
            fileId = 1
            remoteId = "01"
            decryptedRemotePath = path
        }
        assert(folder.getDepth() == OCFileDepth.Root)
    }

    @Test
    fun testGetNavStateWhenGivenRootSubDirOfRootPathShouldReturnSubDirOfRootState() {
        val path = "/Abc/"
        val folder = OCFile(path).apply {
            fileId = 2
            remoteId = "02"
            decryptedRemotePath = path
        }
        assert(folder.getDepth() == OCFileDepth.FirstLevel)
    }

    @Test
    fun testGetNavStateWhenGivenRootSubDirOfDirPathShouldReturnSubDirOfDirState() {
        val path = "/Abc/Ba/"
        val folder = OCFile(path).apply {
            fileId = 3
            remoteId = "03"
            decryptedRemotePath = path
        }
        assert(folder.getDepth() == OCFileDepth.DeepLevel)
    }
}
