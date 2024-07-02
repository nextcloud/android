/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.fileNameValidator.FileNameValidator
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameValidatorTests: AbstractIT() {

    @Test
    fun testInvalidCharacter() {
        val result = FileNameValidator.isValid("file<name", targetContext)
        assertEquals("File name contains invalid characters: <", result)
    }

    @Test
    fun testReservedName() {
        val result = FileNameValidator.isValid("CON", targetContext)
        assertEquals(targetContext.getString(R.string.file_name_validator_error_reserved_names), result)
    }

    @Test
    fun testEndsWithSpaceOrPeriod() {
        val result = FileNameValidator.isValid("filename ", targetContext)
        assertEquals(targetContext.getString(R.string.file_name_validator_error_ends_with_space_period), result)

        val result2 = FileNameValidator.isValid("filename.", targetContext)
        assertEquals(targetContext.getString(R.string.file_name_validator_error_ends_with_space_period), result2)
    }

    @Test
    fun testEmptyFileName() {
        val result = FileNameValidator.isValid("", targetContext)
        assertEquals(targetContext.getString(R.string.filename_empty), result)
    }

    @Test
    fun testFileAlreadyExists() {
        val existingFiles = mutableSetOf("existingFile")
        val result = FileNameValidator.isValid("existingFile", targetContext, existingFiles)
        assertEquals(targetContext.getString(R.string.file_already_exists), result)
    }

    @Test
    fun testValidFileName() {
        val result = FileNameValidator.isValid("validFileName", targetContext)
        assertNull(result)
    }

    @Test
    fun testIsFileHidden() {
        assertTrue(FileNameValidator.isFileHidden(".hiddenFile"))
        assertFalse(FileNameValidator.isFileHidden("visibleFile"))
    }

    @Test
    fun testIsFileNameAlreadyExist() {
        val existingFiles = mutableSetOf("existingFile")
        assertTrue(FileNameValidator.isFileNameAlreadyExist("existingFile", existingFiles))
        assertFalse(FileNameValidator.isFileNameAlreadyExist("newFile", existingFiles))
    }
}
