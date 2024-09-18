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

class AutoRenameTests: AbstractOnServerIT() {

    private var capability: OCCapability = fileDataStorageManager.getCapability(account.name)

    @Before
    fun setup() {
        capability = capability.apply {
            forbiddenFilenameExtensionJson = """[" ",".",".part",".part"]"""
            forbiddenFilenameCharactersJson = """["<", ">", ":", "\\\\", "/", "|", "?", "*", "&"]"""
        }
    }

    @Test
    fun testRenameWhenInvalidCharacterIncludedShouldReturnValidFilename() {
        val forbiddenFilenameCharacters = capability.forbiddenFilenameCharacters()
        val randomInvalidChar = forbiddenFilenameCharacters.random()
        val filename = "file${randomInvalidChar}file.txt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "file_file.txt"
        assert(result == expectedFilename)
    }

    @Test
    fun testRenameWhenInvalidFilenameExtensionIncludedShouldReturnValidFilename() {
        val forbiddenFilenameExtension = capability.forbiddenFilenameExtension()
        val randomInvalidFilenameExtension = forbiddenFilenameExtension.random()
        val filename = "file${randomInvalidFilenameExtension}"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "file_"
        assert(result == expectedFilename)
    }


    @Test
    fun testRenameWhenMultipleInvalidCharactersShouldReturnValidFilename() {
        val filename = "file|name?<>.txt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "file_name___.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testRenameWhenFilenameStartsAndEndsWithInvalidExtensionsShouldReturnValidFilename() {
        val filename = " .file.part "
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "_file_part"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }

    @Test
    fun testRenameWhenNonPrintableCharactersArePresentShouldRemoveThem() {
        val filename = "file\u0001name.txt"
        val result = AutoRename.rename(filename, capability)
        val expectedFilename = "filename.txt"
        assert(result == expectedFilename) { "Expected $expectedFilename but got $result" }
    }
}
