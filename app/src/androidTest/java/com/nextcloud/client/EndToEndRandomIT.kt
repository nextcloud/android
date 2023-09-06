/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client

import android.accounts.AccountManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nextcloud.test.RandomStringGenerator.make
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.resources.users.DeletePrivateKeyOperation
import com.owncloud.android.lib.resources.users.DeletePublicKeyOperation
import com.owncloud.android.lib.resources.users.GetPrivateKeyOperation
import com.owncloud.android.lib.resources.users.GetPublicKeyOperation
import com.owncloud.android.lib.resources.users.SendCSROperation
import com.owncloud.android.lib.resources.users.StorePrivateKeyOperation
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.GetCapabilitiesOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.utils.CsrHelper
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.FileStorageUtils
import junit.framework.TestCase
import org.junit.Assume
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.security.interfaces.RSAPrivateCrtKey
import java.util.Random

@RunWith(AndroidJUnit4::class)
class EndToEndRandomIT : AbstractOnServerIT() {
    enum class Action {
        CREATE_FOLDER, GO_INTO_FOLDER, GO_UP, UPLOAD_FILE, DOWNLOAD_FILE, DELETE_FILE
    }

    private var currentFolder: OCFile? = null
    private val actionCount = 20
    private var rootEncFolder = "/e/"

    @Rule
    var retryTestRule = RetryTestRule()
    @Before
    @Throws(IOException::class)
    fun before() {
        val capability = storageManager.getCapability(account.name)
        if (capability.version == OwnCloudVersion("0.0.0")) {
            // fetch new one
            TestCase.assertTrue(
                GetCapabilitiesOperation(storageManager)
                    .execute(client)
                    .isSuccess
            )
        }
        // tests only for NC19+
        Assume.assumeTrue(
            storageManager
                .getCapability(account.name)
                .version
                .isNewerOrEqual(OwnCloudVersion.nextcloud_19)
        )

        // make sure that every file is available, even after tests that remove source file
        createDummyFiles()
    }

