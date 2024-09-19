/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.nextcloud.utils.autoRename.AutoRename
import com.nextcloud.utils.extensions.forbiddenFilenameCharacters
import com.nextcloud.utils.extensions.forbiddenFilenameExtension
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.lib.resources.status.OCCapability
import org.junit.Before
import org.junit.Test

class AutoRenameTests : AbstractOnServerIT() {

    private var capability: OCCapability = fileDataStorageManager.getCapability(account.name)

    @Before
    fun setup() {
        capability = capability.apply {
            forbiddenFilenameExtensionJson = """[" ",".",".part",".part"]"""
            forbiddenFilenameCharactersJson = """["<", ">", ":", "\\\\", "/", "|", "?", "*", "&"]"""
        }
    }

    @Test
    fun testInvalidChar() {
        val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()
        val randomInvalidChar = forbiddenFilenameCharacters.random()

        val filename = "file${randomInvalidChar}file.txt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "file_file.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testInvalidExtension() {
        val forbiddenFilenameExtension = capability.forbiddenFilenameExtension()
        val randomInvalidFilenameExtension = forbiddenFilenameExtension.random()

        val filename = "file$randomInvalidFilenameExtension"
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
        val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()
        val randomInvalidChar = forbiddenFilenameCharacters.random()

        val folderPath = "abc/def/kg$randomInvalidChar/lmo/pp"
        val result = AutoRename.rename(folderPath, capability, true)
        val expectedFolderName = "abc/def/kg_/lmo/pp"
        assert(result == expectedFolderName) { "Expected $expectedFolderName but got $result" }
    }

    @Test
    fun testEndInvalidFolderChar() {
        val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()
        val randomInvalidChar = forbiddenFilenameCharacters.random()

        val folderPath = "abc/def/kg/lmo/pp$randomInvalidChar"
        val result = AutoRename.rename(folderPath, capability, true)
        val expectedFolderName = "abc/def/kg/lmo/pp_"
        assert(result == expectedFolderName) { "Expected $expectedFolderName but got $result" }
    }

    @Test
    fun testStartInvalidFolderChar() {
        val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()
        val randomInvalidChar = forbiddenFilenameCharacters.random()

        val folderPath = "${randomInvalidChar}abc/def/kg/lmo/pp"
        val result = AutoRename.rename(folderPath, capability, true)
        val expectedFolderName = "_abc/def/kg/lmo/pp"
        assert(result == expectedFolderName) { "Expected $expectedFolderName but got $result" }
    }

    @Test
    fun testMixedInvalidChar() {
        val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()
        val randomInvalidChar = forbiddenFilenameCharacters.random()

        val filename = " file\u0001na${randomInvalidChar}me.txt "
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "filena_me.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testFolderPathWithAccountAndServerName() {
        val folderPath = "/storage/emulated/0/Android/media/com.nextcloud.client/nextcloud/user1@wcf.ltd3.nextcloud.com/e2e/04f9f38aeb834d2890735e40bdbb82fa/gos/    268"
        val result = AutoRename.rename(folderPath, capability, true)
        val expectedFilename = "/storage/emulated/0/Android/media/com.nextcloud.client/nextcloud/user1@wcf.ltd3.nextcloud.com/e2e/04f9f38aeb834d2890735e40bdbb82fa/gos/268"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

}
