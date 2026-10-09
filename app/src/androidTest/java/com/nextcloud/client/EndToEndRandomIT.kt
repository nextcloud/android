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
import android.util.Pair
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.test.RandomStringGenerator
import com.nextcloud.test.RetryTestRule
import com.owncloud.android.AbstractIT
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.datamodel.e2e.v2.encrypted.EncryptedFolderMetadataFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.ocs.responses.PrivateKey
import com.owncloud.android.lib.resources.e2ee.CsrHelper
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.lib.resources.users.DeletePrivateKeyRemoteOperation
import com.owncloud.android.lib.resources.users.DeletePublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.GetPrivateKeyRemoteOperation
import com.owncloud.android.lib.resources.users.GetPublicKeyRemoteOperation
import com.owncloud.android.lib.resources.users.SendCSRRemoteOperation
import com.owncloud.android.lib.resources.users.StorePrivateKeyRemoteOperation
import com.owncloud.android.operations.CreateShareWithShareeOperation
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.operations.GetCapabilitiesOperation
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import com.owncloud.android.utils.FileStorageUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.IOException
import java.security.KeyPair
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.util.Random

class EndToEndRandomIT : AbstractOnServerIT() {
    private var currentFolder: OCFile? = null
    private val actionCount = 20
    private var rootEncFolder = "/e/"

    @JvmField
    @Rule
    var retryTestRule = RetryTestRule()

    @Before
    @Throws(IOException::class)
    fun before() {
        val capability: OCCapability = storageManager.getCapability(AbstractIT.account.name)

        if (capability.version == OwnCloudVersion("0.0.0")) {
            // fetch new one
            assertTrue(
                GetCapabilitiesOperation(storageManager)
                    .execute(AbstractIT.client)
                    .isSuccess
            )
        }

        // tests only for NC19+
        Assume.assumeTrue(
            storageManager
                .getCapability(AbstractIT.account.name)
                .version
                .isNewerOrEqual(OwnCloudVersion.nextcloud_19)
        )

        // make sure that every file is available, even after tests that remove source file
        AbstractIT.createDummyFiles()
    }

    @Test
    @Throws(Throwable::class)
    fun run() {
        init()

        for (i in 0 until actionCount) {
            when (EndToEndAction.entries[Random().nextInt(EndToEndAction.entries.size)]) {
                EndToEndAction.CREATE_FOLDER -> createFolder(i)
                EndToEndAction.GO_INTO_FOLDER -> goIntoFolder(i)
                EndToEndAction.GO_UP -> goUp(i)
                EndToEndAction.UPLOAD_FILE -> uploadFile(i)
                EndToEndAction.DOWNLOAD_FILE -> downloadFile(i)
                EndToEndAction.DELETE_FILE -> deleteFile(i)
            }
        }
    }

    @Test
    @Throws(Throwable::class)
    fun uploadOneFile() {
        init()

        uploadFile(0)
    }

    @Test
    @Throws(Throwable::class)
    fun createFolder() {
        init()

        currentFolder = createFolder(0)
        assertNotNull(currentFolder)
    }

    @Test
    @Throws(Throwable::class)
    fun createSubFolders() {
        init()

        currentFolder = createFolder(0)
        assertNotNull(currentFolder)

        currentFolder = createFolder(1)
        assertNotNull(currentFolder)

        currentFolder = createFolder(2)
        assertNotNull(currentFolder)
    }

    @Test
    @Throws(Throwable::class)
    fun createSubFoldersWithFiles() {
        init()

        currentFolder = createFolder(0)
        assertNotNull(currentFolder)

        uploadFile(1)
        uploadFile(1)
        uploadFile(2)

        currentFolder = createFolder(1)
        assertNotNull(currentFolder)
        uploadFile(11)
        uploadFile(12)
        uploadFile(13)

        currentFolder = createFolder(2)
        assertNotNull(currentFolder)

        uploadFile(21)
        uploadFile(22)
        uploadFile(23)
    }

    @Test
    @Throws(Throwable::class)
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
    @Throws(Throwable::class)
    fun deleteFile() {
        init()

        uploadFile(1)
        deleteFile(1)
    }

