/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.fileNameValidator.FileNameValidator
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@Suppress("TooManyFunctions")
class FileNameValidatorTests : AbstractOnServerIT() {

    private var capability: OCCapability = fileDataStorageManager.getCapability(account.name)

    @Before
    fun setup() {
        capability = capability.apply {
            forbiddenFilenamesJson = """[".htaccess",".htaccess"]"""
            forbiddenFilenameBaseNamesJson = """
                                    ["con", "prn", "aux", "nul", "com0", "com1", "com2", "com3", "com4", 
                                    "com5", "com6", "com7", "com8", "com9", "com¹", "com²", "com³", 
                                    "lpt0", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", 
                                    "lpt8", "lpt9", "lpt¹", "lpt²", "lpt³"]
                                    """
            forbiddenFilenameExtensionJson = """[" ",".",".part",".part"]"""
            forbiddenFilenameCharactersJson = """["<", ">", ":", "\\\\", "/", "|", "?", "*", "&"]"""
        }
    }

    @Test
    fun testInvalidCharacter() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val result = FileNameValidator.checkFileName("file<name", capability, targetContext)
        assertEquals(
            String.format(targetContext.getString(R.string.file_name_validator_error_invalid_character), "<"),
            result
        )
    }

    @Test
    fun testReservedName() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val result = FileNameValidator.checkFileName("CON", capability, targetContext)
        assertEquals(targetContext.getString(R.string.file_name_validator_error_reserved_names, "con"), result)
    }

    @Test
    fun testForbiddenFilenameExtension() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val result = FileNameValidator.checkFileName("my_fav_file.part", capability, targetContext)
        assertEquals(
            targetContext.getString(R.string.file_name_validator_error_forbidden_file_extensions, ".part"),
            result
        )
    }

    @Test
    fun testEndsWithSpaceOrPeriod() {
        val firstFilename = "test "
        val secondFilename = "test."
        val result = FileNameValidator.checkFileName(firstFilename, capability, targetContext)
        val result2 = FileNameValidator.checkFileName(secondFilename, capability, targetContext)

        if (capability.version.isOlderThan(NextcloudVersion.nextcloud_30)) {
            assertEquals(null, result)
            assertEquals(null, result2)
        } else {
            assertEquals(
                targetContext.getString(R.string.file_name_validator_error_forbidden_space_character_extensions),
                result
            )
            assertEquals(
                targetContext.getString(R.string.file_name_validator_error_forbidden_file_extensions, "."),
                result2
            )
        }
    }

    @Test
    fun testEmptyFileName() {
        val result = FileNameValidator.checkFileName("", capability, targetContext)
        assertEquals(targetContext.getString(R.string.filename_empty), result)
    }

    @Test
    fun testBlankFileName() {
        val result = FileNameValidator.checkFileName("      ", capability, targetContext)
        assertEquals(targetContext.getString(R.string.filename_empty), result)
    }

    @Test
    fun testFileAlreadyExists() {
        val existingFiles = setOf("existingFile")
        val result = FileNameValidator.checkFileName("existingFile", capability, targetContext, existingFiles)
        assertEquals(targetContext.getString(R.string.file_already_exists), result)
    }

    @Test
    fun testValidFileName() {
        val result = FileNameValidator.checkFileName("validFileName", capability, targetContext)
        assertNull(result)
    }

    @Test
    fun testIsFileHidden() {
        assertTrue(FileNameValidator.isFileHidden(".hiddenFile"))
        assertFalse(FileNameValidator.isFileHidden("visibleFile"))
    }

    @Test
    fun testIsFileNameAlreadyExist() {
        val existingFiles = setOf("existingFile")
        assertTrue(FileNameValidator.isFileNameAlreadyExist("existingFile", existingFiles))
        assertFalse(FileNameValidator.isFileNameAlreadyExist("newFile", existingFiles))
    }

    @Test
    fun testValidFolderAndFilePaths() {
        val folderPath = "validFolder"
        val filePaths = listOf("file1.txt", "file2.doc", "file3.jpg")

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, filePaths, capability, targetContext)
        assertTrue(result)
    }

    @Test
    fun testFolderPathWithReservedName() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val folderPath = "CON"
        val filePaths = listOf("file1.txt", "file2.doc", "file3.jpg")

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, filePaths, capability, targetContext)
        assertFalse(result)
    }

    @Test
    fun testFilePathWithReservedName() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val folderPath = "validFolder"
        val filePaths = listOf("file1.txt", "PRN.doc", "file3.jpg")

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, filePaths, capability, targetContext)
        assertFalse(result)
    }

    @Test
    fun testFolderPathWithInvalidCharacter() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val folderPath = "invalid<Folder"
        val filePaths = listOf("file1.txt", "file2.doc", "file3.jpg")

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, filePaths, capability, targetContext)
        assertFalse(result)
    }

    @Test
    fun testFilePathWithInvalidCharacter() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val folderPath = "validFolder"
        val filePaths = listOf("file1.txt", "file|2.doc", "file3.jpg")

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, filePaths, capability, targetContext)
        assertFalse(result)
    }

    @Test
    fun testFolderPathEndingWithSpace() {
        val folderPath = "folderWithSpace "
        val filePaths = listOf("file1.txt", "file2.doc", "file3.jpg")

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, filePaths, capability, targetContext)
        assertEquals(capability.version.isOlderThan(NextcloudVersion.nextcloud_30), result)
    }

    @Test
    fun testFilePathEndingWithPeriod() {
        val folderPath = "validFolder"
        val filePaths = listOf("file1.txt", "file2.doc", "file3.")

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, filePaths, capability, targetContext)
        assertEquals(capability.version.isOlderThan(NextcloudVersion.nextcloud_30), result)
    }

    @Test
    fun testFilePathWithNestedFolder() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val folderPath = "validFolder\\secondValidFolder\\CON"
        val filePaths = listOf("file1.txt", "file2.doc", "file3.")

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, filePaths, capability, targetContext)
        assertFalse(result)
    }

    @Test
    fun testOnlyFolderPath() {
        val folderPath = "/A1/Aaaww/W/C2/"

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, listOf(), capability, targetContext)
        assertTrue(result)
    }

    @Test
    fun testOnlyFolderPathWithOneReservedName() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        val folderPath = "/A1/Aaaww/CON/W/C2/"

        val result = FileNameValidator.checkFolderAndFilePaths(folderPath, listOf(), capability, targetContext)
        assertFalse(result)
    }
}
