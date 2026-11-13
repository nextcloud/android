/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.adapter.filelist

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendedFilesVisibilityTest {
    private val rootDirectory = OCFile(OCFile.ROOT_PATH).apply {
        decryptedRemotePath = OCFile.ROOT_PATH
    }
    private val recommendations = listOf(OCFile("/example.txt"))

    @Test
    fun `recommendations are hidden when setting is disabled`() {
        assertFalse(shouldDisplayRecommendedFiles(false, recommendations, rootDirectory))
    }

    @Test
    fun `recommendations are shown when enabled at root`() {
        assertTrue(shouldDisplayRecommendedFiles(true, recommendations, rootDirectory))
    }

    @Test
    fun `recommendations are hidden outside root`() {
        val subdirectory = OCFile("/Documents/").apply {
            decryptedRemotePath = "/Documents/"
        }

        assertFalse(shouldDisplayRecommendedFiles(true, recommendations, subdirectory))
    }
}