    @Test
    @Throws(Exception::class)
    fun run() {
        init()
        for (i in 0 until actionCount) {
            val nextAction = Action.values()[Random()
                .nextInt(Action.values().size)]
            when (nextAction) {
                Action.CREATE_FOLDER -> createFolder(i)
                Action.GO_INTO_FOLDER -> goIntoFolder(i)
                Action.GO_UP -> goUp(i)
                Action.UPLOAD_FILE -> uploadFile(i)
                Action.DOWNLOAD_FILE -> downloadFile(i)
                Action.DELETE_FILE -> deleteFile(i)
                else -> Log_OC.d(this, "[$i/$actionCount] Unknown action: $nextAction")
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun uploadOneFile() {
        init()
        uploadFile(0)
    }

    @Test
    @Throws(Exception::class)
    fun createFolder() {
        init()
        currentFolder = createFolder(0)
        TestCase.assertNotNull(currentFolder)
    }

    @Test
    @Throws(Exception::class)
    fun createSubFolders() {
        init()
        currentFolder = createFolder(0)
        TestCase.assertNotNull(currentFolder)
        currentFolder = createFolder(1)
        TestCase.assertNotNull(currentFolder)
        currentFolder = createFolder(2)
        TestCase.assertNotNull(currentFolder)
    }

    @Test
    @Throws(Exception::class)
    fun createSubFoldersWithFiles() {
        init()
        currentFolder = createFolder(0)
        TestCase.assertNotNull(currentFolder)
        uploadFile(1)
        uploadFile(1)
        uploadFile(2)
        currentFolder = createFolder(1)
        TestCase.assertNotNull(currentFolder)
        uploadFile(11)
        uploadFile(12)
        uploadFile(13)
        currentFolder = createFolder(2)
        TestCase.assertNotNull(currentFolder)
        uploadFile(21)
        uploadFile(22)
        uploadFile(23)
    }

    @Test
    @Throws(Exception::class)
    fun pseudoRandom() {
        init()
        uploadFile(1)
        createFolder(2)
        goIntoFolder(3)
        goUp(4)
        createFolder(5)
        uploadFile(6)
        goUp(7)
        goIntoFolder(8)
        goIntoFolder(9)
        uploadFile(10)
    }

    @Test
    @Throws(Exception::class)
    fun deleteFile() {
        init()
        uploadFile(1)
        deleteFile(1)
    }

    @Test
    @Throws(Exception::class)
    fun deleteFolder() {
        init()

        // create folder, go into it
        val createdFolder = createFolder(0)
        TestCase.assertNotNull(createdFolder)
        currentFolder = createdFolder
        uploadFile(1)
        goUp(1)

        // delete folder
        TestCase.assertTrue(
            RemoveFileOperation(
                createdFolder,
                false,
                user,
                false,
                targetContext,
                storageManager
            )
                .execute(client)
                .isSuccess
        )
    }

    @Test
    @Throws(Exception::class)
    fun downloadFile() {
        init()
        uploadFile(1)
        downloadFile(1)
    }

    @Throws(Exception::class)
    private fun init() {
        // create folder
        createFolder(rootEncFolder)
        val encFolder = createFolder(rootEncFolder + make(5) + "/")

        // encrypt it
        TestCase.assertTrue(
            ToggleEncryptionRemoteOperation(
                encFolder.localId,
                encFolder.remotePath,
                true
            )
                .execute(client).isSuccess
        )
        encFolder.isEncrypted = true
        storageManager.saveFolder(encFolder, ArrayList(), ArrayList())
        useExistingKeys()
        rootEncFolder = encFolder.decryptedRemotePath
        currentFolder = encFolder
    }

    private fun createFolder(i: Int): OCFile {
        val path = currentFolder!!.decryptedRemotePath + make(5) + "/"
        Log_OC.d(this, "[$i/$actionCount] Create folder: $path")
        return createFolder(path)
    }

    private fun goIntoFolder(i: Int) {
        val folders = ArrayList<OCFile>()
        for (file in storageManager.getFolderContent(currentFolder, false)) {
            if (file.isFolder) {
                folders.add(file)
            }
        }
        if (folders.isEmpty()) {
            Log_OC.d(this, "[$i/$actionCount] Go into folder: No folders")
            return
        }
        currentFolder = folders[Random().nextInt(folders.size)]
        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " + "Go into folder: " + currentFolder!!.decryptedRemotePath
        )
    }

    private fun goUp(i: Int) {
        if (currentFolder!!.remotePath == rootEncFolder) {
            Log_OC.d(
                this,
                "[" + i + "/" + actionCount + "] " + "Go up to folder: " + currentFolder!!.decryptedRemotePath
            )
            return
        }
        currentFolder = storageManager.getFileById(currentFolder!!.parentId)
        if (currentFolder == null) {
            throw RuntimeException("Current folder is null")
        }
        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " + "Go up to folder: " + currentFolder!!.decryptedRemotePath
        )
    }

    @Throws(IOException::class)
    private fun uploadFile(i: Int) {
        val fileName = make(5) + ".txt"
        val file: File
        file = if (Random().nextBoolean()) {
            createFile(fileName, Random().nextInt(50000))
        } else {
            createFile(fileName, 500000 + Random().nextInt(50000))
        }
        val remotePath = currentFolder!!.remotePath + fileName
        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " +
                "Upload file to: " + currentFolder!!.decryptedRemotePath + fileName
        )
        val ocUpload = OCUpload(
            file.absolutePath,
            remotePath,
            account.name
        )
        uploadOCUpload(ocUpload)
        shortSleep()
        val parentFolder = storageManager
            .getFileByEncryptedRemotePath(File(ocUpload.remotePath).parent + "/")
        val uploadedFileName = File(ocUpload.remotePath).name
        val decryptedPath = parentFolder.decryptedRemotePath + uploadedFileName
        var uploadedFile = storageManager.getFileByDecryptedRemotePath(decryptedPath)
        verifyStoragePath(uploadedFile)

        // verify storage path
        refreshFolder(currentFolder!!.remotePath)
        uploadedFile = storageManager.getFileByDecryptedRemotePath(decryptedPath)
        verifyStoragePath(uploadedFile)

        // verify that encrypted file is on server
        TestCase.assertTrue(
            ReadFileRemoteOperation(currentFolder!!.remotePath + uploadedFile!!.encryptedFileName)
                .execute(client)
                .isSuccess
        )

        // verify that unencrypted file is not on server
        TestCase.assertFalse(
            ReadFileRemoteOperation(currentFolder!!.decryptedRemotePath + fileName)
                .execute(client)
                .isSuccess
        )
    }

