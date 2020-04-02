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

import com.owncloud.android.AbstractIT;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.OCUpload;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation;
import com.owncloud.android.lib.resources.users.GetPrivateKeyOperation;
import com.owncloud.android.lib.resources.users.GetPublicKeyOperation;
import com.owncloud.android.lib.resources.users.SendCSROperation;
import com.owncloud.android.lib.resources.users.StorePrivateKeyOperation;
import com.owncloud.android.operations.RemoveFileOperation;
import com.owncloud.android.utils.CsrHelper;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;

import net.bytebuddy.utility.RandomString;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Random;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class EndToEndRandomIT extends AbstractIT {
    public enum Action {
        CREATE_FOLDER,
        GO_INTO_FOLDER,
        GO_UP,
        UPLOAD_FILE
    }

    private static ArbitraryDataProvider arbitraryDataProvider;

    private OCFile currentFolder;
    private int actionCount = 20;
    private String rootEncFolder = "/e/";

    @BeforeClass
    public static void initClass() {
        arbitraryDataProvider = new ArbitraryDataProvider(targetContext.getContentResolver());
    }

    @Test
    public void run() throws Exception {
        init();

        for (int i = 0; i < actionCount; i++) {
            Action nextAction = Action.values()[new Random().nextInt(Action.values().length)];

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

    private void init() throws Exception {
        // create folder
        createFolder(rootEncFolder);
        OCFile encFolder = createFolder(rootEncFolder + RandomString.make(5) + "/");

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
        String path = currentFolder.getDecryptedRemotePath() + RandomString.make(5) + "/";
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
        String fileName = RandomString.make(5) + ".txt";
        createFile(fileName, new Random().nextInt(50000));
        String path = currentFolder.getRemotePath() + fileName;

        Log_OC.d(this,
                 "[" + i + "/" + actionCount + "] " +
                     "Upload file to: " + currentFolder.getDecryptedRemotePath() + fileName);

        OCUpload ocUpload = new OCUpload(FileStorageUtils.getSavePath(account.name) + File.separator + fileName,
                                         path,
                                         account.name);
        uploadOCUpload(ocUpload);
    }

    private void deleteFile(int i) {
        ArrayList<OCFile> files = new ArrayList<>();
        for (OCFile file : getStorageManager().getFolderContent(currentFolder, false)) {
            if (!file.isFolder()) {
                files.add(file);
            }
        }

        OCFile fileToDelete = files.get(new Random().nextInt(files.size()));

        Log_OC.d(this,
                 "[" + i + "/" + actionCount + "] " +
                     "Delete file: " + currentFolder.getDecryptedRemotePath() + fileToDelete.getDecryptedFileName());

        assertTrue(new RemoveFileOperation(fileToDelete,
                                           false,
                                           account,
                                           false,
                                           targetContext)
                       .execute(client, getStorageManager())
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

        createKeys();

        // delete keys
        arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PRIVATE_KEY);
        arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.PUBLIC_KEY);
        arbitraryDataProvider.deleteKeyForAccount(account.name, EncryptionUtils.MNEMONIC);

        useExistingKeys();
    }

    private void useExistingKeys() throws Exception {
        // download them from server
        GetPublicKeyOperation publicKeyOperation = new GetPublicKeyOperation();
        RemoteOperationResult publicKeyResult = publicKeyOperation.execute(account, targetContext);

        assertTrue(publicKeyResult.isSuccess());

        String publicKeyFromServer = (String) publicKeyResult.getData().get(0);
        arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                                    EncryptionUtils.PUBLIC_KEY,
                                                    publicKeyFromServer);

        GetPrivateKeyOperation privateKeyOperation = new GetPrivateKeyOperation();
        RemoteOperationResult privateKeyResult = privateKeyOperation.execute(account, targetContext);
        assertTrue(privateKeyResult.isSuccess());

        String privateKey = (String) privateKeyResult.getData().get(0);

        String mnemonic = generateMnemonicString();
        String decryptedPrivateKey = EncryptionUtils.decryptPrivateKey(privateKey, mnemonic);

        arbitraryDataProvider.storeOrUpdateKeyValue(account.name,
                                                    EncryptionUtils.PRIVATE_KEY, decryptedPrivateKey);

        Log_OC.d(this, "Private key successfully decrypted and stored");

        arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.MNEMONIC, mnemonic);
    }

    /*
    TODO do not c&p code
     */
    private void createKeys() throws Exception {
        String publicKey;

        // Create public/private key pair
        KeyPair keyPair = EncryptionUtils.generateKeyPair();

        // create CSR
        AccountManager accountManager = AccountManager.get(targetContext);
        String userId = accountManager.getUserData(account, AccountUtils.Constants.KEY_USER_ID);
        String urlEncoded = CsrHelper.generateCsrPemEncodedString(keyPair, userId);

        SendCSROperation operation = new SendCSROperation(urlEncoded);
        RemoteOperationResult result = operation.execute(account, targetContext);

        if (result.isSuccess()) {
            publicKey = (String) result.getData().get(0);
        } else {
            throw new Exception("failed to send CSR", result.getException());
        }

        PrivateKey privateKey = keyPair.getPrivate();
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
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.PUBLIC_KEY, publicKey);
            arbitraryDataProvider.storeOrUpdateKeyValue(account.name, EncryptionUtils.MNEMONIC,
                                                        generateMnemonicString());
        } else {
            throw new RuntimeException("Error uploading private key!");
        }
    }

    private String generateMnemonicString() {
        return "1 2 3 4 5 6";
    }
}