    @Test
    @Throws(Throwable::class)
    fun deleteFolder() {
        init()

        // create folder, go into it
        val createdFolder: OCFile = createFolder(0)
        assertNotNull(createdFolder)
        currentFolder = createdFolder

        uploadFile(1)
        goUp(1)

        // delete folder
        assertTrue(
            RemoveFileOperation(
                createdFolder,
                false,
                user,
                false,
                targetContext,
                storageManager
            )
                .execute(AbstractIT.client)
                .isSuccess
        )
    }

    @Test
    @Throws(Throwable::class)
    fun downloadFile() {
        init()

        uploadFile(1)
        downloadFile(1)
    }

    @Throws(Throwable::class)
    private fun init() {
        // create folder
        createFolder(rootEncFolder)
        val encFolder: OCFile = createFolder(rootEncFolder + RandomStringGenerator.make(5) + "/")

        // encrypt it
        assertTrue(
            ToggleEncryptionRemoteOperation(
                encFolder.localId,
                encFolder.remotePath,
                true
            )
                .execute(AbstractIT.client).isSuccess
        )
        encFolder.isEncrypted = true
        storageManager.saveFolder(encFolder, ArrayList<OCFile>(), ArrayList<OCFile>())

        useExistingKeys()

        // lock folder
        val token: String = EncryptionUtils.lockFolder(encFolder, AbstractIT.client)

        val ocCapability: OCCapability = storageManager.getCapability(AbstractIT.user.accountName)

        when (ocCapability.endToEndEncryptionApiVersion) {
            E2EVersion.V2_0 -> {
                // Update metadata
                val privateKeyString: String =
                    arbitraryDataProvider.getValue(AbstractIT.account.name, EncryptionUtils.PRIVATE_KEY)
                val publicKeyString: String =
                    arbitraryDataProvider.getValue(AbstractIT.account.name, EncryptionUtils.PUBLIC_KEY)

                val metadataPair: Pair<Boolean, DecryptedFolderMetadataFile> = EncryptionUtils.retrieveMetadata(
                    encFolder,
                    AbstractIT.client,
                    privateKeyString,
                    publicKeyString,
                    storageManager,
                    AbstractIT.user,
                    AbstractIT.targetContext,
                    arbitraryDataProvider
                )

                val metadataExists = metadataPair.first
                val metadata: DecryptedFolderMetadataFile = metadataPair.second

                EncryptionUtilsV2().serializeAndUploadMetadata(
                    encFolder,
                    metadata,
                    token,
                    AbstractIT.client,
                    metadataExists,
                    AbstractIT.targetContext,
                    AbstractIT.user,
                    storageManager
                )

                // unlock folder
                EncryptionUtils.unlockFolder(encFolder, AbstractIT.client, token)
            }

            E2EVersion.V1_0, E2EVersion.V1_1, E2EVersion.V1_2 -> {
                // unlock folder
                EncryptionUtils.unlockFolderV1(encFolder, AbstractIT.client, token)
            }

            else -> require(ocCapability.endToEndEncryptionApiVersion != E2EVersion.UNKNOWN) { "Unknown E2E version" }
        }

        rootEncFolder = encFolder.decryptedRemotePath
        currentFolder = encFolder
    }

