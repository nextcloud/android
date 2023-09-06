package com.owncloud.android

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.operations.CreateFolderOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.operations.RenameFileOperation
import com.owncloud.android.operations.SynchronizeFolderOperation
import com.owncloud.android.operations.common.SyncOperation
import junit.framework.TestCase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/**
 * Tests related to file operations
 */
@RunWith(AndroidJUnit4::class)
class FileIT : AbstractOnServerIT() {
    @Test
    fun testCreateFolder() {
        val path = "/testFolder/"

        // folder does not exist yet
        Assert.assertNull(storageManager.getFileByPath(path))
        val syncOp: SyncOperation = CreateFolderOperation(path, user, targetContext, storageManager)
        val result = syncOp.execute(client)
        TestCase.assertTrue(result.toString(), result.isSuccess)

        // folder exists
        val file = storageManager.getFileByPath(path)
        TestCase.assertTrue(file.isFolder)

        // cleanup
        TestCase.assertTrue(
            RemoveFileOperation(file, false, user, false, targetContext, storageManager)
                .execute(client)
                .isSuccess
        )
    }

    @Test
    fun testCreateNonExistingSubFolder() {
        val path = "/subFolder/1/2/3/4/5/"
        // folder does not exist yet
        Assert.assertNull(storageManager.getFileByPath(path))
        val syncOp: SyncOperation = CreateFolderOperation(path, user, targetContext, storageManager)
        val result = syncOp.execute(client)
        TestCase.assertTrue(result.toString(), result.isSuccess)

        // folder exists
        val file = storageManager.getFileByPath(path)
        TestCase.assertTrue(file.isFolder)

        // cleanup
        RemoveFileOperation(
            file,
            false,
            user,
            false,
            targetContext,
            storageManager
        )
            .execute(client)
    }

    @Test
    fun testRemoteIdNull() {
        storageManager.deleteAllFiles()
        Assert.assertEquals(0, storageManager.allFiles.size.toLong())
        val test = OCFile("/123.txt")
        storageManager.saveFile(test)
        Assert.assertEquals(1, storageManager.allFiles.size.toLong())
        storageManager.deleteAllFiles()
        Assert.assertEquals(0, storageManager.allFiles.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testRenameFolder() {
        val folderPath = "/testRenameFolder/"

        // create folder
        createFolder(folderPath)

        // upload file inside it
        uploadFile(getDummyFile("nonEmpty.txt"), folderPath + "text.txt")

        // sync folder
        TestCase.assertTrue(
            SynchronizeFolderOperation(
                targetContext,
                folderPath,
                user,
                System.currentTimeMillis(),
                fileDataStorageManager
            )
                .execute(targetContext)
                .isSuccess
        )

        // check if file exists
        val storagePath1 = fileDataStorageManager.getFileByDecryptedRemotePath(folderPath)!!.storagePath
        TestCase.assertTrue(File(storagePath1).exists())
        val storagePath2 = fileDataStorageManager
            .getFileByDecryptedRemotePath(folderPath + "text.txt")!!
            .getStoragePath()
        TestCase.assertTrue(File(storagePath2).exists())
        shortSleep()

        // Rename
        TestCase.assertTrue(
            RenameFileOperation(folderPath, "test123", fileDataStorageManager)
                .execute(targetContext)
                .isSuccess
        )

        // after rename check new location
        TestCase.assertTrue(
            File(fileDataStorageManager.getFileByDecryptedRemotePath("/test123/")!!.storagePath)
                .exists()
        )
        TestCase.assertTrue(
            File(fileDataStorageManager.getFileByDecryptedRemotePath("/test123/text.txt")!!.storagePath)
                .exists()
        )

        // old files do no exist
        Assert.assertNull(fileDataStorageManager.getFileByDecryptedRemotePath(folderPath))
        Assert.assertNull(fileDataStorageManager.getFileByDecryptedRemotePath(folderPath + "text.txt"))

        // local files also do not exist
        Assert.assertFalse(File(storagePath1).exists())
        Assert.assertFalse(File(storagePath2).exists())
    }
}