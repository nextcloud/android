/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020-2023 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */

@file:Suppress(
    "ReturnCount",
    "TooGenericExceptionCaught",
    "TooManyFunctions",
    "NestedBlockDepth",
    "MagicNumber",
    "LongParameterList",
    "LargeClass",
    "LongMethod"
)

package com.owncloud.android.operations

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.text.format.Formatter
import androidx.annotation.CheckResult
import androidx.core.net.toUri
import com.nextcloud.client.account.User
import com.nextcloud.client.device.PowerManagementService
import com.nextcloud.client.jobs.upload.FileUploadHelper.Companion.instance
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.autoRename.AutoRename.rename
import com.nextcloud.utils.e2ee.E2EVersionHelper.isV2Plus
import com.nextcloud.utils.e2ee.E2EVersionHelper.latestVersion
import com.nextcloud.utils.extensions.isConflict
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTask
import com.owncloud.android.datamodel.ThumbnailsCacheManager.ThumbnailGenerationTaskObject
import com.owncloud.android.datamodel.UploadsStorageManager
import com.owncloud.android.datamodel.e2e.v1.decrypted.Data
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedMetadata
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFile
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.db.OCUpload
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener
import com.owncloud.android.lib.common.network.ProgressiveDataTransfer
import com.owncloud.android.lib.common.operations.OperationCancelledException
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ChunkedFileUploadRemoteOperation
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.UploadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.operations.common.SyncOperation
import com.owncloud.android.operations.e2e.E2EClientData
import com.owncloud.android.operations.e2e.E2EData
import com.owncloud.android.operations.e2e.E2EFiles
import com.owncloud.android.operations.upload.UploadFileException.EmptyOrNullFilePath
import com.owncloud.android.operations.upload.UploadFileException.MissingPermission
import com.owncloud.android.operations.upload.showStoragePermissionNotification
import com.owncloud.android.utils.EncryptionUtils
import com.owncloud.android.utils.EncryptionUtilsV2
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.FileUtil.getCreationTimestamp
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.UriUtils
import com.owncloud.android.utils.theme.CapabilityUtils
import org.apache.commons.httpclient.HttpStatus
import org.apache.commons.httpclient.methods.RequestEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.security.InvalidAlgorithmParameterException
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.security.cert.CertificateException
import java.security.spec.InvalidParameterSpecException
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.NoSuchPaddingException

/**
 * Operation performing the update in the ownCloud server of a file that was modified locally.
 */
