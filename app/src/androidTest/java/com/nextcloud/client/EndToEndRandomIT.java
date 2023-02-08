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

package com.nextcloud.client;

import android.accounts.AccountManager;

import com.nextcloud.test.RandomStringGenerator;
import com.nextcloud.test.RetryTestRule;
import com.owncloud.android.AbstractOnServerIT;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.ocs.responses.PrivateKey;
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation;
import com.owncloud.android.lib.resources.status.OCCapability;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.DeletePrivateKeyOperation;
import com.owncloud.android.lib.resources.users.DeletePublicKeyOperation;
import com.owncloud.android.lib.resources.users.GetPrivateKeyOperation;
import com.owncloud.android.lib.resources.users.GetPublicKeyOperation;
import com.owncloud.android.lib.resources.users.SendCSROperation;
import com.owncloud.android.lib.resources.users.StorePrivateKeyOperation;
import com.owncloud.android.operations.DownloadFileOperation;
import com.owncloud.android.operations.GetCapabilitiesOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.utils.CsrHelper;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import static com.owncloud.android.lib.resources.status.OwnCloudVersion.nextcloud_19;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class EndToEndRandomIT extends AbstractOnServerIT {
    private static ArbitraryDataProvider arbitraryDataProvider;
    private OCFile currentFolder;
    private final int actionCount = 20;
    private String rootEncFolder = "/e/";
    @Rule
    public RetryTestRule retryTestRule = new RetryTestRule();

    @BeforeClass
    public static void initClass() throws Exception {
        arbitraryDataProvider = new ArbitraryDataProviderImpl(targetContext);
        createKeys();
    }

    @Before
    public void before() throws IOException {
        OCCapability capability = getStorageManager().getCapability(account.name);

        if (capability.getVersion().equals(new OwnCloudVersion("0.0.0"))) {
            // fetch new one
            assertTrue(new GetCapabilitiesOperation(getStorageManager())
                           .execute(client)
                           .isSuccess());
        }
        // tests only for NC19+
        assumeTrue(getStorageManager()
                       .getCapability(account.name)
                       .getVersion()
                       .isNewerOrEqual(nextcloud_19)
                  );

        // make sure that every file is available, even after tests that remove source file
        createDummyFiles();
    }

    @Test
    public void run() throws Exception {
        init();

        for (int i = 0; i < actionCount; i++) {
            EndToEndAction nextAction = EndToEndAction.values()[new Random().nextInt(EndToEndAction.values().length)];

            switch (nextAction) {
                case CREATE_FOLDER:
                    createFolder(i);
                    break;

                case GO_INTO_FOLDER:
                    goIntoFolder(i);
                    break;

                case GO_UP:
                    goUp(i);
                    break;

                case UPLOAD_FILE:
                    uploadFile(i);
                    break;

                case DOWNLOAD_FILE:
                    downloadFile(i);
                    break;

                case DELETE_FILE:
                    deleteFile(i);
                    break;

                default:
                    Log_OC.d(this, "[" + i + "/" + actionCount + "]" + " Unknown action: " + nextAction);
                    break;
            }
        }
    }

    @Test
    public void uploadOneFile() throws Exception {
        init();

        uploadFile(0);
    }

    @Test
    public void createFolder() throws Exception {
        init();

        currentFolder = createFolder(0);
        assertNotNull(currentFolder);
    }

    @Test
    public void createSubFolders() throws Exception {
        init();

        currentFolder = createFolder(0);
        assertNotNull(currentFolder);

        currentFolder = createFolder(1);
        assertNotNull(currentFolder);

        currentFolder = createFolder(2);
        assertNotNull(currentFolder);
    }

    @Test
    public void createSubFoldersWithFiles() throws Exception {
        init();

        currentFolder = createFolder(0);
        assertNotNull(currentFolder);

        uploadFile(1);
        uploadFile(1);
        uploadFile(2);

        currentFolder = createFolder(1);
        assertNotNull(currentFolder);
        uploadFile(11);
        uploadFile(12);
        uploadFile(13);

        currentFolder = createFolder(2);
        assertNotNull(currentFolder);

        uploadFile(21);
        uploadFile(22);
        uploadFile(23);
    }

    @Test
    public void pseudoRandom() throws Exception {
        init();

        uploadFile(1);
        createFolder(2);
        goIntoFolder(3);
        goUp(4);
        createFolder(5);
        uploadFile(6);
        goUp(7);
        goIntoFolder(8);
        goIntoFolder(9);
        uploadFile(10);
    }

    @Test
    public void deleteFile() throws Exception {
        init();

        uploadFile(1);
        deleteFile(1);
    }

    @Test
    public void deleteFolder() throws Exception {
        init();

        // create folder, go into it
        OCFile createdFolder = createFolder(0);
        assertNotNull(createdFolder);
        currentFolder = createdFolder;

        uploadFile(1);
        goUp(1);

        // delete folder
        assertTrue(new RemoveFileOperation(createdFolder,
                                           false,
                                           user,
                                           false,
                                           targetContext,
                                           getStorageManager())
                       .execute(client)
                       .isSuccess());
    }

    @Test
    public void downloadFile() throws Exception {
        init();

        uploadFile(1);
        downloadFile(1);
    }

    private void init() throws Exception {
        // create folder
        createFolder(rootEncFolder);
        OCFile encFolder = createFolder(rootEncFolder + RandomStringGenerator.make(5) + "/");

        // encrypt it
        assertTrue(new ToggleEncryptionRemoteOperation(encFolder.getLocalId(),
                                                       encFolder.getRemotePath(),
                                                       true)
                       .execute(client).isSuccess());
        encFolder.setEncrypted(true);
        getStorageManager().saveFolder(encFolder, new ArrayList<>(), new ArrayList<>());

        useExistingKeys();

        rootEncFolder = encFolder.getDecryptedRemotePath();
        currentFolder = encFolder;
    }

    private OCFile createFolder(int i) {
        String path = currentFolder.getDecryptedRemotePath() + RandomStringGenerator.make(5) + "/";
        Log_OC.d(this, "[" + i + "/" + actionCount + "] " + "Create folder: " + path);

        return createFolder(path);
    }

    private void goIntoFolder(int i) {
        ArrayList<OCFile> folders = new ArrayList<>();
        for (OCFile file : getStorageManager().getFolderContent(currentFolder, false)) {
            if (file.isFolder()) {
                folders.add(file);
            }
        }

        if (folders.isEmpty()) {
            Log_OC.d(this, "[" + i + "/" + actionCount + "] " + "Go into folder: No folders");
            return;
        }

        currentFolder = folders.get(new Random().nextInt(folders.size()));
        Log_OC.d(this,
                 "[" + i + "/" + actionCount + "] " + "Go into folder: " + currentFolder.getDecryptedRemotePath());
    }

    private void goUp(int i) {
        if (currentFolder.getRemotePath().equals(rootEncFolder)) {
            Log_OC.d(this,
                     "[" + i + "/" + actionCount + "] " + "Go up to folder: " + currentFolder.getDecryptedRemotePath());
            return;
        }

        currentFolder = getStorageManager().getFileById(currentFolder.getParentId());
        if (currentFolder == null) {
            throw new RuntimeException("Current folder is null");
        }

        Log_OC.d(this,
                 "[" + i + "/" + actionCount + "] " + "Go up to folder: " + currentFolder.getDecryptedRemotePath());
    }

    private void uploadFile(int i) throws IOException {
        String fileName = RandomStringGenerator.make(5) + ".txt";

        File file;
        if (new Random().nextBoolean()) {
            file = createFile(fileName, new Random().nextInt(50000));
        } else {
            file = createFile(fileName, 500000 + new Random().nextInt(50000));
        }

        String remotePath = currentFolder.getRemotePath() + fileName;

        Log_OC.d(this,
                 "[" + i + "/" + actionCount + "] " +
                     "Upload file to: " + currentFolder.getDecryptedRemotePath() + fileName);

        OCUpload ocUpload = new OCUpload(file.getAbsolutePath(),
                                         remotePath,
                                         account.name);
        uploadOCUpload(ocUpload);
        shortSleep();

        OCFile parentFolder = getStorageManager()
            .getFileByEncryptedRemotePath(new File(ocUpload.getRemotePath()).getParent() + "/");
        String uploadedFileName = new File(ocUpload.getRemotePath()).getName();

        String decryptedPath = parentFolder.getDecryptedRemotePath() + uploadedFileName;

        OCFile uploadedFile = getStorageManager().getFileByDecryptedRemotePath(decryptedPath);
        verifyStoragePath(uploadedFile);

        // verify storage path
        refreshFolder(currentFolder.getRemotePath());
        uploadedFile = getStorageManager().getFileByDecryptedRemotePath(decryptedPath);
        verifyStoragePath(uploadedFile);

        // verify that encrypted file is on server
        assertTrue(new ReadFileRemoteOperation(currentFolder.getRemotePath() + uploadedFile.getEncryptedFileName())
                       .execute(client)
                       .isSuccess());

        // verify that unencrypted file is not on server
        assertFalse(new ReadFileRemoteOperation(currentFolder.getDecryptedRemotePath() + fileName)
                        .execute(client)
                        .isSuccess());
    }

    private void downloadFile(int i) {
        ArrayList<OCFile> files = new ArrayList<>();
        for (OCFile file : getStorageManager().getFolderContent(currentFolder, false)) {
            if (!file.isFolder()) {
                files.add(file);
            }
        }

        if (files.isEmpty()) {
            Log_OC.d(this, "[" + i + "/" + actionCount + "] No files in: " + currentFolder.getDecryptedRemotePath());
            return;
        }

        OCFile fileToDownload = files.get(new Random().nextInt(files.size()));
        assertNotNull(fileToDownload.getRemoteId());

        Log_OC.d(this,
                 "[" + i + "/" + actionCount + "] " + "Download file: " +
                     currentFolder.getDecryptedRemotePath() + fileToDownload.getDecryptedFileName());

        assertTrue(new DownloadFileOperation(user, fileToDownload, targetContext)
                       .execute(client)
                       .isSuccess());

        assertTrue(new File(fileToDownload.getStoragePath()).exists());
        verifyStoragePath(fileToDownload);
    }

    @Test
    public void testUploadWithCopy() throws Exception {
        init();

        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         currentFolder.getRemotePath() + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_COPY);

        File originalFile = new File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        OCFile uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(currentFolder.getRemotePath() +
                                                                                      "nonEmpty.txt");

        assertTrue(originalFile.exists());
        assertTrue(new File(uploadedFile.getStoragePath()).exists());
    }

    @Test
    public void testUploadWithMove() throws Exception {
        init();

        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         currentFolder.getRemotePath() + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_MOVE);

        File originalFile = new File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        OCFile uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(currentFolder.getRemotePath() +
                                                                                      "nonEmpty.txt");

        assertFalse(originalFile.exists());
        assertTrue(new File(uploadedFile.getStoragePath()).exists());
    }

    @Test
    public void testUploadWithForget() throws Exception {
        init();

        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         currentFolder.getRemotePath() + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_FORGET);

        File originalFile = new File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        OCFile uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(currentFolder.getRemotePath() +
                                                                                      "nonEmpty.txt");

        assertTrue(originalFile.exists());
        assertFalse(new File(uploadedFile.getStoragePath()).exists());
    }

    @Test
    public void testUploadWithDelete() throws Exception {
        init();

        OCUpload ocUpload = new OCUpload(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt",
                                         currentFolder.getRemotePath() + "nonEmpty.txt",
                                         account.name);

        uploadOCUpload(ocUpload, FileUploader.LOCAL_BEHAVIOUR_DELETE);

        File originalFile = new File(FileStorageUtils.getTemporalPath(account.name) + "/nonEmpty.txt");
        OCFile uploadedFile = fileDataStorageManager.getFileByDecryptedRemotePath(currentFolder.getRemotePath() +
                                                                                      "nonEmpty.txt");

        assertFalse(originalFile.exists());
        assertFalse(new File(uploadedFile.getStoragePath()).exists());
    }

    @Test
    public void testCheckCSR() throws Exception {
        deleteKeys();

        // Create public/private key pair
        KeyPair keyPair = EncryptionUtils.generateKeyPair();

        // create CSR
        AccountManager accountManager = AccountManager.get(targetContext);
        String userId = accountManager.getUserData(account, AccountUtils.Constants.KEY_USER_ID);
        String urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, userId);

        SendCSROperation operation = new SendCSROperation(urlEncoded);
        RemoteOperationResult<String> result = operation.executeNextcloudClient(account, targetContext);

        assertTrue(result.isSuccess());
        String publicKeyString = result.getResultData();

        // check key
        RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
        RSAPublicKey publicKey = EncryptionUtils.convertPublicKeyFromString(publicKeyString);

        BigInteger modulusPublic = publicKey.getModulus();
        BigInteger modulusPrivate = privateKey.getModulus();

        assertEquals(modulusPrivate, modulusPublic);

        createKeys();
    }

    private void deleteFile(int i) {
        ArrayList<OCFile> files = new ArrayList<>();
        for (OCFile file : getStorageManager().getFolderContent(currentFolder, false)) {
            if (!file.isFolder()) {
                files.add(file);
            }
        }

        if (files.isEmpty()) {
            Log_OC.d(this, "[" + i + "/" + actionCount + "] No files in: " + currentFolder.getDecryptedRemotePath());
            return;
        }

        OCFile fileToDelete = files.get(new Random().nextInt(files.size()));
        assertNotNull(fileToDelete.getRemoteId());

        Log_OC.d(this,
                 "[" + i + "/" + actionCount + "] " +
                     "Delete file: " + currentFolder.getDecryptedRemotePath() + fileToDelete.getDecryptedFileName());

        assertTrue(new RemoveFileOperation(fileToDelete,
                                           false,
                                           user,
                                           false,
                                           targetContext,
                                           getStorageManager())
                       .execute(client)
                       .isSuccess());
    }

    @Test
    public void reInit() throws Exception {
        // create folder
        OCFile encFolder = createFolder(rootEncFolder);

        // encrypt it
        assertTrue(new ToggleEncryptionRemoteOperation(encFolder.getLocalId(),
                                                       encFolder.getRemotePath(),
                                                       true)
                       .execute(client).isSuccess());
        encFolder.setEncrypted(true);
        getStorageManager().saveFolder(encFolder, new ArrayList<>(), new ArrayList<>());


        // delete keys
        arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PRIVATE_KEY);
        arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PUBLIC_KEY);
        arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.MNEMONIC);

        useExistingKeys();
    }

    private void useExistingKeys() throws Exception {
        // download them from server
        GetPublicKeyOperation publicKeyOperation = new GetPublicKeyOperation();
        RemoteOperationResult<String> publicKeyResult = publicKeyOperation.executeNextcloudClient(account,
                                                                                                  targetContext);

        assertTrue("Result code:" + publicKeyResult.getHttpCode(), publicKeyResult.isSuccess());

        String publicKeyFromServer = publicKeyResult.getResultData();
        arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                                    EncryptionUtils.PUBLIC_KEY,
                                                    publicKeyFromServer);

        RemoteOperationResult<PrivateKey> privateKeyResult = new GetPrivateKeyOperation()
            .executeNextcloudClient(account, targetContext);
        assertTrue(privateKeyResult.isSuccess());

        PrivateKey privateKey = privateKeyResult.getResultData();

        String mnemonic = generateMnemonicString();
        String decryptedPrivateKey = EncryptionUtils.decryptPrivateKey(privateKey.getKey(), mnemonic);

        arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                                    EncryptionUtils.PRIVATE_KEY, decryptedPrivateKey);

        Log_OC.d(this, "Private key successfully decrypted and stored");

        arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.MNEMONIC, mnemonic);
    }

    /*
    TODO do not c&p code
     */
    private static void createKeys() throws Exception {
        deleteKeys();

        String publicKeyString;

        // Create public/private key pair
        KeyPair keyPair = EncryptionUtils.generateKeyPair();

        // create CSR
        AccountManager accountManager = AccountManager.get(targetContext);
        String userId = accountManager.getUserData(account, AccountUtils.Constants.KEY_USER_ID);
        String urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, userId);

        SendCSROperation operation = new SendCSROperation(urlEncoded);
        RemoteOperationResult<String> result = operation.executeNextcloudClient(account, targetContext);

        if (result.isSuccess()) {
            publicKeyString = result.getResultData();

            // check key
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) keyPair.getPrivate();
            RSAPublicKey publicKey = EncryptionUtils.convertPublicKeyFromString(publicKeyString);

            BigInteger modulusPublic = publicKey.getModulus();
            BigInteger modulusPrivate = privateKey.getModulus();

            if (modulusPrivate.compareTo(modulusPublic) != 0) {
                throw new RuntimeException("Wrong CSR returned");
            }
        } else {
            throw new Exception("failed to send CSR", result.getException());
        }

        java.security.PrivateKey privateKey = keyPair.getPrivate();
        String privateKeyString = EncryptionUtils.encodeBytesToBase64String(privateKey.getEncoded());
        String privatePemKeyString = EncryptionUtils.privateKeyToPEM(privateKey);
        String encryptedPrivateKey = EncryptionUtils.encryptPrivateKey(privatePemKeyString,
                                                                       generateMnemonicString());

        // upload encryptedPrivateKey
        StorePrivateKeyOperation storePrivateKeyOperation = new StorePrivateKeyOperation(encryptedPrivateKey);
        RemoteOperationResult storePrivateKeyResult = storePrivateKeyOperation.execute(account, targetContext);

        if (storePrivateKeyResult.isSuccess()) {
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PRIVATE_KEY,
                                                        privateKeyString);
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PUBLIC_KEY, publicKeyString);
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.MNEMONIC,
                                                        generateMnemonicString());
        } else {
            throw new RuntimeException("Error uploading private key!");
        }
    }

    private static void deleteKeys() {
        RemoteOperationResult<PrivateKey> privateKeyRemoteOperationResult =
            new GetPrivateKeyOperation().execute(nextcloudClient);
        RemoteOperationResult<String> publicKeyRemoteOperationResult =
            new GetPublicKeyOperation().execute(nextcloudClient);

        if (privateKeyRemoteOperationResult.isSuccess() || publicKeyRemoteOperationResult.isSuccess()) {
            // delete keys
            assertTrue(new DeletePrivateKeyOperation().execute(nextcloudClient).isSuccess());
            assertTrue(new DeletePublicKeyOperation().execute(nextcloudClient).isSuccess());

            arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PRIVATE_KEY);
            arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PUBLIC_KEY);
            arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.MNEMONIC);
        } else {
            throw new RuntimeException("Error fetching keys");
        }
    }

    private static String generateMnemonicString() {
        return "1 2 3 4 5 6";
    }

    public void after() {
        // remove all encrypted files
        OCFile root = fileDataStorageManager.getFileByDecryptedRemotePath("/");
        removeFolder(root);

//        List<OCFile> files = fileDataStorageManager.getFolderContent(root, false);
//
//        for (OCFile child : files) {
//            removeFolder(child);
//        }

        assertEquals(0, fileDataStorageManager.getFolderContent(root, false).size());

        super.after();
    }

    private void removeFolder(OCFile folder) {
        Log_OC.d(this, "Start removing content of folder: " + folder.getDecryptedRemotePath());

        List<OCFile> children = fileDataStorageManager.getFolderContent(folder, false);

        // remove children
        for (OCFile child : children) {
            if (child.isFolder()) {
                removeFolder(child);

                // remove folder
                Log_OC.d(this, "Remove folder: " + child.getDecryptedRemotePath());
                if (!folder.isEncrypted() && child.isEncrypted()) {
                    assertTrue(new ToggleEncryptionRemoteOperation(child.getLocalId(),
                                                                   child.getRemotePath(),
                                                                   false)
                                   .execute(client)
                                   .isSuccess());

                    OCFile f = getStorageManager().getFileByEncryptedRemotePath(child.getRemotePath());
                    f.setEncrypted(false);
                    getStorageManager().saveFile(f);

                    child.setEncrypted(false);
                }
            } else {
                Log_OC.d(this, "Remove file: " + child.getDecryptedRemotePath());
            }

            assertTrue(new RemoveFileOperation(child, false, user, false, targetContext, getStorageManager())
                           .execute(client)
                           .isSuccess()
                      );
        }

        Log_OC.d(this, "Finished removing content of folder: " + folder.getDecryptedRemotePath());
    }

    private void verifyStoragePath(OCFile file) {
        assertEquals(FileStorageUtils.getSavePath(account.name) +
                         currentFolder.getDecryptedRemotePath() +
                         file.getDecryptedFileName(),
                     file.getStoragePath());
    }
}