    private fun createFolder(i: Int): OCFile {
        val path: String = currentFolder?.decryptedRemotePath + RandomStringGenerator.make(5) + "/"
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
            "[" + i + "/" + actionCount + "] " + "Go into folder: " + currentFolder?.decryptedRemotePath
        )
    }

    private fun goUp(i: Int) {
        if (currentFolder?.remotePath == rootEncFolder) {
            Log_OC.d(
                this,
                "[" + i + "/" + actionCount + "] " + "Go up to folder: " + currentFolder?.decryptedRemotePath
            )
            return
        }

        currentFolder = currentFolder?.parentId?.let { storageManager.getFileById(it) }
        if (currentFolder == null) {
            throw RuntimeException("Current folder is null")
        }

        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " + "Go up to folder: " + currentFolder?.decryptedRemotePath
        )
    }

    @Throws(IOException::class)
    private fun uploadFile(i: Int) {
        val fileName: String = RandomStringGenerator.make(5) + ".txt"
        val file: File = if (Random().nextBoolean()) {
            AbstractIT.createFile(fileName, Random().nextInt(50000))
        } else {
            AbstractIT.createFile(fileName, 500000 + Random().nextInt(50000))
        }

        val remotePath: String = currentFolder?.remotePath + fileName

        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " +
                "Upload file to: " + currentFolder?.decryptedRemotePath + fileName
        )

        val ocUpload = OCUpload(
            file.absolutePath,
            remotePath,
            AbstractIT.account.name
        )
        uploadOCUpload(ocUpload)
        AbstractIT.shortSleep()

        val parentFolder: OCFile =
            storageManager.getFileByEncryptedRemotePath(File(ocUpload.remotePath).getParent() + "/")
        val uploadedFileName: String = File(ocUpload.remotePath).getName()

        val decryptedPath: String = parentFolder.decryptedRemotePath + uploadedFileName

        var uploadedFile: OCFile? = storageManager.getFileByDecryptedRemotePath(decryptedPath)
        if (uploadedFile != null) {
            verifyStoragePath(uploadedFile)
        }

        // verify storage path
        refreshFolder(currentFolder?.remotePath)
        uploadedFile = storageManager.getFileByDecryptedRemotePath(decryptedPath)
        if (uploadedFile != null) {
            verifyStoragePath(uploadedFile)
        }

        // verify that encrypted file is on server
        assertTrue(
            ReadFileRemoteOperation(currentFolder?.remotePath + uploadedFile?.getEncryptedFileName())
                .execute(AbstractIT.client)
                .isSuccess
        )

        // verify that unencrypted file is not on server
        assertFalse(
            ReadFileRemoteOperation(currentFolder?.decryptedRemotePath + fileName)
                .execute(AbstractIT.client)
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
            Log_OC.d(this, "[" + i + "/" + actionCount + "] No files in: " + currentFolder?.decryptedRemotePath)
            return
        }

        val fileToDownload: OCFile = files[Random().nextInt(files.size)]
        assertNotNull(fileToDownload.remoteId)

        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " + "Download file: " +
                currentFolder?.decryptedRemotePath + fileToDownload.decryptedFileName
        )

        assertTrue(
            DownloadFileOperation(user, fileToDownload, targetContext)
                .execute(AbstractIT.client)
                .isSuccess
        )

        assertTrue(File(fileToDownload.storagePath).exists())
        verifyStoragePath(fileToDownload)
    }

    @Test
    @Throws(Throwable::class)
    fun testUploadWithCopy() {
        init()

        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(AbstractIT.account.name) + "/nonEmpty.txt",
            currentFolder?.remotePath + "nonEmpty.txt",
            AbstractIT.account.name
        )

        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_COPY)

        val originalFile = File(FileStorageUtils.getTemporalPath(AbstractIT.account.name) + "/nonEmpty.txt")
        val uploadedFile: OCFile? = fileDataStorageManager.getFileByDecryptedRemotePath(
            currentFolder?.remotePath +
                "nonEmpty.txt"
        )

        assertTrue(originalFile.exists())
        assertTrue(File(uploadedFile?.storagePath).exists())
    }

    @Test
    @Throws(Throwable::class)
    fun testUploadWithMove() {
        init()

        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(AbstractIT.account.name) + "/nonEmpty.txt",
            currentFolder?.remotePath + "nonEmpty.txt",
            AbstractIT.account.name
        )

        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_MOVE)

        val originalFile = File(FileStorageUtils.getTemporalPath(AbstractIT.account.name) + "/nonEmpty.txt")
        val uploadedFile: OCFile? = fileDataStorageManager.getFileByDecryptedRemotePath(
            currentFolder?.remotePath +
                "nonEmpty.txt"
        )

        assertFalse(originalFile.exists())
        assertTrue(File(uploadedFile?.storagePath).exists())
    }

    @Test
    @Throws(Throwable::class)
    fun testUploadWithForget() {
        init()

        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(AbstractIT.account.name) + "/nonEmpty.txt",
            currentFolder?.remotePath + "nonEmpty.txt",
            AbstractIT.account.name
        )

        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_FORGET)

        val originalFile = File(FileStorageUtils.getTemporalPath(AbstractIT.account.name) + "/nonEmpty.txt")
        val uploadedFile: OCFile? = fileDataStorageManager.getFileByDecryptedRemotePath(
            currentFolder?.remotePath +
                "nonEmpty.txt"
        )

        assertTrue(originalFile.exists())
        assertFalse(File(uploadedFile?.storagePath).exists())
    }

    @Test
    @Throws(Throwable::class)
    fun testUploadWithDelete() {
        init()

        val ocUpload = OCUpload(
            FileStorageUtils.getTemporalPath(AbstractIT.account.name) + "/nonEmpty.txt",
            currentFolder?.remotePath + "nonEmpty.txt",
            AbstractIT.account.name
        )

        uploadOCUpload(ocUpload, FileUploadWorker.LOCAL_BEHAVIOUR_DELETE)

        val originalFile = File(FileStorageUtils.getTemporalPath(AbstractIT.account.name) + "/nonEmpty.txt")
        val uploadedFile: OCFile? = fileDataStorageManager.getFileByDecryptedRemotePath(
            currentFolder?.remotePath +
                "nonEmpty.txt"
        )

        assertFalse(originalFile.exists())
        assertFalse(File(uploadedFile?.storagePath).exists())
    }

    @Test
    @Throws(Exception::class)
    fun testCheckCSR() {
        deleteKeys()

        // Create public/private key pair
        val keyPair: KeyPair = EncryptionUtils.generateKeyPair()

        // create CSR
        val accountManager: AccountManager = AccountManager.get(AbstractIT.targetContext)
        val userId: String = accountManager.getUserData(AbstractIT.account, AccountUtils.Constants.KEY_USER_ID)
        val urlEncoded: String = CsrHelper().generateCsrPemEncodedString(keyPair, userId)

        val operation = SendCSRRemoteOperation(urlEncoded)
        val result: RemoteOperationResult<String> =
            operation.executeNextcloudClient(AbstractIT.account, AbstractIT.targetContext)

        assertTrue(result.isSuccess)
        val publicKeyString: String = result.resultData

        // check key
        val privateKey = keyPair.private as RSAPrivateCrtKey
        val publicKey: RSAPublicKey = EncryptionUtils.convertPublicKeyFromString(publicKeyString)

        val modulusPublic = publicKey.modulus
        val modulusPrivate = privateKey.modulus

        assertEquals(modulusPrivate, modulusPublic)

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
            Log_OC.d(this, "[" + i + "/" + actionCount + "] No files in: " + currentFolder?.decryptedRemotePath)
            return
        }

        val fileToDelete: OCFile = files[Random().nextInt(files.size)]
        assertNotNull(fileToDelete.remoteId)

        Log_OC.d(
            this,
            "[" + i + "/" + actionCount + "] " +
                "Delete file: " + currentFolder?.decryptedRemotePath + fileToDelete.decryptedFileName
        )

        assertTrue(
            RemoveFileOperation(
                fileToDelete,
                false,
                AbstractIT.user,
                false,
                AbstractIT.targetContext,
                storageManager
            ).execute(AbstractIT.client).isSuccess
        )
    }

    @Test
    @Throws(Exception::class)
    fun reInit() {
        // create folder
        val encFolder: OCFile = createFolder(rootEncFolder)

        // encrypt it
        assertTrue(
            ToggleEncryptionRemoteOperation(
                encFolder.localId,
                encFolder.remotePath,
                true
            )
                .execute(AbstractIT.client).isSuccess
        )
        encFolder.isEncrypted = true
        storageManager.saveFolder(encFolder, ArrayList<OCFile>(), ArrayList<OCFile>())

        // delete keys
        arbitraryDataProvider.deleteKeyForAccount(AbstractIT.account.name, EncryptionUtils.PRIVATE_KEY)
        arbitraryDataProvider.deleteKeyForAccount(AbstractIT.account.name, EncryptionUtils.PUBLIC_KEY)
        arbitraryDataProvider.deleteKeyForAccount(AbstractIT.account.name, EncryptionUtils.MNEMONIC)

        useExistingKeys()
    }

    @Test
    @Throws(Throwable::class)
    fun shareFolder() {
        init()

        val `object`: Any? = EncryptionUtils.downloadFolderMetadata(
            currentFolder,
            AbstractIT.client,
            AbstractIT.targetContext,
            AbstractIT.user
        )

        // metadata does not yet exist
        assertNull(`object`)

        assertTrue(
            CreateShareWithShareeOperation(
                currentFolder?.remotePath,
                "e2e",
                ShareType.USER,
                OCShare.SHARE_PERMISSION_FLAG,
                "",
                "",
                -1,
                false,
                fileDataStorageManager,
                targetContext,
                user,
                ArbitraryDataProviderImpl(targetContext)
            )
                .execute(AbstractIT.client)
                .isSuccess
        )

        // verify
        val newObject: Any? = EncryptionUtils.downloadFolderMetadata(
            currentFolder,
            AbstractIT.client,
            AbstractIT.targetContext,
            AbstractIT.user
        )

        assertTrue(newObject is EncryptedFolderMetadataFile)

        assertEquals(2, (newObject as EncryptedFolderMetadataFile).users.size)
    }

    //    @Test
    //    public void testRemoveFiles() throws Exception {
    //        init();
    //        createFolder();
    //        goIntoFolder(1);
    //
    //        OCFile root = fileDataStorageManager.getFileByDecryptedRemotePath("/");
    //        removeFolder(root);
    //    }
    @Throws(Exception::class)
    private fun useExistingKeys() {
        // download them from server
        val publicKeyOperation = GetPublicKeyRemoteOperation()
        val publicKeyResult: RemoteOperationResult<String> = publicKeyOperation.executeNextcloudClient(
            AbstractIT.account,
            AbstractIT.targetContext
        )

        assertTrue("Result code:" + publicKeyResult.httpCode, publicKeyResult.isSuccess)

        val publicKeyFromServer: String = publicKeyResult.resultData
        arbitraryDataProvider.storeOrUpdateKeyValue(
            AbstractIT.account.name,
            EncryptionUtils.PUBLIC_KEY,
            publicKeyFromServer
        )

        val privateKeyResult: RemoteOperationResult<PrivateKey> = GetPrivateKeyRemoteOperation()
            .executeNextcloudClient(AbstractIT.account, AbstractIT.targetContext)
        assertTrue(privateKeyResult.isSuccess)

        val privateKey: PrivateKey = privateKeyResult.resultData

        val mnemonic = generateMnemonicString()
        val decryptedPrivateKey: String = EncryptionUtils.decryptPrivateKey(privateKey.getKey(), mnemonic)

        arbitraryDataProvider.storeOrUpdateKeyValue(
            AbstractIT.account.name,
            EncryptionUtils.PRIVATE_KEY, decryptedPrivateKey
        )

        Log_OC.d(this, "Private key successfully decrypted and stored")

        arbitraryDataProvider.storeOrUpdateKeyValue(AbstractIT.account.name, EncryptionUtils.MNEMONIC, mnemonic)
    }

    override fun after() {
        // remove all encrypted files
//        OCFile root = fileDataStorageManager.getFileByDecryptedRemotePath("/");
        // removeFolder(root);

//        List<OCFile> files = fileDataStorageManager.getFolderContent(root, false);
//
//        for (OCFile child : files) {
//            removeFolder(child);
//        }

//        assertEquals(0, fileDataStorageManager.getFolderContent(root, false).size());

//        super.after();
    }

    private fun removeFolder(folder: OCFile) {
        Log_OC.d(this, "Start removing content of folder: " + folder.decryptedRemotePath)

        val children: List<OCFile> = fileDataStorageManager.getFolderContent(folder, false)

        // remove children
        for (child in children) {
            if (child.isFolder) {
                removeFolder(child)

                // remove folder
                Log_OC.d(this, "Remove folder: " + child.decryptedRemotePath)
                if (!folder.isEncrypted && child.isEncrypted) {
                    val result: RemoteOperationResult<*> = ToggleEncryptionRemoteOperation(
                        child.localId,
                        child.remotePath,
                        false
                    )
                        .execute(AbstractIT.client)
                    assertTrue(result.logMessage, result.isSuccess)

                    val f: OCFile = storageManager.getFileByEncryptedRemotePath(child.remotePath)
                    f.isEncrypted = false
                    storageManager.saveFile(f)

                    child.isEncrypted = false
                }
            } else {
                Log_OC.d(this, "Remove file: " + child.decryptedRemotePath)
            }

            assertTrue(
                RemoveFileOperation(child, false, AbstractIT.user, false, AbstractIT.targetContext, storageManager)
                    .execute(AbstractIT.client)
                    .isSuccess
            )
        }

        Log_OC.d(this, "Finished removing content of folder: " + folder.decryptedRemotePath)
    }

    private fun verifyStoragePath(file: OCFile) {
        if (currentFolder == null) {
            fail("currentFolder cannot be null")
        }

        assert(currentFolder != null)

        assertTrue(file.decryptedRemotePath.startsWith(currentFolder!!.decryptedRemotePath))
    }

    @Before
    @Throws(Exception::class)
    fun initClass() {
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
        val keyPair: KeyPair = EncryptionUtils.generateKeyPair()

        // create CSR
        val accountManager: AccountManager = AccountManager.get(AbstractIT.targetContext)
        val userId: String = accountManager.getUserData(AbstractIT.account, AccountUtils.Constants.KEY_USER_ID)
        val urlEncoded: String = CsrHelper().generateCsrPemEncodedString(keyPair, userId)

        val operation = SendCSRRemoteOperation(urlEncoded)
        val result: RemoteOperationResult<String> =
            operation.executeNextcloudClient(AbstractIT.account, AbstractIT.targetContext)

        if (result.isSuccess) {
            publicKeyString = result.resultData

            // check key
            val privateKey = keyPair.private as RSAPrivateCrtKey
            val publicKey: RSAPublicKey = EncryptionUtils.convertPublicKeyFromString(publicKeyString)

            val modulusPublic = publicKey.modulus
            val modulusPrivate = privateKey.modulus

            if (modulusPrivate.compareTo(modulusPublic) != 0) {
                throw RuntimeException("Wrong CSR returned")
            }
        } else {
            throw Exception("failed to send CSR", result.exception)
        }

        val privateKey = keyPair.private
        val privateKeyString: String = EncryptionUtils.encodeBytesToBase64String(privateKey.encoded)
        val privatePemKeyString: String = EncryptionUtils.privateKeyToPEM(privateKey)
        val encryptedPrivateKey: String = EncryptionUtils.encryptPrivateKey(
            privatePemKeyString,
            generateMnemonicString()
        )

        // upload encryptedPrivateKey
        val storePrivateKeyOperation = StorePrivateKeyRemoteOperation(encryptedPrivateKey)
        val storePrivateKeyResult: RemoteOperationResult<*> = storePrivateKeyOperation.executeNextcloudClient(
            AbstractIT.account,
            AbstractIT.targetContext
        )

        if (storePrivateKeyResult.isSuccess) {
            arbitraryDataProvider?.storeOrUpdateKeyValue(
                AbstractIT.account.name, EncryptionUtils.PRIVATE_KEY,
                privateKeyString
            )
            arbitraryDataProvider?.storeOrUpdateKeyValue(
                AbstractIT.account.name,
                EncryptionUtils.PUBLIC_KEY,
                publicKeyString
            )
            arbitraryDataProvider?.storeOrUpdateKeyValue(
                AbstractIT.account.name, EncryptionUtils.MNEMONIC,
                generateMnemonicString()
            )
        } else {
            throw RuntimeException("Error uploading private key!")
        }
    }

    private fun deleteKeys() {
        val privateKeyRemoteOperationResult: RemoteOperationResult<PrivateKey> =
            GetPrivateKeyRemoteOperation().execute(AbstractIT.nextcloudClient)
        val publicKeyRemoteOperationResult: RemoteOperationResult<String> =
            GetPublicKeyRemoteOperation().execute(AbstractIT.nextcloudClient)

        if (privateKeyRemoteOperationResult.isSuccess || publicKeyRemoteOperationResult.isSuccess) {
            // delete keys
            assertTrue(DeletePrivateKeyRemoteOperation().execute(AbstractIT.nextcloudClient).isSuccess)
            assertTrue(DeletePublicKeyRemoteOperation().execute(AbstractIT.nextcloudClient).isSuccess)

            arbitraryDataProvider?.deleteKeyForAccount(AbstractIT.account.name, EncryptionUtils.PRIVATE_KEY)
            arbitraryDataProvider?.deleteKeyForAccount(AbstractIT.account.name, EncryptionUtils.PUBLIC_KEY)
            arbitraryDataProvider?.deleteKeyForAccount(AbstractIT.account.name, EncryptionUtils.MNEMONIC)
        }
    }

    private fun generateMnemonicString(): String {
        return "1 2 3 4 5 6"
    }
}