@Suppress("ReturnCount")
class UploadFileOperation(
    uploadsStorageManager: UploadsStorageManager,
    connectivityService: ConnectivityService,
    powerManagementService: PowerManagementService,
    user: User,
    file: OCFile?,
    upload: OCUpload?,
    nameCollisionPolicy: NameCollisionPolicy,
    localBehaviour: Int,
    context: Context,
    onWifiOnly: Boolean,
    whileChargingOnly: Boolean,
    disableRetries: Boolean,
    storageManager: FileDataStorageManager
) : SyncOperation(storageManager) {
    /**
     * OCFile which is to be uploaded.
     */
    var file: OCFile? = null
        private set

    /**
     * If remote file was renamed, return original OCFile which was uploaded. Is null is file was not renamed.
     * Original OCFile which is to be uploaded in case file had to be renamed (if nameCollisionPolicy==RENAME and remote
     * file already exists).
     */
    var oldFile: OCFile? = null
        private set
    private var mRemotePath: String
    private val folderUnlockToken: String?
    private var remoteFolderToBeCreated: Boolean
    private val nameCollisionPolicy: NameCollisionPolicy
    val localBehaviour: Int
    private var mCreatedBy: Int
    val isWifiRequired: Boolean
    val isChargingRequired: Boolean
    val isIgnoringPowerSaveMode: Boolean
    private val disableRetries: Boolean

    private var wasRenamed = false
    var ocUploadId: Long

    /**
     * Local path to file which is to be uploaded (before any possible renaming or moving).
     */
    val originalStoragePath: String
    val dataTransferListeners: MutableSet<OnDatatransferProgressListener?> = HashSet()
    private var renameUploadListener: OnRenameListener? = null

    private val cancellationRequested = AtomicBoolean(false)
    private val uploadStarted = AtomicBoolean(false)

    val context: Context

    private var uploadOperation: UploadFileRemoteOperation? = null

    private var requestEntity: RequestEntity? = null

    val user: User
    private val oCUpload: OCUpload?
    private val uploadsStorageManager: UploadsStorageManager
    private val connectivityService: ConnectivityService
    private val powerManagementService: PowerManagementService

    private var encryptedAncestor = false
    private var duplicatedEncryptedFile: OCFile? = null
    private val missingPermissionThrown = AtomicBoolean(false)

    @Suppress("ReturnCount")
    constructor(
        uploadsStorageManager: UploadsStorageManager,
        connectivityService: ConnectivityService,
        powerManagementService: PowerManagementService,
        user: User,
        file: OCFile?,
        upload: OCUpload,
        nameCollisionPolicy: NameCollisionPolicy,
        localBehaviour: Int,
        context: Context,
        onWifiOnly: Boolean,
        whileChargingOnly: Boolean,
        storageManager: FileDataStorageManager
    ) : this(
        uploadsStorageManager,
        connectivityService,
        powerManagementService,
        user,
        file,
        upload,
        nameCollisionPolicy,
        localBehaviour,
        context,
        onWifiOnly,
        whileChargingOnly,
        true,
        storageManager
    )

    init {
        if (upload == null) {
            Log_OC.e(TAG, "UploadFileOperation upload is null cant construct")
            throw IllegalArgumentException("Illegal NULL file in UploadFileOperation creation")
        }
        if (TextUtils.isEmpty(upload.localPath)) {
            Log_OC.e(TAG, "UploadFileOperation local path is null cant construct")
            throw IllegalArgumentException("storage path invalid: ${upload.localPath}")
        }
        Log_OC.d(
            TAG,
            "creating upload file operation, user: " + user.accountName + " upload account name " + upload.accountName
        )
        this.uploadsStorageManager = uploadsStorageManager
        this.connectivityService = connectivityService
        this.powerManagementService = powerManagementService
        this.user = user
        oCUpload = upload
        if (file == null) {
            Log_OC.w(TAG, "UploadFileOperation file is null, obtaining from upload")
            this.file = obtainNewOCFileToUpload(
                upload.remotePath,
                upload.localPath,
                upload.mimeType
            )
        } else {
            this.file = file
        }
        this.isWifiRequired = onWifiOnly
        this.isChargingRequired = whileChargingOnly
        mRemotePath = upload.remotePath
        this@UploadFileOperation.nameCollisionPolicy = nameCollisionPolicy
        this.localBehaviour = localBehaviour
        this.originalStoragePath = file!!.storagePath
        this.context = context
        this.ocUploadId = upload.uploadId
        mCreatedBy = upload.createdBy
        remoteFolderToBeCreated = upload.isCreateRemoteFolder
        // Ignore power save mode only if user explicitly created this upload
        this.isIgnoringPowerSaveMode = mCreatedBy == CREATED_BY_USER
        folderUnlockToken = upload.folderUnlockToken
        this@UploadFileOperation.disableRetries = disableRetries
    }

    val fileName: String?
        get() = if (this.file != null) file!!.fileName else null

    val storagePath: String
        get() = file!!.storagePath

    val remotePath: String
        get() = file!!.remotePath

    val decryptedRemotePath: String?
        get() = file!!.getDecryptedRemotePath()

    val mimeType: String?
        get() = file!!.mimeType

    fun setRemoteFolderToBeCreated(): UploadFileOperation {
        remoteFolderToBeCreated = true

        return this
    }

    fun wasRenamed(): Boolean = wasRenamed

    var createdBy: Int
        get() = mCreatedBy
        set(createdBy) {
            mCreatedBy = createdBy
            if (createdBy !in CREATED_BY_USER..CREATED_AS_INSTANT_VIDEO) {
                mCreatedBy = CREATED_BY_USER
            }
        }

    val isInstantPicture: Boolean
        get() = mCreatedBy == CREATED_AS_INSTANT_PICTURE

    val isInstantVideo: Boolean
        get() = mCreatedBy == CREATED_AS_INSTANT_VIDEO

    fun addDataTransferProgressListener(listener: OnDatatransferProgressListener?) {
        synchronized(this.dataTransferListeners) {
            dataTransferListeners.add(listener)
        }
        if (requestEntity != null) {
            (requestEntity as ProgressiveDataTransfer).addDataTransferProgressListener(listener)
        }
        if (uploadOperation != null) {
            uploadOperation!!.addDataTransferProgressListener(listener)
        }
    }

    fun removeDataTransferProgressListener(listener: OnDatatransferProgressListener?) {
        synchronized(this.dataTransferListeners) {
            dataTransferListeners.remove(listener)
        }
        if (requestEntity != null) {
            (requestEntity as ProgressiveDataTransfer).removeDataTransferProgressListener(listener)
        }
        if (uploadOperation != null) {
            uploadOperation!!.removeDataTransferProgressListener(listener)
        }
    }

    fun addRenameUploadListener(listener: OnRenameListener?): UploadFileOperation {
        renameUploadListener = listener

        return this
    }

    fun isMissingPermissionThrown(): Boolean = missingPermissionThrown.get()

    @Suppress("ReturnCount")
    @Deprecated("Deprecated in Java")
    override fun run(client: OwnCloudClient): RemoteOperationResult<*> {
        Log_OC.d(TAG, "------- Upload File Operation Started -------")
        if (TextUtils.isEmpty(this.storagePath)) {
            Log_OC.e(TAG, "Upload cancelled for " + this.storagePath + ": file path is null or empty.")
            return RemoteOperationResult<Any?>(EmptyOrNullFilePath())
        }

        val localFile = File(this.storagePath)
        if (!localFile.exists()) {
            Log_OC.e(TAG, "Upload cancelled for " + this.storagePath + ": local file not exists.")
            return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND)
        }

        if (!localFile.canRead()) {
            Log_OC.e(TAG, "Upload cancelled for " + this.storagePath + ": file is not readable or inaccessible.")
            this.showStoragePermissionNotification()
            missingPermissionThrown.set(true)
            return RemoteOperationResult<Any?>(MissingPermission())
        }

        cancellationRequested.set(false)
        uploadStarted.set(true)

        updateSize(0)
        Log_OC.d(TAG, "file size set to 0KB before upload")

        var remoteParentPath = File(this.remotePath).parent
        if (remoteParentPath == null) {
            Log_OC.e(TAG, "remoteParentPath is null: " + this.remotePath)
            return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
        }
        remoteParentPath = if (remoteParentPath.endsWith(OCFile.PATH_SEPARATOR)) {
            remoteParentPath
        } else {
            remoteParentPath + OCFile.PATH_SEPARATOR
        }

        val renamedRemoteParentPath = rename(remoteParentPath, this.capabilities)
        if (remoteParentPath != renamedRemoteParentPath) {
            Log_OC.w(TAG, "remoteParentPath was renamed: $remoteParentPath → $renamedRemoteParentPath")
        }
        remoteParentPath = renamedRemoteParentPath

        var parent = storageManager.getFileByPath(remoteParentPath)
        Log_OC.d(
            TAG,
            "parent lookup for path: " + remoteParentPath + " → " +
                (if (parent == null) "not found in DB" else "found, id=" + parent.fileId)
        )

        // in case of a fresh upload with subfolder, where parent does not exist yet
        if (parent == null && folderUnlockToken.isNullOrEmpty()) {
            Log_OC.d(
                TAG,
                "parent not in DB and no unlock token, attempting to grant folder existence: " +
                    remoteParentPath
            )
            val result = grantFolderExistence(remoteParentPath, client)

            if (!result.isSuccess) {
                Log_OC.e(
                    TAG,
                    "grantFolderExistence failed for: " + remoteParentPath + ", code: " +
                        result.code + ", message: " + result.message
                )
                return result
            }

            parent = storageManager.getFileByPath(remoteParentPath)
            if (parent == null) {
                Log_OC.e(TAG, "parent still null after grantFolderExistence: $remoteParentPath")
                return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
            }

            Log_OC.d(
                TAG,
                "parent created and retrieved successfully: " + remoteParentPath + ", id=" +
                    parent.fileId
            )
        }

        if (parent == null) {
            Log_OC.e(
                TAG,
                "parent is null, cannot proceed: $remoteParentPath, unlock token: $folderUnlockToken"
            )
            return RemoteOperationResult<Any?>(false, "Parent folder not found", HttpStatus.SC_NOT_FOUND)
        }

        // - resume of encrypted upload, then parent file exists already as unlock is only for direct parent
        file!!.parentId = parent.fileId

        // check if any parent is encrypted
        encryptedAncestor = FileStorageUtils.checkEncryptionStatus(parent, storageManager)
        file!!.isEncrypted = encryptedAncestor

        if (encryptedAncestor) {
            Log_OC.d(TAG, "⬆️🔗" + "encrypted upload")
            return encryptedUpload(client, parent)
        } else {
            Log_OC.d(TAG, "⬆️" + "normal upload")
            return normalUpload(client)
        }
    }

    // region E2E Upload
    @SuppressLint("AndroidLintUseSparseArrays", "ReturnCount")
    // gson cannot handle sparse arrays easily, therefore use hashmap
    private fun encryptedUpload(client: OwnCloudClient, parentFile: OCFile): RemoteOperationResult<*> {
        var result: RemoteOperationResult<*>? = null
        val e2eFiles = E2EFiles(parentFile, null, File(this.originalStoragePath), null, null)
        var fileLock: FileLock? = null
        val size: Long
        var metadataExists = false
        var token: String? = null
        var anyObject: Any? = null
        var channel: FileChannel? = null
        val arbitraryDataProvider: ArbitraryDataProvider = ArbitraryDataProviderImpl(this.context)
        val publicKey = arbitraryDataProvider.getValue(user.accountName, EncryptionUtils.PUBLIC_KEY)

        try {
            result = checkConditions(e2eFiles.originalFile)

            if (result != null) {
                return result
            }

            val counter = getE2ECounter(parentFile)

            try {
                token = getFolderUnlockTokenOrLockFolder(client, parentFile, counter)
            } catch (e: Exception) {
                Log_OC.e(TAG, "Failed to lock folder", e)
                return RemoteOperationResult<Any?>(e)
            }

            // Update metadata
            val encryptionUtilsV2 = EncryptionUtilsV2()
            anyObject = EncryptionUtils.downloadFolderMetadata(parentFile, client, this.context, user)
            if (anyObject is DecryptedFolderMetadataFileV1 && anyObject.metadata != null) {
                metadataExists = true
            }

            if (this.isEndToEndVersionAtLeastV2) {
                if (anyObject == null) {
                    return RemoteOperationResult<Any?>(IllegalStateException("Metadata does not exist"))
                }
            } else {
                anyObject = getDecryptedFolderMetadataV1(publicKey, anyObject)
            }

            val clientData = E2EClientData(client, token, publicKey)

            val fileNames = getCollidedFileNames(anyObject)

            val collisionResult = checkNameCollision(parentFile, client, fileNames, parentFile.isEncrypted)
            if (collisionResult != null) {
                result = collisionResult
                return collisionResult
            }

            file!!.decryptedRemotePath = parentFile.getDecryptedRemotePath() + e2eFiles.originalFile.name
            val expectedPath = FileStorageUtils.getDefaultSavePathFor(user.accountName, this.file)
            e2eFiles.expectedFile = File(expectedPath)

            result = copyFile(e2eFiles.originalFile, expectedPath)
            if (!result.isSuccess) {
                return result
            }

            val lastModifiedTimestamp = e2eFiles.originalFile.lastModified() / 1000
            val creationTimestamp = getCreationTimestamp(e2eFiles.originalFile)
            if (creationTimestamp == null) {
                Log_OC.e(TAG, "UploadFileOperation creationTimestamp cannot be null")
                throw NullPointerException("creationTimestamp cannot be null")
            }

            val e2eData = getE2EData(anyObject)
            e2eFiles.encryptedTempFile = e2eData.encryptedFile.encryptedFile

            val channelResult = initFileChannel(result, null, e2eFiles)
            fileLock = channelResult.first
            result = channelResult.second
            channel = channelResult.third

            size = getChannelSize(channel!!)
            updateSize(size)
            setUploadOperationForE2E(
                token,
                e2eFiles.encryptedTempFile!!,
                e2eData.encryptedFileName,
                lastModifiedTimestamp,
                creationTimestamp,
                size
            )

            result = performE2EUpload(clientData)

            if (result.isSuccess) {
                updateMetadataForE2E(
                    anyObject,
                    e2eData,
                    clientData,
                    e2eFiles,
                    arbitraryDataProvider,
                    encryptionUtilsV2,
                    metadataExists
                )
            }
        } catch (_: FileNotFoundException) {
            Log_OC.e(TAG, file!!.storagePath + " does not exist anymore")
            result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND)
        } catch (_: OverlappingFileLockException) {
            Log_OC.e(TAG, "Overlapping file lock exception")
            result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCK_FAILED)
        } catch (e: Exception) {
            Log_OC.e(TAG, "UploadFileOperation exception: " + e.localizedMessage)
            result = RemoteOperationResult<Any?>(e)
        } finally {
            result = cleanupE2EUpload(fileLock, channel, e2eFiles, result, anyObject, client, token)

            // update upload status
            uploadsStorageManager.updateDatabaseUploadResult(result, this)
        }

        completeE2EUpload(result, e2eFiles, client)

        return result
    }

    private val isEndToEndVersionAtLeastV2: Boolean
        get() {
            val capability = CapabilityUtils.getCapability(this.context)
            return isV2Plus(capability)
        }

    private fun getE2ECounter(parentFile: OCFile): Long {
        var counter: Long = -1

        if (this.isEndToEndVersionAtLeastV2) {
            counter = parentFile.e2eCounter + 1
        }

        return counter
    }

    @Throws(UploadException::class)
    private fun getFolderUnlockTokenOrLockFolder(client: OwnCloudClient?, parentFile: OCFile, counter: Long): String {
        if (!folderUnlockToken.isNullOrEmpty()) {
            Log_OC.d(TAG, "Reusing existing folder unlock token from previous upload attempt")
            return folderUnlockToken
        }

        val token = EncryptionUtils.lockFolder(parentFile, client, counter)
        if (token == null || token.isEmpty()) {
            Log_OC.e(TAG, "Lock folder returned null or empty token")
            throw UploadException("Failed to lock folder: token is null or empty")
        }

        oCUpload!!.folderUnlockToken = token
        uploadsStorageManager.updateUpload(oCUpload)

        Log_OC.d(TAG, "Folder locked successfully, token saved")
        return token
    }

    @Throws(
        NoSuchPaddingException::class,
        IllegalBlockSizeException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        BadPaddingException::class,
        InvalidKeyException::class
    )
    private fun getDecryptedFolderMetadataV1(publicKey: String, anyObject: Any?): DecryptedFolderMetadataFileV1 {
        var metadata = DecryptedFolderMetadataFileV1()
        metadata.metadata = DecryptedMetadata()
        metadata.metadata.version = 1.2
        metadata.metadata.metadataKeys = HashMap<Int?, String?>()
        val metadataKey = EncryptionUtils.encodeBytesToBase64String(EncryptionUtils.generateKey())
        val encryptedMetadataKey = EncryptionUtils.encryptStringAsymmetric(metadataKey, publicKey)
        metadata.metadata.metadataKey = encryptedMetadataKey

        if (anyObject is DecryptedFolderMetadataFileV1) {
            metadata = anyObject
        }

        return metadata
    }

    @Suppress("UNCHECKED_CAST")
    private fun getCollidedFileNames(anyObject: Any?): MutableList<String> {
        val result: MutableList<String> = ArrayList()

        if (anyObject is DecryptedFolderMetadataFileV1) {
            for (file in anyObject.files.values) {
                result.add(file.encrypted.filename)
            }
        } else if (anyObject is DecryptedFolderMetadataFile) {
            val files: MutableMap<String?, DecryptedFile> =
                anyObject.metadata.files as MutableMap<String?, DecryptedFile>
            for (file in files.values) {
                result.add(file.filename)
            }
        }

        return result
    }

    private fun getEncryptedFileName(anyObject: Any): String {
        var encryptedFileName = EncryptionUtils.generateUid()

        if (anyObject is DecryptedFolderMetadataFileV1) {
            while (anyObject.files[encryptedFileName] != null) {
                encryptedFileName = EncryptionUtils.generateUid()
            }
        } else {
            while ((anyObject as DecryptedFolderMetadataFile).metadata.files[encryptedFileName] != null) {
                encryptedFileName = EncryptionUtils.generateUid()
            }
        }

        return encryptedFileName
    }

    private fun setUploadOperationForE2E(
        token: String?,
        encryptedTempFile: File,
        encryptedFileName: String?,
        lastModifiedTimestamp: Long,
        creationTimestamp: Long,
        size: Long
    ) {
        if (size > ChunkedFileUploadRemoteOperation.CHUNK_SIZE_MOBILE) {
            val onWifiConnection = connectivityService.getConnectivity().isWifi

            uploadOperation = ChunkedFileUploadRemoteOperation(
                encryptedTempFile.absolutePath,
                file!!.getParentRemotePath() + encryptedFileName,
                file!!.mimeType,
                file!!.etagInConflict,
                lastModifiedTimestamp,
                onWifiConnection,
                token,
                creationTimestamp,
                disableRetries
            )
        } else {
            uploadOperation = UploadFileRemoteOperation(
                encryptedTempFile.absolutePath,
                file!!.getParentRemotePath() + encryptedFileName,
                file!!.mimeType,
                file!!.etagInConflict,
                lastModifiedTimestamp,
                creationTimestamp,
                token,
                disableRetries
            )
        }
    }

    @Throws(IOException::class)
    private fun initFileChannel(
        result: RemoteOperationResult<*>?,
        fileLock: FileLock?,
        e2eFiles: E2EFiles
    ): Triple<FileLock?, RemoteOperationResult<*>?, FileChannel?> {
        var result = result
        var fileLock = fileLock
        var channel: FileChannel? = null

        try {
            val randomAccessFile = RandomAccessFile(file!!.storagePath, "rw")
            channel = randomAccessFile.channel
            fileLock = channel.tryLock()
        } catch (ioException: IOException) {
            Log_OC.d(TAG, "Error caught at getChannelFromFile: $ioException")

            // this basically means that the file is on SD card
            // try to copy file to temporary dir if it doesn't exist
            val temporalPath = FileStorageUtils.getInternalTemporalPath(user.accountName, this.context) +
                file!!.remotePath
            file!!.setStoragePath(temporalPath)
            e2eFiles.temporalFile = File(temporalPath)

            Files.deleteIfExists(Paths.get(temporalPath))
            result = copy(e2eFiles.originalFile, e2eFiles.temporalFile!!)

            if (result.isSuccess) {
                if (e2eFiles.temporalFile!!.length() == e2eFiles.originalFile.length()) {
                    try {
                        RandomAccessFile(e2eFiles.temporalFile!!.absolutePath, "rw").use { randomAccessFile ->
                            channel = randomAccessFile.channel
                            fileLock = channel.tryLock()
                        }
                    } catch (e: IOException) {
                        Log_OC.d(TAG, "Error caught at getChannelFromFile: $e")
                    }
                } else {
                    result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCK_FAILED)
                }
            }
        }

        return Triple(fileLock, result, channel)
    }

    private fun getChannelSize(channel: FileChannel): Long = try {
        channel.size()
    } catch (_: IOException) {
        File(file!!.storagePath).length()
    }

    @Throws(OperationCancelledException::class)
    private fun performE2EUpload(data: E2EClientData): RemoteOperationResult<*> {
        for (mDataTransferListener in this.dataTransferListeners) {
            uploadOperation!!.addDataTransferProgressListener(mDataTransferListener)
        }

        if (cancellationRequested.get()) {
            throw OperationCancelledException()
        }

        var result = uploadOperation!!.execute(data.client)

        // move local temporal file or original file to its corresponding
        // location in the Nextcloud local folder
        if (!result.isSuccess && result.httpCode == HttpStatus.SC_PRECONDITION_FAILED) {
            result = RemoteOperationResult(RemoteOperationResult.ResultCode.SYNC_CONFLICT)
        }

        return result
    }

    @Throws(
        InvalidAlgorithmParameterException::class,
        NoSuchPaddingException::class,
        NoSuchAlgorithmException::class,
        InvalidKeyException::class,
        InvalidParameterSpecException::class,
        IOException::class
    )
    private fun getE2EData(anyObject: Any): E2EData {
        val key = EncryptionUtils.generateKey()
        val iv = EncryptionUtils.randomBytes(EncryptionUtils.ivLength)
        val cipher = EncryptionUtils.getCipher(Cipher.ENCRYPT_MODE, key, iv)
        val file = File(file!!.storagePath)
        val encryptedFile = EncryptionUtils.encryptFile(user.accountName, file, cipher)
        val encryptedFileName = getEncryptedFileName(anyObject)

        if (key == null) {
            throw NullPointerException("key cannot be null")
        }

        return E2EData(key, iv, encryptedFile, encryptedFileName)
    }

    @Throws(
        InvalidAlgorithmParameterException::class,
        UploadException::class,
        NoSuchPaddingException::class,
        IllegalBlockSizeException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        BadPaddingException::class,
        InvalidKeyException::class
    )
    private fun updateMetadataForE2E(
        anyObject: Any?,
        e2eData: E2EData,
        clientData: E2EClientData,
        e2eFiles: E2EFiles,
        arbitraryDataProvider: ArbitraryDataProvider?,
        encryptionUtilsV2: EncryptionUtilsV2,
        metadataExists: Boolean
    ) {
        val filename = File(file!!.remotePath).name
        file!!.decryptedRemotePath = e2eFiles.parentFile.getDecryptedRemotePath() + filename
        file!!.remotePath = e2eFiles.parentFile.remotePath + e2eData.encryptedFileName

        if (anyObject is DecryptedFolderMetadataFileV1) {
            updateMetadataForV1(
                anyObject,
                e2eData,
                clientData,
                e2eFiles.parentFile,
                arbitraryDataProvider,
                metadataExists
            )
        } else if (anyObject is DecryptedFolderMetadataFile) {
            updateMetadataForV2(
                anyObject,
                encryptionUtilsV2,
                e2eData,
                clientData,
                e2eFiles.parentFile
            )
        }
    }

    @Throws(
        InvalidAlgorithmParameterException::class,
        NoSuchPaddingException::class,
        IllegalBlockSizeException::class,
        CertificateException::class,
        NoSuchAlgorithmException::class,
        BadPaddingException::class,
        InvalidKeyException::class,
        UploadException::class
    )
    private fun updateMetadataForV1(
        metadata: DecryptedFolderMetadataFileV1,
        e2eData: E2EData,
        clientData: E2EClientData,
        parentFile: OCFile,
        arbitraryDataProvider: ArbitraryDataProvider?,
        metadataExists: Boolean
    ) {
        val decryptedFile = com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFile()
        val data = Data()
        data.filename = file!!.getDecryptedFileName()
        data.mimetype = file!!.mimeType
        data.key = EncryptionUtils.encodeBytesToBase64String(e2eData.key)
        decryptedFile.encrypted = data
        decryptedFile.initializationVector = EncryptionUtils.encodeBytesToBase64String(e2eData.iv)
        decryptedFile.authenticationTag = e2eData.encryptedFile.authenticationTag

        metadata.files[e2eData.encryptedFileName] = decryptedFile

        val encryptedFolderMetadata =
            EncryptionUtils.encryptFolderMetadata(
                metadata,
                clientData.publicKey,
                parentFile.localId,
                user,
                arbitraryDataProvider
            )

        val serializedFolderMetadata: String? = if (metadata.metadata.getMetadataKey() != null) {
            EncryptionUtils.serializeJSON(encryptedFolderMetadata, true)
        } else {
            EncryptionUtils.serializeJSON(encryptedFolderMetadata)
        }

        // upload metadata
        EncryptionUtils.uploadMetadata(
            parentFile,
            serializedFolderMetadata,
            clientData.token,
            clientData.client,
            metadataExists,
            latestVersion(false),
            "",
            arbitraryDataProvider,
            user
        )
    }

    @Throws(UploadException::class)
    private fun updateMetadataForV2(
        metadata: DecryptedFolderMetadataFile,
        encryptionUtilsV2: EncryptionUtilsV2,
        e2eData: E2EData,
        clientData: E2EClientData,
        parentFile: OCFile
    ) {
        encryptionUtilsV2.addFileToMetadata(
            e2eData.encryptedFileName,
            this.file!!,
            e2eData.iv,
            e2eData.encryptedFile.authenticationTag,
            e2eData.key,
            metadata,
            storageManager
        )

        // upload metadata
        encryptionUtilsV2.serializeAndUploadMetadata(
            parentFile,
            metadata,
            clientData.token,
            clientData.client,
            true,
            this.context,
            user,
            storageManager
        )
    }

    private fun completeE2EUpload(result: RemoteOperationResult<*>, e2eFiles: E2EFiles, client: OwnCloudClient?) {
        if (result.isSuccess) {
            handleLocalBehaviour(e2eFiles.temporalFile, e2eFiles.expectedFile!!, e2eFiles.originalFile, client)
        } else if (result.code.isConflict()) {
            storageManager.saveConflict(this.file, file!!.etagInConflict)
        }

        e2eFiles.deleteTemporalFile()
    }

    private fun cleanupE2EUpload(
        fileLock: FileLock?,
        channel: FileChannel?,
        e2eFiles: E2EFiles,
        result: RemoteOperationResult<*>?,
        anyObject: Any?,
        client: OwnCloudClient,
        token: String?
    ): RemoteOperationResult<*> {
        var result = result
        uploadStarted.set(false)

        if (fileLock != null) {
            try {
                // Only release if the channel is still open/valid
                if (channel != null && channel.isOpen) {
                    fileLock.release()
                }
            } catch (_: IOException) {
                Log_OC.e(TAG, "Failed to unlock file with path " + file!!.storagePath)
            }
        }

        if (channel != null) {
            try {
                channel.close()
            } catch (e: IOException) {
                Log_OC.e(TAG, "Failed to close file channel", e)
            }
        }

        e2eFiles.deleteTemporalFileWithOriginalFileComparison()

        if (result == null) {
            result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
        }

        logResult(result, file!!.storagePath, file!!.remotePath)

        if (token.isNullOrEmpty()) {
            Log_OC.e(
                TAG,
                "CRITICAL ERROR: Folder was locked but token is null/empty. Cannot unlock! " +
                    "Folder: " + e2eFiles.parentFile.fileName
            )
            val tokenError = RemoteOperationResult<Void?>(
                IllegalStateException("Folder locked but token lost - manual intervention may be required")
            )

            // Override result only if original operation succeeded
            if (result.isSuccess) {
                result = tokenError
            }
            return result
        }

        // Unlock must be done otherwise folder stays locked and user can't upload any file
        var unlockFolderResult: RemoteOperationResult<Void?>?
        try {
            unlockFolderResult = if (anyObject is DecryptedFolderMetadataFileV1) {
                EncryptionUtils.unlockFolderV1(e2eFiles.parentFile, client, token)
            } else {
                EncryptionUtils.unlockFolder(e2eFiles.parentFile, client, token)
            }
        } catch (e: Exception) {
            Log_OC.e(TAG, "CRITICAL ERROR: Exception during folder unlock", e)
            unlockFolderResult = RemoteOperationResult<Void?>(e)
        }

        if (unlockFolderResult != null && !unlockFolderResult.isSuccess) {
            result = unlockFolderResult
        }

        if (unlockFolderResult != null && unlockFolderResult.isSuccess) {
            Log_OC.d(TAG, "Folder successfully unlocked: " + e2eFiles.parentFile.fileName)

            if (duplicatedEncryptedFile != null) {
                instance().removeDuplicatedFile(duplicatedEncryptedFile!!, client, user) {
                    duplicatedEncryptedFile = null
                }
            }
        }

        e2eFiles.deleteEncryptedTempFile()

        return result
    }

    // endregion
    private fun checkConditions(originalFile: File): RemoteOperationResult<*>? {
        var remoteOperationResult: RemoteOperationResult<*>? = null

        // check that connectivity conditions are met and delays the upload otherwise
        val connectivity = connectivityService.getConnectivity()
        if (this.isWifiRequired && (!connectivity.isWifi || connectivity.isMetered)) {
            Log_OC.d(TAG, "Upload delayed until WiFi is available: " + this.remotePath)
            remoteOperationResult = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.DELAYED_FOR_WIFI)
        }

        // check if charging conditions are met and delays the upload otherwise
        val battery = powerManagementService.battery
        if (this.isChargingRequired && !battery.isCharging) {
            Log_OC.d(TAG, "Upload delayed until the device is charging: " + this.remotePath)
            remoteOperationResult = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.DELAYED_FOR_CHARGING)
        }

        // check that device is not in power save mode
        if (!this.isIgnoringPowerSaveMode && powerManagementService.isPowerSavingEnabled) {
            Log_OC.d(TAG, "Upload delayed because device is in power save mode: " + this.remotePath)
            remoteOperationResult =
                RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.DELAYED_IN_POWER_SAVE_MODE)
        }

        // check if the file continues existing before schedule the operation
        if (!originalFile.exists()) {
            Log_OC.d(TAG, this.originalStoragePath + " does not exist anymore")
            remoteOperationResult = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND)
        }

        // check that internet is not behind walled garden
        if (!connectivityService.getConnectivity().isConnected || connectivityService.isInternetWalled()) {
            remoteOperationResult = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.NO_NETWORK_CONNECTION)
        }

        return remoteOperationResult
    }

    @Suppress("ReturnCount")
    private fun normalUpload(client: OwnCloudClient?): RemoteOperationResult<*> {
        var result: RemoteOperationResult<*>? = null
        var temporalFile: File? = null
        val originalFile = File(this.originalStoragePath)
        var expectedFile: File? = null

        try {
            Log_OC.d(TAG, "checking conditions")
            result = checkConditions(originalFile)
            if (result != null) {
                return result
            }

            val collisionResult = checkNameCollision(null, client, null, false)
            if (collisionResult != null) {
                Log_OC.e(TAG, "name collision detected")
                result = collisionResult
                return collisionResult
            }

            val expectedPath = FileStorageUtils.getDefaultSavePathFor(user.accountName, this.file)
            expectedFile = File(expectedPath)

            result = copyFile(originalFile, expectedPath)
            if (!result.isSuccess) {
                Log_OC.e(TAG, "file copying failed")
                return result
            }

            // Get the last modification date of the file from the file system
            val lastModifiedTimestamp = originalFile.lastModified() / 1000
            val creationTimestamp = getCreationTimestamp(originalFile)

            var filePath = Paths.get(file!!.storagePath)

            // file does not exist in storage
            if (!Files.exists(filePath)) {
                Log_OC.e(TAG, "file not found exception: normal upload, probably file in sd card")
                val temporalPath = FileStorageUtils.getInternalTemporalPath(
                    user.accountName,
                    this.context
                ) +
                    file!!.remotePath
                file!!.setStoragePath(temporalPath)
                temporalFile = File(temporalPath)

                Files.deleteIfExists(Paths.get(temporalPath))
                result = copy(originalFile, temporalFile)

                if (!result.isSuccess) {
                    return result
                }

                if (temporalFile.length() != originalFile.length()) {
                    Log_OC.e(TAG, "temporal file and original file lengths are not same - result is LOCK_FAILED")
                    result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCK_FAILED)
                }
                filePath = temporalFile.toPath()
            }

            FileChannel.open(filePath, StandardOpenOption.READ).use { channel ->
                var fileLock: FileLock? = null
                try {
                    // request a shared lock instead of exclusive one, since we are just reading file
                    fileLock = channel.tryLock(0L, Long.MAX_VALUE, true)
                    Log_OC.d(TAG, "🔒" + "file locked")
                } catch (_: OverlappingFileLockException) {
                    Log_OC.e(TAG, "shared lock overlap detected; proceeding safely.")
                }

                // determine size
                var size: Long
                try {
                    size = channel.size()
                } catch (e: Exception) {
                    Log_OC.e(TAG, "failed to determine file size from channel: ", e)

                    try {
                        size = Files.size(filePath)
                    } catch (exception: Exception) {
                        Log_OC.e(TAG, "failed to determine file size from nio.File: ", exception)
                        result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.FILE_NOT_FOUND)
                        return result
                    }
                }

                val formattedFileSize = Formatter.formatFileSize(this.context, size)
                updateSize(size)
                Log_OC.d(TAG, "file size set to $formattedFileSize")

                // decide whether chunked or not
                if (size > ChunkedFileUploadRemoteOperation.CHUNK_SIZE_MOBILE) {
                    Log_OC.d(TAG, "chunked upload operation will be used")

                    val onWifiConnection = connectivityService.getConnectivity().isWifi
                    uploadOperation = ChunkedFileUploadRemoteOperation(
                        file!!.storagePath,
                        file!!.remotePath,
                        file!!.mimeType,
                        file!!.etagInConflict,
                        lastModifiedTimestamp,
                        creationTimestamp,
                        onWifiConnection,
                        disableRetries
                    )
                } else {
                    Log_OC.d(TAG, "upload file operation will be used")

                    uploadOperation = UploadFileRemoteOperation(
                        file!!.storagePath,
                        file!!.remotePath,
                        file!!.mimeType,
                        file!!.etagInConflict,
                        lastModifiedTimestamp,
                        creationTimestamp,
                        disableRetries
                    )
                }

                Log_OC.d(TAG, "upload type operation determined")

                // Adds the onTransferProgress in FileUploadWorker
                // [FileUploadWorker.onTransferProgress]
                for (mDataTransferListener in this.dataTransferListeners) {
                    uploadOperation!!.addDataTransferProgressListener(mDataTransferListener)
                }

                if (cancellationRequested.get()) {
                    Log_OC.e(TAG, "upload operation cancelled")
                    throw OperationCancelledException()
                }

                // execute
                if (result.isSuccess && uploadOperation != null) {
                    Log_OC.d(TAG, "upload operation completed")
                    result = uploadOperation!!.execute(client)
                }

                // move local temporal file or original file to its corresponding
                // location in the Nextcloud local folder
                if (!result.isSuccess && result.httpCode == HttpStatus.SC_PRECONDITION_FAILED) {
                    Log_OC.e(TAG, "upload operation failed with SC_PRECONDITION_FAILED")
                    result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.SYNC_CONFLICT)
                }
                if (fileLock != null && fileLock.isValid) {
                    fileLock.release()
                    Log_OC.d(TAG, "🔓" + "file lock released")
                }
            }
        } catch (_: FileNotFoundException) {
            Log_OC.e(TAG, "normal upload(): file not found exception")
            result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_FILE_NOT_FOUND)
        } catch (e: Exception) {
            Log_OC.e(TAG, "normal upload(): exception: ", e)
            result = RemoteOperationResult<Any?>(e)
        } finally {
            Log_OC.d(TAG, "normal upload(): finally block")

            uploadStarted.set(false)

            // clean up temporal file if it exists
            try {
                if (temporalFile != null) {
                    if (temporalFile.exists() && !temporalFile.delete()) {
                        Log_OC.e(TAG, "Could not delete temporal file")
                    }
                } else {
                    Log_OC.d(TAG, "temporal file is null - internal storage is used instead of sd-card")
                }
            } catch (e: Exception) {
                Log_OC.e(TAG, "an exception occurred during deletion of temporal file: ", e)
            }

            if (result == null) {
                Log_OC.e(TAG, "result is null, UNKNOWN_ERROR")
                result = RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.UNKNOWN_ERROR)
            }

            logResult(result, this.originalStoragePath, mRemotePath)
            uploadsStorageManager.updateDatabaseUploadResult(result, this)
        }

        if (result.isSuccess) {
            handleLocalBehaviour(temporalFile, expectedFile!!, originalFile, client)
        } else if (result.code == RemoteOperationResult.ResultCode.SYNC_CONFLICT) {
            storageManager.saveConflict(this.file, file!!.etagInConflict)
        }

        Log_OC.d(TAG, "returning normal upload() result")

        return result
    }

    private fun updateSize(size: Long) {
        val ocUpload = uploadsStorageManager.getUploadById(this.ocUploadId)
        if (ocUpload != null) {
            ocUpload.fileSize = size
            uploadsStorageManager.updateUpload(ocUpload)
        }
    }

    private fun logResult(result: RemoteOperationResult<*>, sourcePath: String?, targetPath: String?) {
        if (result.isSuccess) {
            Log_OC.i(TAG, "Upload of " + sourcePath + " to " + targetPath + ": " + result.logMessage)
        } else {
            if (result.exception != null) {
                if (result.isCancelled) {
                    Log_OC.w(
                        TAG,
                        ("Upload of " + sourcePath + " to " + targetPath + ": " + result.logMessage)
                    )
                } else {
                    Log_OC.e(
                        TAG,
                        (
                            "Upload of " + sourcePath + " to " + targetPath + ": " +
                                result.logMessage
                            ),
                        result.exception
                    )
                }
            } else {
                Log_OC.e(TAG, "Upload of " + sourcePath + " to " + targetPath + ": " + result.logMessage)
            }
        }
    }

    @Throws(OperationCancelledException::class, IOException::class)
    private fun copyFile(originalFile: File, expectedPath: String?): RemoteOperationResult<*> {
        if (this.localBehaviour == FileUploadWorker.LOCAL_BEHAVIOUR_COPY && this.originalStoragePath != expectedPath) {
            val temporalPath = FileStorageUtils.getInternalTemporalPath(user.accountName, this.context) +
                file!!.remotePath
            file!!.setStoragePath(temporalPath)
            val temporalFile = File(temporalPath)

            return copy(originalFile, temporalFile)
        }

        if (cancellationRequested.get()) {
            throw OperationCancelledException()
        }

        return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
    }

    @CheckResult
    @Suppress("ReturnCount")
    @Throws(OperationCancelledException::class)
    private fun checkNameCollision(
        parentFile: OCFile?,
        client: OwnCloudClient?,
        fileNames: MutableList<String>?,
        encrypted: Boolean
    ): RemoteOperationResult<*>? {
        Log_OC.d(TAG, "Checking name collision in server")

        val isFileExists: Boolean = existsFile(client, mRemotePath, fileNames, encrypted)

        if (isFileExists) {
            when (nameCollisionPolicy) {
                NameCollisionPolicy.SKIP -> {
                    Log_OC.d(TAG, "user choose to skip upload if same file exists")
                    return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
                }

                NameCollisionPolicy.RENAME -> {
                    mRemotePath = getNewAvailableRemotePath(client, mRemotePath, fileNames, encrypted)
                    wasRenamed = true
                    createNewOCFile(mRemotePath)
                    Log_OC.d(TAG, "File renamed as $mRemotePath")
                    if (renameUploadListener != null) {
                        renameUploadListener!!.onRenameUpload()
                    }
                }

                NameCollisionPolicy.OVERWRITE -> {
                    if (parentFile != null && encrypted) {
                        duplicatedEncryptedFile = storageManager.findDuplicatedFile(parentFile, this.file)
                    }

                    Log_OC.d(TAG, "Overwriting file")
                }

                NameCollisionPolicy.ASK_USER -> {
                    Log_OC.d(TAG, "Name collision; asking the user what to do")

                    // check if its real SYNC_CONFLICT
                    var isSameFileOnRemote = false
                    if (this.file != null) {
                        isSameFileOnRemote = instance()
                            .isSameFileOnRemote(user, file!!.storagePath, mRemotePath, this.context)
                    }

                    return if (isSameFileOnRemote) {
                        RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
                    } else {
                        RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.SYNC_CONFLICT)
                    }
                }
            }
        }

        if (cancellationRequested.get()) {
            throw OperationCancelledException()
        }

        return null
    }

    private fun deleteNonExistingFile(file: File) {
        if (file.exists()) {
            return
        }

        Log_OC.d(TAG, "deleting non-existing file from upload list and file list")

        uploadsStorageManager.removeUpload(this.ocUploadId)

        // some chunks can be uploaded and can still exist in db thus we have to remove it as well
        storageManager.removeFile(this.file, true, true)
    }

    private fun handleLocalBehaviour(
        temporalFile: File?,
        expectedFile: File,
        originalFile: File,
        client: OwnCloudClient?
    ) {
        // only LOCAL_BEHAVIOUR_COPY not using original file

        if (this.localBehaviour != FileUploadWorker.LOCAL_BEHAVIOUR_COPY) {
            // if file is not exists we should only delete from our app
            deleteNonExistingFile(originalFile)
        }

        Log_OC.d(TAG, "handling local behaviour for: " + originalFile.name + " behaviour: " + this.localBehaviour)

        when (this.localBehaviour) {
            FileUploadWorker.LOCAL_BEHAVIOUR_DELETE -> {
                Log_OC.d(TAG, "DELETE local behaviour will be handled")
                try {
                    Files.delete(originalFile.toPath())
                } catch (e: IOException) {
                    Log_OC.e(TAG, "Could not delete original file: " + originalFile.absolutePath, e)
                }
                file!!.setStoragePath("")
                storageManager.deleteFileInMediaScan(originalFile.absolutePath)
                saveUploadedFile(client)
            }

            FileUploadWorker.LOCAL_BEHAVIOUR_COPY -> {
                Log_OC.d(TAG, "COPY local behaviour will be handled")
                if (temporalFile != null) {
                    try {
                        move(temporalFile, expectedFile)
                    } catch (e: IOException) {
                        Log_OC.e(TAG, e.message)

                        // handling non-existing file for local copy as well
                        deleteNonExistingFile(temporalFile)
                    }
                } else {
                    try {
                        copy(originalFile, expectedFile)
                    } catch (e: IOException) {
                        Log_OC.e(TAG, e.message)
                    }
                }
                file!!.setStoragePath(expectedFile.absolutePath)
                saveUploadedFile(client)
                if (MimeTypeUtil.isMedia(file!!.mimeType)) {
                    FileDataStorageManager.triggerMediaScan(expectedFile.absolutePath)
                }
            }

            FileUploadWorker.LOCAL_BEHAVIOUR_MOVE -> {
                Log_OC.d(TAG, "MOVE local behaviour will be handled")
                val expectedPath = FileStorageUtils.getDefaultSavePathFor(user.accountName, this.file)
                val newFile = File(expectedPath)

                try {
                    move(originalFile, newFile)
                } catch (e: IOException) {
                    Log_OC.e(TAG, "Error moving file", e)
                }
                storageManager.deleteFileInMediaScan(originalFile.absolutePath)
                file!!.setStoragePath(newFile.absolutePath)
                saveUploadedFile(client)
                if (MimeTypeUtil.isMedia(file!!.mimeType)) {
                    FileDataStorageManager.triggerMediaScan(newFile.absolutePath)
                }
            }

            else -> {
                Log_OC.d(TAG, "DEFAULT local behaviour will be handled")
                file!!.setStoragePath("")
                saveUploadedFile(client)
            }
        }
    }

    private val capabilities: OCCapability
        get() = CapabilityUtils.getCapability(this.context)

    /**
     * Checks the existence of the folder where the current file will be uploaded both in the remote server and in the
     * local database.
     *
     *
     * If the upload is set to enforce the creation of the folder, the method tries to create it both remote and
     * locally.
     *
     * @param pathToGrant Full remote path whose existence will be granted.
     * @return An [OCFile] instance corresponding to the folder where the file will be uploaded.
     */
    private fun grantFolderExistence(pathToGrant: String, client: OwnCloudClient?): RemoteOperationResult<*> {
        val operation = ExistenceCheckRemoteOperation(pathToGrant, false)
        var result = operation.execute(client)
        if (!result.isSuccess && result.code == RemoteOperationResult.ResultCode.FILE_NOT_FOUND &&
            remoteFolderToBeCreated
        ) {
            val syncOp: SyncOperation = CreateFolderOperation(pathToGrant, user, this.context, storageManager)
            result = syncOp.execute(client)
        }
        if (result.isSuccess) {
            var parentDir = storageManager.getFileByPath(pathToGrant)
            if (parentDir == null) {
                parentDir = createLocalFolder(pathToGrant)
            }
            result = if (parentDir != null) {
                RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
            } else {
                RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.CANNOT_CREATE_FILE)
            }
        }
        return result
    }

    private fun createLocalFolder(remotePath: String): OCFile? {
        var parentPath = File(remotePath).parent
        parentPath =
            if (parentPath!!.endsWith(OCFile.PATH_SEPARATOR)) parentPath else parentPath + OCFile.PATH_SEPARATOR
        var parent = storageManager.getFileByPath(parentPath)
        if (parent == null) {
            parent = createLocalFolder(parentPath)
        }
        if (parent != null) {
            val createdFolder = OCFile(remotePath)
            createdFolder.mimeType = MimeType.DIRECTORY
            createdFolder.parentId = parent.fileId
            storageManager.saveFile(createdFolder)
            return createdFolder
        }
        return null
    }

    /**
     * Create a new OCFile mFile with new remote path. This is required if nameCollisionPolicy==RENAME. New file is
     * stored as mFile, original as mOldFile.
     *
     * @param newRemotePath new remote path
     */
    private fun createNewOCFile(newRemotePath: String?) {
        // a new OCFile instance must be created for a new remote path
        val newFile = OCFile(newRemotePath)
        newFile.creationTimestamp = file!!.creationTimestamp
        newFile.fileLength = file!!.fileLength
        newFile.mimeType = file!!.mimeType
        newFile.modificationTimestamp = file!!.modificationTimestamp
        newFile.modificationTimestampAtLastSyncForData = file!!.modificationTimestampAtLastSyncForData
        newFile.etag = file!!.etag
        newFile.lastSyncDateForProperties = file!!.lastSyncDateForProperties
        newFile.lastSyncDateForData = file!!.lastSyncDateForData
        newFile.setStoragePath(file!!.storagePath)
        newFile.parentId = file!!.parentId
        this.oldFile = this.file
        this.file = newFile
    }

    /**
     * Cancels the current upload process.
     *
     *
     *
     * Behavior depends on the current state of the upload:
     *
     *  * **Upload in preparation:** Upload will not start and a cancellation flag is set.
     *  * **Upload in progress:** The ongoing upload operation is cancelled via
     * [UploadFileRemoteOperation.cancel].
     *  * **No upload operation:** A cancellation flag is still set, but this situation is unexpected
     * and logged as an error.
     *
     *
     *
     *
     * Once cancelled, the database will be updated through
     * [UploadsStorageManager.updateDatabaseUploadResult].
     *
     * @param cancellationReason the reason for cancellation
     */
    fun cancel(cancellationReason: RemoteOperationResult.ResultCode?) {
        if (uploadOperation != null) {
            // Cancel an active upload
            Log_OC.d(TAG, "Cancelling upload during actual upload operation.")
            uploadOperation!!.cancel(cancellationReason)
        } else {
            // Cancel while preparing or when no upload exists
            cancellationRequested.set(true)
            if (uploadStarted.get()) {
                Log_OC.d(TAG, "Cancelling upload during preparation.")
            } else {
                Log_OC.e(TAG, "No upload in progress. This should not happen.")
            }
        }
    }

    /**
     * As soon as this method return true, upload can be canceled via cancel().
     */
    val isUploadInProgress: Boolean
        get() = uploadStarted.get()

    /**
     * TODO rewrite with homogeneous fail handling, remove dependency on [RemoteOperationResult],
     * TODO use Exceptions instead
     *
     * @param sourceFile Source file to copy.
     * @param targetFile Target location to copy the file.
     * @return [RemoteOperationResult]
     * @throws IOException exception if file cannot be accessed
     */
    @Suppress("ReturnCount")
    @Throws(IOException::class)
    private fun copy(sourceFile: File, targetFile: File): RemoteOperationResult<*> {
        Log_OC.d(TAG, "Copying local file")

        if (FileStorageUtils.getUsableSpace() < sourceFile.length()) {
            // error when the file should be copied
            return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_STORAGE_FULL)
        } else {
            Log_OC.d(TAG, "Creating temporal folder")
            val temporalParent = targetFile.parentFile

            if (!temporalParent!!.mkdirs() && !temporalParent.isDirectory) {
                return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.CANNOT_CREATE_FILE)
            }

            Log_OC.d(TAG, "Creating temporal file")
            if (!targetFile.createNewFile() && !targetFile.isFile) {
                return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.CANNOT_CREATE_FILE)
            }

            Log_OC.d(TAG, "Copying file contents")
            var inStream: InputStream? = null
            var out: OutputStream? = null

            try {
                if (this.originalStoragePath != targetFile.absolutePath) {
                    // In case document provider schema as 'content://'
                    if (originalStoragePath.startsWith(UriUtils.URI_CONTENT_SCHEME)) {
                        val uri = this.originalStoragePath.toUri()
                        inStream = context.contentResolver.openInputStream(uri)
                    } else {
                        inStream = FileInputStream(sourceFile)
                    }
                    out = FileOutputStream(targetFile)
                    var nRead = 0
                    val buf = ByteArray(BYTE_ARRAY_COPY_BUFFER_SIZE)
                    while (!cancellationRequested.get() &&
                        (inStream!!.read(buf).also { nRead = it }) > -1
                    ) {
                        out.write(buf, 0, nRead)
                    }
                    out.flush()
                } // else: weird but possible situation, nothing to copy

                if (cancellationRequested.get()) {
                    return RemoteOperationResult<Any?>(OperationCancelledException())
                }
            } catch (_: Exception) {
                return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.LOCAL_STORAGE_NOT_COPIED)
            } finally {
                try {
                    inStream?.close()
                } catch (e: Exception) {
                    Log_OC.d(
                        TAG,
                        "Weird exception while closing input stream for " +
                            this.originalStoragePath + " (ignoring)",
                        e
                    )
                }
                try {
                    out?.close()
                } catch (e: Exception) {
                    Log_OC.d(
                        TAG,
                        "Weird exception while closing output stream for " +
                            targetFile.absolutePath + " (ignoring)",
                        e
                    )
                }
            }
        }
        return RemoteOperationResult<Any?>(RemoteOperationResult.ResultCode.OK)
    }

    /**
     * TODO rewrite with homogeneous fail handling, remove dependency on [RemoteOperationResult],
     * TODO     use Exceptions instead
     * TODO refactor both this and 'copy' in a single method
     *
     * @param sourceFile Source file to move.
     * @param targetFile Target location to move the file.
     * @throws IOException exception if file cannot be read/wrote
     */
    @Throws(IOException::class)
    private fun move(sourceFile: File, targetFile: File) {
        if (targetFile != sourceFile) {
            val expectedFolder = targetFile.parentFile
            Files.createDirectories(expectedFolder!!.toPath())

            if (expectedFolder.isDirectory) {
                if (!sourceFile.renameTo(targetFile)) {
                    // try to copy and then delete
                    Files.createFile(targetFile.toPath())
                    try {
                        FileInputStream(sourceFile).channel.use { inChannel ->
                            FileOutputStream(targetFile).channel.use { outChannel ->
                                inChannel.transferTo(0, inChannel.size(), outChannel)
                                Files.delete(sourceFile.toPath())
                            }
                        }
                    } catch (_: Exception) {
                        file!!.setStoragePath("") // forget the local file
                        // by now, treat this as a success; the file was uploaded
                        // the best option could be show a warning message
                    }
                }
            } else {
                file!!.setStoragePath("")
            }
        }
    }

    /**
     * Saves a OC File after a successful upload.
     *
     *
     * A PROPFIND is necessary to keep the props in the local database synchronized with the server, specially the
     * modification time and Etag (where available)
     */
    private fun saveUploadedFile(client: OwnCloudClient?) {
        var file = this.file
        if (file!!.fileExists()) {
            file = storageManager.getFileById(file.fileId)
        }
        if (file == null) {
            // this can happen e.g. when the file gets deleted during upload
            return
        }
        val syncDate = System.currentTimeMillis()
        file.lastSyncDateForData = syncDate

        // new PROPFIND to keep data consistent with server
        // in theory, should return the same we already have
        // TODO from the appropriate OC server version, get data from last PUT response headers, instead
        // TODO of a new PROPFIND; the latter may fail, specially for chunked uploads
        val path = if (encryptedAncestor) {
            file.getParentRemotePath() + file.getEncryptedFileName()
        } else {
            this.remotePath
        }

        val operation = ReadFileRemoteOperation(path)
        val result = operation.execute(client)
        if (result.isSuccess) {
            updateOCFile(
                file,
                (result.data[0] as RemoteFile?)!!
            )
            file.lastSyncDateForProperties = syncDate
        } else {
            Log_OC.e(TAG, "Error reading properties of file after successful upload; this is gonna hurt...")
        }

        if (wasRenamed) {
            val oldFile = storageManager.getFileByPath(oldFile!!.remotePath)
            if (oldFile != null) {
                oldFile.setStoragePath(null)
                storageManager.saveFile(oldFile)
                storageManager.saveConflict(oldFile, null)
            }
            // else: it was just an automatic renaming due to a name
            // coincidence; nothing else is needed, the storagePath is right
            // in the instance returned by mCurrentUpload.getFile()
        }
        file.isUpdateThumbnailNeeded = true
        storageManager.saveFile(file)
        storageManager.saveConflict(file, null)

        if (MimeTypeUtil.isMedia(file.mimeType)) {
            FileDataStorageManager.triggerMediaScan(file.storagePath, file)
        }

        // generate new Thumbnail
        val task =
            ThumbnailGenerationTask(storageManager, user)
        task.execute(ThumbnailGenerationTaskObject(file, file.remoteId))
    }

    private fun updateOCFile(file: OCFile, remoteFile: RemoteFile) {
        file.creationTimestamp = remoteFile.creationTimestamp
        file.fileLength = remoteFile.length
        file.mimeType = remoteFile.mimeType
        file.modificationTimestamp = remoteFile.modifiedTimestamp
        file.modificationTimestampAtLastSyncForData = remoteFile.modifiedTimestamp
        file.etag = remoteFile.etag
        file.remoteId = remoteFile.remoteId
        file.permissions = remoteFile.permissions
        file.uploadTimestamp = remoteFile.uploadTimestamp
    }

    interface OnRenameListener {
        fun onRenameUpload()
    }

    companion object {
        private val TAG: String = UploadFileOperation::class.java.simpleName

        const val BYTE_ARRAY_COPY_BUFFER_SIZE = 4096
        const val CREATED_BY_USER: Int = 0
        const val CREATED_AS_INSTANT_PICTURE: Int = 1
        const val CREATED_AS_INSTANT_VIDEO: Int = 2
        const val MISSING_FILE_PERMISSION_NOTIFICATION_ID: Int = 2501

        fun obtainNewOCFileToUpload(remotePath: String?, localPath: String?, mimeType: String?): OCFile {
            val newFile = OCFile(remotePath)
            newFile.setStoragePath(localPath)
            newFile.lastSyncDateForProperties = 0
            newFile.lastSyncDateForData = 0

            // size
            if (!TextUtils.isEmpty(localPath)) {
                val localFile = File(localPath)
                newFile.fileLength = localFile.length()
                newFile.lastSyncDateForData = localFile.lastModified()
            } // don't worry about not assigning size, the problems with localPath

            // are checked when the UploadFileOperation instance is created

            // MIME type
            if (TextUtils.isEmpty(mimeType)) {
                newFile.mimeType = MimeTypeUtil.getBestMimeTypeByFilename(localPath)
            } else {
                newFile.mimeType = mimeType
            }

            return newFile
        }

        /**
         * Returns a new and available (does not exist on the server) remotePath. This adds an incremental suffix.
         *
         * @param client     OwnCloud client
         * @param remotePath remote path of the file
         * @param fileNames  list of decrypted file names
         * @return new remote path
         */
        fun getNewAvailableRemotePath(
            client: OwnCloudClient?,
            remotePath: String,
            fileNames: MutableList<String>?,
            encrypted: Boolean
        ): String {
            val extPos = remotePath.lastIndexOf('.')
            var suffix: String
            var extension = ""
            var remotePathWithoutExtension = ""
            if (extPos >= 0) {
                extension = remotePath.substring(extPos + 1)
                remotePathWithoutExtension = remotePath.substring(0, extPos)
            }

            var count = 2
            var exists: Boolean
            var newPath: String

            // Causing infinite loop during tests due to ExistenceCheckRemoteOperation result
            do {
                suffix = " ($count)"
                newPath =
                    if (extPos >= 0) "$remotePathWithoutExtension$suffix.$extension" else remotePath + suffix
                exists = existsFile(client, newPath, fileNames, encrypted)
                count++
            } while (exists)

            return newPath
        }

        private fun existsFile(
            client: OwnCloudClient?,
            remotePath: String,
            fileNames: MutableList<String>?,
            encrypted: Boolean
        ): Boolean {
            if (encrypted) {
                val fileName = File(remotePath).name

                if (fileNames != null) {
                    for (name in fileNames) {
                        if (name.equals(fileName, ignoreCase = true)) {
                            return true
                        }
                    }
                }

                return false
            } else {
                val existsOperation = ExistenceCheckRemoteOperation(remotePath, false)
                val result = existsOperation.execute(client)
                return result.isSuccess
            }
        }
    }
}
