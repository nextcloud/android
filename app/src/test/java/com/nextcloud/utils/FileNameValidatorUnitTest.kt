/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import android.content.Context
import com.nextcloud.utils.fileNameValidator.FileNameValidator
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.status.CapabilityBooleanType
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameValidatorUnitTest {

    private val context = mockk<Context> {
        every {
            getString(R.string.file_name_validator_error_reserved_names, "con")
        } returns "reserved con"
    }

    private val capability = OCCapability().apply {
        versionMayor = NextcloudVersion.nextcloud_32.majorVersionNumber
        isWCFEnabled = CapabilityBooleanType.TRUE
        forbiddenFilenameBaseNamesJson = """["con"]"""
    }

    @Test
    fun checkParentRemotePathsRejectsReservedPathSegment() {
        val file = OCFile("/CON/Folder/document.txt")

        val result = FileNameValidator.checkParentRemotePaths(listOf(file), capability, context)

        assertFalse(result)
    }

    @Test
    fun checkParentRemotePathsAcceptsValidNestedParentPath() {
        val file = OCFile("/Projects/Notes/document.txt")

        val result = FileNameValidator.checkParentRemotePaths(listOf(file), capability, context)

        assertTrue(result)
    }

    @Test
    fun checkParentRemotePathsAcceptsRootParentPath() {
        val file = OCFile("/document.txt")

        val result = FileNameValidator.checkParentRemotePaths(listOf(file), capability, context)

        assertTrue(result)
    }
}
