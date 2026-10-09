/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2019 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.utils

import com.nextcloud.client.preferences.SubFolderRule
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.util.Locale
import java.util.TimeZone

@Suppress("TooManyFunctions", "LargeClass")
class FileStorageUtilsTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var defaultTimeZone: TimeZone

    @Before
    fun setUp() {
        defaultTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun testValidFilenames() {
        assertTrue(FileStorageUtils.isValidExtFilename("example.txt"))
        assertTrue(FileStorageUtils.isValidExtFilename("file_name-123"))
        assertTrue(FileStorageUtils.isValidExtFilename("normalFile"))
    }

    @Test
    fun testInvalidFilenamesWithSpecialChars() {
        assertFalse(FileStorageUtils.isValidExtFilename("file:name.txt"))
        assertFalse(FileStorageUtils.isValidExtFilename("file*name"))
        assertFalse(FileStorageUtils.isValidExtFilename("file/name"))
        assertFalse(FileStorageUtils.isValidExtFilename("file\\name"))
        assertFalse(FileStorageUtils.isValidExtFilename("file|name"))
        assertFalse(FileStorageUtils.isValidExtFilename("file\"name"))
        assertFalse(FileStorageUtils.isValidExtFilename("file<name>"))
        assertFalse(FileStorageUtils.isValidExtFilename("file?name"))
    }

    @Test
    fun testFilenamesWithControlCharacters() {
        assertFalse(FileStorageUtils.isValidExtFilename("file\u0001name"))
        assertFalse(FileStorageUtils.isValidExtFilename("file\u001Fname"))
    }

    @Test
    fun testEmptyFilename() {
        assertTrue(FileStorageUtils.isValidExtFilename(""))
    }

    @Test
    fun testInstantUploadPathSubfolder() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L
        val subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/subfolder/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathNoSubfolder() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L
        val subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathEmptyDateZero() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 0L
        var subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPath() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L
        var subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderByDate() {
        val file = File("/sdcard/DCIM/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 1569918628000L
        val subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/2019/10/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderFile() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = false
        val dateTaken = 123123123L
        var subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/subfolder/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun testInstantUploadPathWithSubfolderByDateWithSubfolderFile() {
        val file = File("/sdcard/DCIM/subfolder/file.jpg")
        val syncedFolderLocalPath = "/sdcard/DCIM"
        val syncedFolderRemotePath = "/Camera"
        val subFolderByDate = true
        val dateTaken = 1569918628000L
        var subFolderRule = SubFolderRule.YEAR_MONTH

        val result = FileStorageUtils.getInstantUploadFilePath(
            file,
            Locale.ROOT,
            syncedFolderRemotePath,
            syncedFolderLocalPath,
            dateTaken,
            subFolderByDate,
            subFolderRule
        )
        val expected = "/Camera/2019/10/subfolder/file.jpg"

        assertEquals(expected, result)
    }

    @Test
    fun instantUploadPathIgnoresLetterCaseOfTheSyncedFolder() {
        val result = FileStorageUtils.getInstantUploadFilePath(
            File("/storage/emulated/0/DCIM/camera/IMG_20260101_120000.jpg"),
            Locale.ROOT,
            "/Autoupload/Camera",
            "/storage/emulated/0/DCIM/Camera",
            123123123L,
            false,
            SubFolderRule.YEAR_MONTH
        )

        assertEquals("/Autoupload/Camera/IMG_20260101_120000.jpg", result)
    }

    @Test
    fun instantUploadPathNeverMirrorsALocalPathThatIsNotBelowTheSyncedFolder() {
        val result = FileStorageUtils.getInstantUploadFilePath(
            File("/storage/emulated/0/Pictures/Other/file.jpg"),
            Locale.ROOT,
            "/Autoupload/Camera",
            "/storage/emulated/0/DCIM/Camera",
            123123123L,
            false,
            SubFolderRule.YEAR_MONTH
        )

        assertEquals("/Autoupload/Camera/file.jpg", result)
    }

    @Test
    fun instantUploadPathStripsTheSyncedFolderOnlyFromTheStart() {
        val result = FileStorageUtils.getInstantUploadFilePath(
            File("/sdcard/DCIM/DCIM/file.jpg"),
            Locale.ROOT,
            "/Camera",
            "/sdcard/DCIM",
            123123123L,
            false,
            SubFolderRule.YEAR_MONTH
        )

        assertEquals("/Camera/DCIM/file.jpg", result)
    }

    @Test
    fun instantUploadPathToleratesATrailingSeparatorOnTheSyncedFolder() {
        val result = FileStorageUtils.getInstantUploadFilePath(
            File("/sdcard/DCIM/file.jpg"),
            Locale.ROOT,
            "/Camera",
            "/sdcard/DCIM/",
            123123123L,
            false,
            SubFolderRule.YEAR_MONTH
        )

        assertEquals("/Camera/file.jpg", result)
    }

    @Test
    fun testGetFilenameAndExtensionWhenGivenInvalidFilenamesWithSpecialChars() {
        val result = FileStorageUtils.getFilenameAndExtension("invoice\u202Ecod.exe", false, false)
        assertEquals("invoice\u202Ecod", result.first)
        assertEquals(".exe", result.second)
    }

    @Test
    fun testGetFilenameAndExtensionWhenGivenMultipleDotsInFilename() {
        val result = FileStorageUtils.getFilenameAndExtension("archive.tar.gz", false, false)
        assertEquals("archive.tar", result.first)
        assertEquals(".gz", result.second)
    }

    @Test
    fun testGetFilenameAndExtensionWhenGivenFolderName() {
        val result = FileStorageUtils.getFilenameAndExtension("myFolder", true, false)
        assertEquals("myFolder", result.first)
        assertEquals("", result.second)
    }

    @Test
    fun testGetFilenameAndExtensionWhenGivenNormalFile() {
        val result = FileStorageUtils.getFilenameAndExtension("document.txt", false, false)
        assertEquals("document", result.first)
        assertEquals(".txt", result.second)
    }

    @Test
    fun testGetFilenameAndExtensionRTL() {
        val result = FileStorageUtils.getFilenameAndExtension("document.txt", false, true)
        assertEquals(".txt", result.first)
        assertEquals("document", result.second)
    }

    @Test
    fun testGetFilenameAndExtensionRTLEmptyExtension() {
        val result = FileStorageUtils.getFilenameAndExtension("document", false, true)
        assertEquals("", result.first)
        assertEquals("document", result.second)
    }

    @Test
    fun testGetFilenameAndExtensionEmptyExtension() {
        val result = FileStorageUtils.getFilenameAndExtension("document", false, false)
        assertEquals("document", result.first)
        assertEquals("", result.second)
    }

    @Test
    fun testGetFilenameAndExtensionWithRLO() {
        val filename = "Foo\u202Edm.exe"
        val result = FileStorageUtils.getFilenameAndExtension(filename, false, false)

        // we are not touching the filename that's why expected filename must stay same
        assertEquals("Foo\u202Edm", result.first)
        assertEquals(".exe", result.second)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenNormalFilenameShouldReturnFalse() {
        val result = FileStorageUtils.containsBidiControlCharacters("myfile.txt")
        assertFalse(result)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenFilenameWithEncodedRtlOverrideShouldReturnTrue() {
        val result = FileStorageUtils.containsBidiControlCharacters("myfile%e2%80%aetxt.exe")
        assertTrue(result)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenFilenameWithRawRtlOverrideCharShouldReturnTrue() {
        val result = FileStorageUtils.containsBidiControlCharacters("safe\u202Enotsafe.exe")
        assertTrue(result)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenFilenameWithControlCharacterShouldReturnTrue() {
        val result = FileStorageUtils.containsBidiControlCharacters("hello\u0001world.txt")
        assertTrue(result)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenNullShouldReturnFalse() {
        val result = FileStorageUtils.containsBidiControlCharacters(null)
        assertFalse(result)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenValidFilenameShouldReturnTrue() {
        val result = FileStorageUtils.containsBidiControlCharacters("/Foo%e2%80%aedm.exe")
        assertTrue(result)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenMalformedEncodedSequenceShouldNotThrowAndReturnFalse() {
        val result = FileStorageUtils.containsBidiControlCharacters("file%")
        assertFalse(result)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenBrokenUrlEncodedPatternShouldHandleGracefully() {
        val result = FileStorageUtils.containsBidiControlCharacters("file%2")
        assertFalse(result)
    }

    @Test
    fun testContainsBidiControlCharactersWhenGivenMultipleBidiCharactersShouldReturnTrue() {
        val result = FileStorageUtils.containsBidiControlCharacters("safe\u202Ebad\u202Bname.txt")
        assertTrue(result)
    }

    @Test
    fun testIsValidExtFilenameRejectsDeleteCharacter() {
        assertFalse(FileStorageUtils.isValidExtFilename("file\u007Fname"))
    }

    @Test
    fun testIsValidExtFilenameAcceptsSpaceAndUnicode() {
        assertTrue(FileStorageUtils.isValidExtFilename("my file äöü 😀.txt"))
    }

    @Test
    fun testContainsBidiControlCharactersDetectsEveryBidiCharacter() {
        val bidiCharacters = listOf(
            '\u202A', '\u202B', '\u202C', '\u202D', '\u202E',
            '\u200E', '\u200F', '\u2066', '\u2067', '\u2068',
            '\u2069', '\u061C'
        )

        bidiCharacters.forEach {
            assertTrue("$it must be detected", FileStorageUtils.containsBidiControlCharacters("a${it}b.txt"))
        }
    }

    @Test
    fun testContainsBidiControlCharactersIgnoresSurrogatePairsAndDeleteCharacter() {
        assertFalse(FileStorageUtils.containsBidiControlCharacters("photo😀.jpg"))
        assertFalse(FileStorageUtils.containsBidiControlCharacters("file\u007F.txt"))
    }

    @Test
    fun testContainsBidiControlCharactersDetectsTab() {
        assertTrue(FileStorageUtils.containsBidiControlCharacters("file\tname.txt"))
    }

    @Test
    fun testInstantUploadPathWithYearRule() {
        val result = instantUploadPath(subFolderByDate = true, subFolderRule = SubFolderRule.YEAR)
        assertEquals("/Camera/2019/file.jpg", result)
    }

    @Test
    fun testInstantUploadPathWithYearMonthDayRule() {
        val result = instantUploadPath(subFolderByDate = true, subFolderRule = SubFolderRule.YEAR_MONTH_DAY)
        assertEquals("/Camera/2019/10/01/file.jpg", result)
    }

    @Test
    fun testInstantUploadPathWithoutRuleIgnoresDate() {
        val result = instantUploadPath(subFolderByDate = true, subFolderRule = null)
        assertEquals("/Camera/file.jpg", result)
    }

    @Test
    fun testInstantUploadPathCollapsesRepeatedSeparators() {
        val result = instantUploadPath(remotePath = "/Camera//", subFolderByDate = true)
        assertEquals("/Camera/2019/10/file.jpg", result)
    }

    @Test
    fun testInstantUploadPathWithoutSyncedFolderKeepsFullLocalPath() {
        assertEquals("/Camera/sdcard/DCIM/file.jpg", instantUploadPath(syncedFolderLocalPath = null))
        assertEquals("/Camera/sdcard/DCIM/file.jpg", instantUploadPath(syncedFolderLocalPath = ""))
    }

    @Test
    fun testGetParentPath() {
        assertEquals("/a/b/", FileStorageUtils.getParentPath("/a/b/c.txt"))
        assertEquals("/", FileStorageUtils.getParentPath("/c.txt"))
        assertNull(FileStorageUtils.getParentPath("c.txt"))
    }

    @Test
    fun testGetFolderSizeOfInvalidInputsIsZero() {
        val regularFile = temporaryFolder.newFile("regular.txt").apply { writeText("content") }

        assertEquals(0L, FileStorageUtils.getFolderSize(null))
        assertEquals(0L, FileStorageUtils.getFolderSize(File(temporaryFolder.root, "missing")))
        assertEquals(0L, FileStorageUtils.getFolderSize(regularFile))
    }

    @Test
    fun testGetFolderSizeSumsNestedFiles() {
        val root = temporaryFolder.newFolder("root")
        File(root, "a.txt").writeText("12345")
        File(root, "sub").mkdirs()
        File(root, "sub/b.txt").writeText("123")
        File(root, "sub/empty").mkdirs()

        assertEquals(8L, FileStorageUtils.getFolderSize(root))
    }

    @Test
    fun testCopyFileCopiesContent() {
        val content = ByteArray(LARGE_FILE_SIZE) { it.toByte() }
        val source = temporaryFolder.newFile("source.bin").apply { writeBytes(content) }
        val target = File(temporaryFolder.root, "target.bin")

        assertTrue(FileStorageUtils.copyFile(source, target))
        assertArrayEquals(content, target.readBytes())
        assertTrue(source.exists())
    }

    @Test
    fun testCopyFileReturnsFalseForMissingSource() {
        val target = File(temporaryFolder.root, "target.txt")

        assertFalse(FileStorageUtils.copyFile(File(temporaryFolder.root, "missing.txt"), target))
        assertFalse(target.exists())
    }

    @Test
    fun testMoveFileDeletesSource() {
        val source = temporaryFolder.newFile("source.txt").apply { writeText("content") }
        val target = File(temporaryFolder.root, "target.txt")

        assertTrue(FileStorageUtils.moveFile(source, target))
        assertFalse(source.exists())
        assertEquals("content", target.readText())
    }

    @Test
    fun testMoveFileReturnsFalseForMissingSource() {
        val target = File(temporaryFolder.root, "target.txt")

        assertFalse(FileStorageUtils.moveFile(File(temporaryFolder.root, "missing.txt"), target))
        assertFalse(target.exists())
    }

    @Test
    fun testCopyDirsCopiesNestedStructure() {
        val source = temporaryFolder.newFolder("source")
        File(source, "a.txt").writeText("a")
        File(source, "sub").mkdirs()
        File(source, "sub/b.txt").writeText("b")
        val target = File(temporaryFolder.root, "target")

        assertTrue(FileStorageUtils.copyDirs(source, target))
        assertEquals("a", File(target, "a.txt").readText())
        assertEquals("b", File(target, "sub/b.txt").readText())
    }

    @Test
    fun testCopyDirsReturnsFalseWhenTargetAlreadyExists() {
        val source = temporaryFolder.newFolder("source")
        val target = temporaryFolder.newFolder("target")

        assertFalse(FileStorageUtils.copyDirs(source, target))
    }

    @Test
    fun testDeleteRecursiveDeletesNestedStructure() {
        val root = temporaryFolder.newFolder("root")
        File(root, "sub/deeper").mkdirs()
        File(root, "a.txt").writeText("a")
        File(root, "sub/deeper/b.txt").writeText("b")

        assertTrue(FileStorageUtils.deleteRecursive(root))
        assertFalse(root.exists())
    }

    @Test
    fun testDeleteRecursiveReturnsFalseForMissingFile() {
        assertFalse(FileStorageUtils.deleteRecursive(File(temporaryFolder.root, "missing")))
    }

    @Test
    fun testCheckEncryptionStatusOfNullFileIsFalse() {
        assertFalse(FileStorageUtils.checkEncryptionStatus(null, mock()))
    }

    @Test
    fun testCheckEncryptionStatusOfEncryptedFileIsTrue() {
        val file = ocFile("/folder/file.txt", id = 2, parentId = 1).apply { isEncrypted = true }
        assertTrue(FileStorageUtils.checkEncryptionStatus(file, mock()))
    }

    @Test
    fun testCheckEncryptionStatusDetectsEncryptedAncestor() {
        val storageManager = mock<FileDataStorageManager>()
        val root = ocFile(OCFile.ROOT_PATH, id = 1, parentId = 0)
        val encryptedFolder = ocFile("/e2e/", id = 2, parentId = 1).apply { isEncrypted = true }
        val subFolder = ocFile("/e2e/sub/", id = 3, parentId = 2)
        val file = ocFile("/e2e/sub/file.txt", id = 4, parentId = 3)
        listOf(root, encryptedFolder, subFolder).forEach {
            whenever(storageManager.getFileById(it.fileId)).thenReturn(it)
        }

        assertTrue(FileStorageUtils.checkEncryptionStatus(file, storageManager))
    }

    @Test
    fun testCheckEncryptionStatusStopsAtRoot() {
        val storageManager = mock<FileDataStorageManager>()
        val root = ocFile(OCFile.ROOT_PATH, id = 1, parentId = 0).apply { isEncrypted = true }
        val file = ocFile("/file.txt", id = 2, parentId = 1)
        whenever(storageManager.getFileById(1)).thenReturn(root)

        assertFalse(FileStorageUtils.checkEncryptionStatus(file, storageManager))
    }

    @Test
    fun testCheckEncryptionStatusWithMissingParentIsFalse() {
        val file = ocFile("/folder/file.txt", id = 2, parentId = 1)
        assertFalse(FileStorageUtils.checkEncryptionStatus(file, mock()))
    }

    @Test
    fun testSortOcFolderDescDateModifiedWithoutFavoritesFirstIsStableAndDescending() {
        val oldest = ocFile("/oldest.txt", modificationTimestamp = 1)
        val firstOfTie = ocFile("/first.txt", modificationTimestamp = 2)
        val secondOfTie = ocFile("/second.txt", modificationTimestamp = 2).apply { isFavorite = true }
        val newest = ocFile("/newest.txt", modificationTimestamp = 3)

        val result = FileStorageUtils.sortOcFolderDescDateModifiedWithoutFavoritesFirst(
            listOf(oldest, firstOfTie, newest, secondOfTie)
        )

        assertEquals(listOf(newest, firstOfTie, secondOfTie, oldest), result)
    }

    @Test
    fun testSortOcFolderDescDateModifiedPutsFavoritesFirst() {
        val oldFavorite = ocFile("/favorite.txt", modificationTimestamp = 1).apply { isFavorite = true }
        val middle = ocFile("/middle.txt", modificationTimestamp = 2)
        val newest = ocFile("/newest.txt", modificationTimestamp = 3)

        val result = FileStorageUtils.sortOcFolderDescDateModified(mutableListOf(middle, oldFavorite, newest))

        assertEquals(listOf(oldFavorite, newest, middle), result)
    }

    @Test
    fun testCheckIfEnoughSpaceForFileRequiresMoreThanFileLength() {
        val file = ocFile("/file.txt").apply { fileLength = 100 }

        assertTrue(FileStorageUtils.checkIfEnoughSpace(101L, file))
        assertFalse(FileStorageUtils.checkIfEnoughSpace(100L, file))
    }

    @Test
    fun testCheckIfEnoughSpaceForFolderSubtractsLocalFolderSize() {
        val localFolder = temporaryFolder.newFolder("local")
        File(localFolder, "a.txt").writeBytes(ByteArray(LOCAL_FOLDER_CONTENT_SIZE))
        val notDownloadedFolder = ocFile("/folder/").apply {
            mimeType = MimeType.DIRECTORY
            fileLength = 100
        }
        val downloadedFolder = ocFile("/folder/").apply {
            mimeType = MimeType.DIRECTORY
            fileLength = 100
            storagePath = localFolder.absolutePath
        }

        assertFalse(FileStorageUtils.checkIfEnoughSpace(50L, notDownloadedFolder))
        assertTrue(FileStorageUtils.checkIfEnoughSpace(50L, downloadedFolder))
        assertFalse(FileStorageUtils.checkIfEnoughSpace(40L, downloadedFolder))
    }

    @Test
    fun testFillOCFileForFileUsesLengthAndIgnoresEncryption() {
        val remote = RemoteFile("/file.txt").apply {
            mimeType = "text/plain"
            length = 10
            size = 20
            isEncrypted = true
            etag = "etag"
            remoteId = "remoteId"
            modifiedTimestamp = 1234
            isFavorite = true
        }

        val result = FileStorageUtils.fillOCFile(remote)

        assertEquals("/file.txt", result.remotePath)
        assertEquals("/file.txt", result.decryptedRemotePath)
        assertEquals(10L, result.fileLength)
        assertFalse(result.isEncrypted)
        assertEquals("etag", result.etag)
        assertEquals("remoteId", result.remoteId)
        assertEquals(1234L, result.modificationTimestamp)
        assertTrue(result.isFavorite)
    }

    @Test
    fun testFillOCFileForFolderUsesSizeAndEncryption() {
        val remote = RemoteFile("/folder/").apply {
            mimeType = MimeType.DIRECTORY
            length = 10
            size = 20
            isEncrypted = true
        }

        val result = FileStorageUtils.fillOCFile(remote)

        assertEquals(20L, result.fileLength)
        assertTrue(result.isEncrypted)
    }

    @Test
    fun testFillRemoteFileCopiesFields() {
        val ocFile = ocFile("/file.txt", modificationTimestamp = 1234).apply {
            creationTimestamp = 1000
            fileLength = 42
            mimeType = "text/plain"
            etag = "etag"
            permissions = "RGDNVW"
            remoteId = "remoteId"
            isFavorite = true
        }

        val result = FileStorageUtils.fillRemoteFile(ocFile)

        assertEquals("/file.txt", result.remotePath)
        assertEquals(1000L, result.creationTimestamp)
        assertEquals(42L, result.length)
        assertEquals("text/plain", result.mimeType)
        assertEquals(1234L, result.modifiedTimestamp)
        assertEquals("etag", result.etag)
        assertEquals("RGDNVW", result.permissions)
        assertEquals("remoteId", result.remoteId)
        assertTrue(result.isFavorite)
    }

    @Test
    fun testPathToUserFriendlyDisplayWithoutPathOrContextReturnsPath() {
        assertEquals("", FileStorageUtils.pathToUserFriendlyDisplay(null, null, mock()))
        assertEquals("/storage/x", FileStorageUtils.pathToUserFriendlyDisplay("/storage/x", null, mock()))
    }

    @Suppress("LongParameterList")
    private fun instantUploadPath(
        remotePath: String = "/Camera",
        syncedFolderLocalPath: String? = "/sdcard/DCIM",
        subFolderByDate: Boolean = false,
        subFolderRule: SubFolderRule? = SubFolderRule.YEAR_MONTH
    ): String = FileStorageUtils.getInstantUploadFilePath(
        File("/sdcard/DCIM/file.jpg"),
        Locale.ROOT,
        remotePath,
        syncedFolderLocalPath,
        DATE_TAKEN_2019_10_01,
        subFolderByDate,
        subFolderRule
    )

    private fun ocFile(path: String, id: Long = 0, parentId: Long = 0, modificationTimestamp: Long = 0): OCFile =
        OCFile(path).apply {
            fileId = id
            this.parentId = parentId
            decryptedRemotePath = path
            this.modificationTimestamp = modificationTimestamp
        }

    companion object {
        private const val DATE_TAKEN_2019_10_01 = 1569918628000L
        private const val LARGE_FILE_SIZE = 20_000
        private const val LOCAL_FOLDER_CONTENT_SIZE = 60
    }
}