    private fun downloadFile(i: Int) {
        val files = ArrayList<OCFile>()
        for (file in storageManager.getFolderContent(currentFolder, false)) {
            if (!file.isFolder) {
                files.add(file)
            }
        }
        if (files.isEmpty()) {
            Log_OC.d(this, "[" + i + "/" + actionCount + "] No files in: " + currentFolder!!.decryptedRemotePath)
            return
        }
        val fileToDownload = files[Random().nextInt(files.size)]
        TestCase.assertNotNull(fileToDownload.remoteId)
        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " + "Download file: " +
                currentFolder!!.decryptedRemotePath + fileToDownload.decryptedFileName
        )
        TestCase.assertTrue(
            DownloadFileOperation(user, fileToDownload, targetContext)
                .execute(client)
                .isSuccess
        )
        TestCase.assertTrue(File(fileToDownload.storagePath).exists())
        verifyStoragePath(fileToDownload)
    }

    @Test
    @Throws(Exception::class)
    fun testUploadWithCopy() {
        init()
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            currentFolder!!.remotePath + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_COPY)
        val originalFile = File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt")
        val uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(
            currentFolder!!.remotePath +
                "nonEmpty.txt"
        )
        TestCase.assertTrue(originalFile.exists())
        TestCase.assertTrue(File(uploadedFile!!.storagePath).exists())
    }

    @Test
    @Throws(Exception::class)
    fun testUploadWithMove() {
        init()
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            currentFolder!!.remotePath + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_MOVE)
        val originalFile = File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt")
        val uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(
            currentFolder!!.remotePath +
                "nonEmpty.txt"
        )
        TestCase.assertFalse(originalFile.exists())
        TestCase.assertTrue(File(uploadedFile!!.storagePath).exists())
    }

    @Test
    @Throws(Exception::class)
    fun testUploadWithForget() {
        init()
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            currentFolder!!.remotePath + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_FORGET)
        val originalFile = File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt")
        val uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(
            currentFolder!!.remotePath +
                "nonEmpty.txt"
        )
        TestCase.assertTrue(originalFile.exists())
        TestCase.assertFalse(File(uploadedFile!!.storagePath).exists())
    }

    @Test
    @Throws(Exception::class)
    fun testUploadWithDelete() {
        init()
        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
            currentFolder!!.remotePath + "nonEmpty.txt",
            account.name
        )
        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_DELETE)
        val originalFile = File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt")
        val uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(
            currentFolder!!.remotePath +
                "nonEmpty.txt"
        )
        TestCase.assertFalse(originalFile.exists())
        TestCase.assertFalse(File(uploadedFile!!.storagePath).exists())
    }

    @Test
    @Throws(Exception::class)
    fun testCheckCSR() {
        deleteKeys()

        // Create public/private key pair
        val keyPair = EncryptionUtils.generateKeyPair()

        // create CSR
        val accountManager = AccountManager.get(targetContext)
        val userId = accountManager.getUserData(account, AccountUtils.Constants.KEY_USER_ID)
        val urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, userId)
        val operation = SendCSROperation(urlEncoded)
        val result = operation.execute(account, targetContext)
        TestCase.assertTrue(result.isSuccess)
        val publicKeyString = result.data[0] as String

        // check key
        val privateKey = keyPair.private as RSAPrivateCrtKey
        val publicKey = EncryptionUtils.convertPublicKeyFromString(publicKeyString)
        val modulusPublic = publicKey.modulus
        val modulusPrivate = privateKey.modulus
        TestCase.assertEquals(modulusPrivate, modulusPublic)
        createKeys()
    }

    private fun deleteFile(i: Int) {
        val files = ArrayList<OCFile>()
        for (file in storageManager.getFolderContent(currentFolder, false)) {
            if (!file.isFolder) {
                files.add(file)
            }
        }
        if (files.isEmpty()) {
            Log_OC.d(this, "[" + i + "/" + actionCount + "] No files in: " + currentFolder!!.decryptedRemotePath)
            return
        }
        val fileToDelete = files[Random().nextInt(files.size)]
        TestCase.assertNotNull(fileToDelete.remoteId)
        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " +
                "Delete file: " + currentFolder!!.decryptedRemotePath + fileToDelete.decryptedFileName
        )
        TestCase.assertTrue(
            RemoveFileOperation(
                fileToDelete,
                false,
                user,
                false,
                targetContext,
                storageManager
            )
                .execute(client)
                .isSuccess
        )
    }

    @Test
    @Throws(Exception::class)
    fun reInit() {
        // create folder
        val encFolder = createFolder(rootEncFolder)

        // encrypt it
        TestCase.assertTrue(
            ToggleEncryptionRemoteOperation(
                encFolder.localId,
                encFolder.remotePath,
                true
            )
                .execute(client).isSuccess
        )
        encFolder.isEncrypted = true
        storageManager.saveFolder(encFolder, ArrayList(), ArrayList())

        // delete keys
        arbitraryDataProvider!!.deleteKeyForAccount(account.name, EncryptionUtils.PRIVATE_KEY)
        arbitraryDataProvider!!.deleteKeyForAccount(account.name, EncryptionUtils.PUBLIC_KEY)
        arbitraryDataProvider!!.deleteKeyForAccount(account.name, EncryptionUtils.MNEMONIC)
        useExistingKeys()
    }

    @Throws(Exception::class)
    private fun useExistingKeys() {
        // download them from server
        val publicKeyOperation = GetPublicKeyOperation()
        val publicKeyResult = publicKeyOperation.execute(account, targetContext)
        TestCase.assertTrue("Result code:" + publicKeyResult.httpCode, publicKeyResult.isSuccess)
        val publicKeyFromServer = publicKeyResult.resultData
        arbitraryDataProvider!!.storeOrUpdateKeyValue(
            account.name,
            EncryptionUtils.PUBLIC_KEY,
            publicKeyFromServer
        )
        val privateKeyResult = GetPrivateKeyOperation().execute(
            account,
            targetContext
        )
        TestCase.assertTrue(privateKeyResult.isSuccess)
        val privateKey = privateKeyResult.resultData
        val mnemonic = generateMnemonicString()
        val decryptedPrivateKey = EncryptionUtils.decryptPrivateKey(privateKey.getKey(), mnemonic)
        arbitraryDataProvider!!.storeOrUpdateKeyValue(
            account.name,
            EncryptionUtils.PRIVATE_KEY, decryptedPrivateKey
        )
        Log_OC.d(this, "Private key successfully decrypted and stored")
        arbitraryDataProvider!!.storeOrUpdateKeyValue(account.name, EncryptionUtils.MNEMONIC, mnemonic)
    }

    override fun after() {
        // remove all encrypted files
        val root = fileDataStorageManager.getFileByDecryptedRemotePath("/")
        removeFolder(root)

//        List<OCFile> files = fileDataStorageManager.getFolderContent(root, false);
//
//        for (OCFile child : files) {
//            removeFolder(child);
//        }
        TestCase.assertEquals(0, fileDataStorageManager.getFolderContent(root, false).size)
        super.after()
    }

    private fun removeFolder(folder: OCFile?) {
        Log_OC.d(this, "Start removing content of folder: " + folder!!.decryptedRemotePath)
        val children = fileDataStorageManager.getFolderContent(folder, false)

        // remove children
        for (child in children) {
            if (child.isFolder) {
                removeFolder(child)

                // remove folder
                Log_OC.d(this, "Remove folder: " + child.decryptedRemotePath)
                if (!folder.isEncrypted && child.isEncrypted) {
                    TestCase.assertTrue(
                        ToggleEncryptionRemoteOperation(
                            child.localId,
                            child.remotePath,
                            false
                        )
                            .execute(client)
                            .isSuccess
                    )
                    val f = storageManager.getFileByEncryptedRemotePath(child.remotePath)
                    f.isEncrypted = false
                    storageManager.saveFile(f)
                    child.isEncrypted = false
                }
            } else {
                Log_OC.d(this, "Remove file: " + child.decryptedRemotePath)
            }
            TestCase.assertTrue(
                RemoveFileOperation(child, false, user, false, targetContext, storageManager)
                    .execute(client)
                    .isSuccess
            )
        }
        Log_OC.d(this, "Finished removing content of folder: " + folder.decryptedRemotePath)
    }

    private fun verifyStoragePath(file: OCFile?) {
        TestCase.assertEquals(
            FileStorageUtils.getSavePath(account.name) +
                currentFolder!!.decryptedRemotePath +
                file!!.decryptedFileName,
            file.storagePath
        )
    }

    companion object {
        private var arbitraryDataProvider: ArbitraryDataProvider? = null
        @BeforeClass
        @Throws(Exception::class)
        fun initClass() {
            arbitraryDataProvider = ArbitraryDataProviderImpl(targetContext)
            createKeys()
        }

        /*
    TODO do not c&p code
     */
        @Throws(Exception::class)
        private fun createKeys() {
            deleteKeys()
            val publicKeyString: String

            // Create public/private key pair
            val keyPair = EncryptionUtils.generateKeyPair()

            // create CSR
            val accountManager = AccountManager.get(targetContext)
            val userId = accountManager.getUserData(account, AccountUtils.Constants.KEY_USER_ID)
            val urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, userId)
            val operation = SendCSROperation(urlEncoded)
            val result = operation.execute(account, targetContext)
            if (result.isSuccess) {
                publicKeyString = result.data[0] as String

                // check key
                val privateKey = keyPair.private as RSAPrivateCrtKey
                val publicKey = EncryptionUtils.convertPublicKeyFromString(publicKeyString)
                val modulusPublic = publicKey.modulus
                val modulusPrivate = privateKey.modulus
                if (modulusPrivate.compareTo(modulusPublic) != 0) {
                    throw RuntimeException("Wrong CSR returned")
                }
            } else {
                throw Exception("failed to send CSR", result.exception)
            }
            val privateKey = keyPair.private
            val privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKey.encoded)
            val privatePemKeyString = EncryptionUtils.privateKeyToPEM(privateKey)
            val encryptedPrivateKey = EncryptionUtils.encryptPrivateKey(
                privatePemKeyString,
                generateMnemonicString()
            )

            // upload encryptedPrivateKey
            val storePrivateKeyOperation = StorePrivateKeyOperation(encryptedPrivateKey)
            val storePrivateKeyResult = storePrivateKeyOperation.execute(account, targetContext)
            if (storePrivateKeyResult.isSuccess) {
                arbitraryDataProvider!!.storeOrUpdateKeyValue(
                    account.name, EncryptionUtils.PRIVATE_KEY,
                    privateKeyString
                )
                arbitraryDataProvider!!.storeOrUpdateKeyValue(account.name, EncryptionUtils.PUBLIC_KEY, publicKeyString)
                arbitraryDataProvider!!.storeOrUpdateKeyValue(
                    account.name, EncryptionUtils.MNEMONIC,
                    generateMnemonicString()
                )
            } else {
                throw RuntimeException("Error uploading private key!")
            }
        }

        private fun deleteKeys() {
            val privateKeyRemoteOperationResult = GetPrivateKeyOperation().execute(client)
            val publicKeyRemoteOperationResult = GetPublicKeyOperation().execute(client)
            if (privateKeyRemoteOperationResult.isSuccess || publicKeyRemoteOperationResult.isSuccess) {
                // delete keys
                TestCase.assertTrue(DeletePrivateKeyOperation().execute(client).isSuccess)
                TestCase.assertTrue(DeletePublicKeyOperation().execute(client).isSuccess)
                arbitraryDataProvider!!.deleteKeyForAccount(account.name, EncryptionUtils.PRIVATE_KEY)
                arbitraryDataProvider!!.deleteKeyForAccount(account.name, EncryptionUtils.PUBLIC_KEY)
                arbitraryDataProvider!!.deleteKeyForAccount(account.name, EncryptionUtils.MNEMONIC)
            }
        }

        private fun generateMnemonicString(): String {
            return "1 2 3 4 5 6"
        }
    }
}