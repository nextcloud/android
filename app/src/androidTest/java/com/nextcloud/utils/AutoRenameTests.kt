/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.autoRename.AutoRename
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import org.junit.Before
import org.junit.Test

@Suppress("TooManyFunctions")
class AutoRenameTests : AbstractOnServerIT() {

    private var capability: OCCapability = fileDataStorageManager.getCapability(account.name)
    private val forbiddenFilenameExtension = "."
    private val forbiddenFilenameCharacter = ">"

    @Before
    fun setup() {
        testOnlyOnServer(NextcloudVersion.nextcloud_30)

        capability = capability.apply {
            forbiddenFilenameExtensionJson = listOf(
                """[" ",".",".part",".part"]""",
                """[".",".part",".part"," "]""",
                """[".",".part"," ", ".part"]""",
                """[".part"," ", ".part","."]""",
                """[" ",".",".PART",".PART"]""",
                """[".",".PART",".PART"," "]""",
                """[".",".PART"," ", ".PART"]""",
                """[".PART"," ", ".PART","."]"""
            ).random()
            forbiddenFilenameCharactersJson = """["<", ">", ":", "\\\\", "/", "|", "?", "*", "&"]"""
        }
    }

    @Test
    fun testInvalidChar() {
        val filename = "file${forbiddenFilenameCharacter}file.txt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "file_file.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testInvalidExtension() {
        val filename = "file$forbiddenFilenameExtension"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "file_"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testMultipleInvalidChars() {
        val filename = "file|name?<>.txt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "file_name___.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testStartEndInvalidExtensions() {
        val filename = " .file.part "
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "_file_part"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testStartInvalidExtension() {
        val filename = " .file.part"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "_file_part"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testEndInvalidExtension() {
        val filename = ".file.part "
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "_file_part"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testMiddleNonPrintableChar() {
        val filename = "file\u0001name.txt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "filename.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testStartNonPrintableChar() {
        val filename = "\u0001filename.txt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "filename.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testEndNonPrintableChar() {
        val filename = "filename.txt\u0001"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "filename.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testExtensionNonPrintableChar() {
        val filename = "filename.t\u0001xt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "filename.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testMiddleInvalidFolderChar() {
        val folderPath = "abc/def/kg$forbiddenFilenameCharacter/lmo/pp/"
        val result = AutoRename.rename(folderPath, capability)
        val expectedFolderName = "abc/def/kg_/lmo/pp/"
        assert(result == expectedFolderName) { "Expected $expectedFolderName but got $result" }
    }

    @Test
    fun testEndInvalidFolderChar() {
        val folderPath = "abc/def/kg/lmo/pp$forbiddenFilenameCharacter/"
        val result = AutoRename.rename(folderPath, capability)
        val expectedFolderName = "abc/def/kg/lmo/pp_/"
        assert(result == expectedFolderName) { "Expected $expectedFolderName but got $result" }
    }

    @Test
    fun testStartInvalidFolderChar() {
        val folderPath = "${forbiddenFilenameCharacter}abc/def/kg/lmo/pp/"
        val result = AutoRename.rename(folderPath, capability)
        val expectedFolderName = "_abc/def/kg/lmo/pp/"
        assert(result == expectedFolderName) { "Expected $expectedFolderName but got $result" }
    }

    @Test
    fun testMixedInvalidChar() {
        val filename = " file\u0001na${forbiddenFilenameCharacter}me.txt "
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "filena_me.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testStartsWithPathSeparator() {
        val folderPath = "/abc/def/kg/lmo/pp$forbiddenFilenameCharacter/file.txt/"
        val result = AutoRename.rename(folderPath, capability)
        val expectedFolderName = "/abc/def/kg/lmo/pp_/file.txt/"
        assert(result == expectedFolderName) { "Expected $expectedFolderName but got $result" }
    }

    @Test
    fun testStartsWithPathSeparatorAndValidFilepath() {
        val folderPath = "/COm02/2569.webp/"
        val result = AutoRename.rename(folderPath, capability)
        val expectedFolderName = "/COm02/2569.webp/"
        assert(result == expectedFolderName) { "Expected $expectedFolderName but got $result" }
    }

    @Test
    fun testValidFilename() {
        val filename = ".file.TXT"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "_file.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testRenameExtensionForFolder() {
        val filename = "/Pictures/@User/SubDir/08.16.07 Ka Yel/"
        val result = AutoRename.rename(filename, capability)
        assert(result == filename) { "Expected $filename but got $result" }
    }

    @Test
    fun testRenameExtensionForFile() {
        val filename = "/Pictures/@User/SubDir/08.16.07 Ka Yel.TXT"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "/Pictures/@User/SubDir/08.16.07 Ka Yel.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testE2EEFile() {
        val decryptedFile = DecryptedFile(
            authenticationTag = "HQlWBdm+gYC5kZwWnqXR1Q==",
            filename = "a:a.jpg",
            nonce = "sigyys8SfPZSScDJ860vYw==",
            mimetype = "image/jpeg",
            key = "sigyys8SfPZSScDJ860vYw=="
        )

        val result = AutoRename.rename(decryptedFile.filename, capability)
        val expectedFilename = "a_a.jpg"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testFilenameWithEmoji() {
        val filename = "Testfolder 🔵"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "Testfolder 🔵"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testFilenameWithEmojiV2() {
        val filename = "Testfolder ◼️‍⬜"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "Testfolder ◼️‍⬜"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testFilenameWithZeroWidthJoiner() {
        val filename = "Testfolder\u200D"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "Testfolder"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }
}
