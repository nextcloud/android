/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022-2025 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2021 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2018-2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2012 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2011 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.datamodel

import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.RemoteException
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.nextcloud.android.lib.resources.files.FileDownloadLimit
import com.nextcloud.client.account.User
import com.nextcloud.client.database.NextcloudDatabase
import com.nextcloud.client.database.dao.CapabilityDao
import com.nextcloud.client.database.dao.FileDao
import com.nextcloud.client.database.dao.OfflineOperationDao
import com.nextcloud.client.database.dao.RecommendedFileDao
import com.nextcloud.client.database.dao.ShareDao
import com.nextcloud.client.database.entity.FileEntity
import com.nextcloud.client.database.entity.OfflineOperationEntity
import com.nextcloud.client.database.entity.model.ShareeKey
import com.nextcloud.client.database.entity.toEntity
import com.nextcloud.client.database.entity.toOCCapability
import com.nextcloud.client.jobs.offlineOperations.repository.OfflineOperationsRepository
import com.nextcloud.client.jobs.offlineOperations.repository.OfflineOperationsRepositoryType
import com.nextcloud.model.HTTPStatusCodes
import com.nextcloud.model.OfflineOperationRawType
import com.nextcloud.model.OfflineOperationType
import com.nextcloud.model.ShareeEntry
import com.nextcloud.utils.AppConstants
import com.nextcloud.utils.ResultParser.data
import com.nextcloud.utils.e2ee.E2EVersionHelper
import com.nextcloud.utils.extensions.createStoragePath
import com.nextcloud.utils.extensions.isTheSameAs
import com.nextcloud.utils.extensions.matchesMimeType
import com.nextcloud.utils.extensions.toEntity
import com.nextcloud.utils.extensions.toFile
import com.nextcloud.utils.extensions.toFileEntity
import com.nextcloud.utils.extensions.toFolderMetadataUpdate
import com.owncloud.android.MainApp
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.db.ProviderMeta.ProviderTableMeta
import com.owncloud.android.lib.common.OwnCloudClient
import com.owncloud.android.lib.common.network.WebdavEntry.MountType
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.ExistenceCheckRemoteOperation
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.lib.resources.files.model.FileLockType
import com.owncloud.android.lib.resources.files.model.GeoLocation
import com.owncloud.android.lib.resources.files.model.ImageDimension
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.files.model.ServerFileInterface
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.lib.resources.status.E2EVersion
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.tags.Tag
import com.owncloud.android.operations.RemoteOperationFailedException
import com.owncloud.android.operations.upload.RemoteFileExistence
import com.owncloud.android.utils.FileStorageUtils
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.MimeTypeUtil
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.util.Locale

@Suppress("LargeClass", "TooManyFunctions")
class FileDataStorageManager private constructor(
    @JvmField val user: User,
    val contentResolver: ContentResolver?,
    val contentProviderClient: ContentProviderClient?
) {
    constructor(user: User, contentResolver: ContentResolver?) : this(user, contentResolver, null)

    constructor(user: User, contentProviderClient: ContentProviderClient?) : this(user, null, contentProviderClient)

    val db: NextcloudDatabase = NextcloudDatabase.instance()
    val recommendedFileDao: RecommendedFileDao = db.recommendedFileDao()
    val offlineOperationDao: OfflineOperationDao = db.offlineOperationDao()
    val fileDao: FileDao = db.fileDao()
    val shareDao: ShareDao = db.shareDao()
    val capabilityDao: CapabilityDao = db.capabilityDao()

    private val gson = Gson()
    val offlineOperationsRepository: OfflineOperationsRepositoryType = OfflineOperationsRepository(this)

    val defaultRootPath: OCFile
        get() = OCFile(OCFile.ROOT_PATH)

    val rootFolder: OCFile
        get() = fileDao.getFileByEncryptedRemotePath(OCFile.ROOT_PATH, user.accountName)
            ?.let(::createFileInstance)
            ?: createRootDir()

    // region File lookup
    @Deprecated("Use getFileByEncryptedRemotePath() or getFileByDecryptedRemotePath()")
    fun getFileByPath(path: String?): OCFile? = getFileByEncryptedRemotePath(path)

    fun getFileByEncryptedRemotePath(path: String?): OCFile? =
        findFileByPath(path, fileDao::getFileByEncryptedRemotePath)

    fun getFileByDecryptedRemotePath(path: String?): OCFile? =
        findFileByPath(path, fileDao::getFileByDecryptedRemotePath)

    fun getFileByRemotePath(path: String?): OCFile? {
        if (path == null) return null

        return getFileByDecryptedRemotePath(path) ?: getFileByDecryptedRemotePath(path + OCFile.PATH_SEPARATOR)
    }

    private fun findFileByPath(path: String?, query: (String, String) -> FileEntity?): OCFile? {
        if (path == null) return null

        val fileEntity = query(path, user.accountName)
        if (fileEntity != null) return createFileInstance(fileEntity)
        if (OCFile.ROOT_PATH == path) return createRootDir()

        return null
    }

    fun getFileById(id: Long): OCFile? = fileDao.getFileById(id)?.let(::createFileInstance)

    fun getFileByLocalId(localId: Long): OCFile? = fileDao.getFileByLocalId(localId)?.let(::createFileInstance)

    fun getFileByLocalPath(path: String): OCFile? =
        fileDao.getFileByLocalPath(path, user.accountName)?.let(::createFileInstance)

    fun getFileByRemoteId(remoteId: String): OCFile? =
        fileDao.getFileByRemoteId(remoteId, user.accountName)?.let(::createFileInstance)

    fun fileExists(id: Long): Boolean = fileDao.getFileById(id) != null

    fun fileExists(path: String): Boolean = fileDao.getFileByEncryptedRemotePath(path, user.accountName) != null

    fun getTopParent(file: OCFile): OCFile? = getFileById(getTopParentId(file))

    tailrec fun getTopParentId(file: OCFile): Long {
        val parentFile = getFileById(file.parentId) ?: return file.fileId
        if (parentFile.parentId == ROOT_PARENT_ID.toLong()) return file.fileId

        return getTopParentId(parentFile)
    }

    fun getFileEntity(file: OCFile?): FileEntity? = file?.let { fileDao.getFileById(it.fileId) }

    fun updateFileEntity(entity: FileEntity) {
        fileDao.update(entity)
    }

    fun getDecryptedPath(file: OCFile): String {
        val paths = mutableListOf<String>()
        var entity = fileDao.getFileByEncryptedRemotePath(file.remotePath, user.accountName)

        while (entity != null) {
            entity.name?.takeIf { it.isNotEmpty() }?.let { paths.add(it.removePrefix(OCFile.PATH_SEPARATOR)) }
            entity = entity.parent?.let { fileDao.getFileById(it) } ?: break
        }

        return paths.reversed().joinToString(OCFile.PATH_SEPARATOR)
    }

    fun searchFilesByName(file: OCFile, accountName: String, query: String): List<OCFile> =
        fileDao.searchFilesInFolder(file.fileId, accountName, query).map(::createFileInstance)
    // endregion

    // region Offline operations
    fun addCreateFileOfflineOperation(localPaths: Array<String>, remotePaths: Array<String>) {
        if (localPaths.size != remotePaths.size) {
            Log_OC.d(TAG, "Local path and remote path size do not match")
            return
        }

        localPaths.zip(remotePaths).forEach { (localPath, remotePath) ->
            queueCreateFileOperation(localPath, remotePath)
        }
    }

    private fun queueCreateFileOperation(localPath: String, remotePath: String) {
        val mimeType = MimeTypeUtil.getMimeTypeFromPath(remotePath)
        val createdAt = System.currentTimeMillis()
        val operationType = OfflineOperationType.CreateFile(
            OfflineOperationRawType.CreateFile.name,
            localPath,
            remotePath,
            mimeType
        )

        addCreateFolderOfflineOperationsForMissingParents(remotePath)

        val parentId = getFileByDecryptedRemotePath(FileStorageUtils.getParentPath(remotePath))?.fileId
        offlineOperationDao.insert(
            createOfflineOperation(remotePath, operationType, File(remotePath).name, parentId, createdAt)
        )

        createPendingFile(remotePath, mimeType, createdAt, createdAt, localPath)
    }

    private fun addCreateFolderOfflineOperationsForMissingParents(remotePath: String) {
        val parentPath = FileStorageUtils.getParentPath(remotePath)
        if (parentPath == null || OCFile.ROOT_PATH == parentPath) return
        if (getFileByEncryptedRemotePath(parentPath) != null) return

        addCreateFolderOfflineOperationsForMissingParents(parentPath)

        val grandParentId = FileStorageUtils.getParentPath(parentPath)
            ?.let { getFileByEncryptedRemotePath(it) }
            ?.fileId

        addCreateFolderOfflineOperation(parentPath, File(parentPath).name, grandParentId)
    }

    fun getOfflineEntityFromOCFile(file: OCFile): OfflineOperationEntity? =
        offlineOperationDao.getByPath(file.decryptedRemotePath)

    fun addCreateFolderOfflineOperation(
        path: String,
        filename: String?,
        parentOCFileId: Long?
    ): OfflineOperationEntity {
        val createdAt = System.currentTimeMillis()
        val operationType = OfflineOperationType.CreateFolder(OfflineOperationRawType.CreateFolder.name, path)
        val entity = createOfflineOperation(path, operationType, filename, parentOCFileId, createdAt)

        offlineOperationDao.insert(entity)
        createPendingDirectory(path, createdAt, createdAt)

        return entity
    }

    fun createPendingFile(
        remotePath: String,
        mimeType: String?,
        createdAt: Long,
        modificationTimestamp: Long,
        localPath: String?
    ) {
        val existingFile = getFileByRemotePath(remotePath)
        if (existingFile.isTheSameAs(localPath.toFile())) {
            Log_OC.i(
                TAG,
                "Creating pendingFile for an already uploaded file: keeping metadata to avoid triggering a conflict"
            )
            return
        }

        savePendingFile(remotePath, mimeType, createdAt, modificationTimestamp)
    }

    fun createPendingDirectory(path: String, createdAt: Long, modificationTimestamp: Long) {
        savePendingFile(path, MimeType.DIRECTORY, createdAt, modificationTimestamp)
    }

    private fun savePendingFile(remotePath: String, mimeType: String?, createdAt: Long, modifiedAt: Long) {
        val file = OCFile(remotePath)
        file.mimeType = mimeType
        file.creationTimestamp = createdAt
        file.modificationTimestamp = modifiedAt
        file.permissions = getParentPermissions(remotePath)

        saveFileWithParent(file, MainApp.getAppContext())
    }

    private fun getParentPermissions(path: String): String? = FileStorageUtils.getParentPath(path)
        ?.let { getFileByDecryptedRemotePath(it) }
        ?.permissions

    fun deleteOfflineOperation(file: OCFile) {
        offlineOperationsRepository.deleteOperation(file)
    }

    fun addRenameFileOfflineOperation(file: OCFile, newName: String) {
        val path = file.decryptedRemotePath
        deletePendingRenameOperations(path)

        val operationType =
            OfflineOperationType.RenameFile(OfflineOperationRawType.RenameFile.name, file.fileId, newName)

        offlineOperationDao.insert(
            createOfflineOperation(path, operationType, newName, file.parentId, System.currentTimeMillis())
        )
    }

    fun addRemoveFileOfflineOperation(file: OCFile) {
        val path = file.decryptedRemotePath
        deletePendingRenameOperations(path)

        val operationType = OfflineOperationType.RemoveFile(OfflineOperationRawType.RemoveFile.name, path)

        offlineOperationDao.insert(
            createOfflineOperation(path, operationType, file.fileName, file.parentId, System.currentTimeMillis())
        )
    }

    private fun deletePendingRenameOperations(path: String) {
        offlineOperationDao.getAllByPath(path)
            .filter { it.type is OfflineOperationType.RenameFile }
            .forEach { offlineOperationDao.delete(it) }
    }

    private fun createOfflineOperation(
        path: String?,
        operationType: OfflineOperationType,
        filename: String?,
        parentOCFileId: Long?,
        createdAt: Long
    ) = OfflineOperationEntity(
        parentOCFileId = parentOCFileId,
        path = path,
        type = operationType,
        filename = filename,
        createdAt = createdAt,
        modifiedAt = createdAt / AppConstants.MILLIS_PER_SECOND
    )

    fun getFileNameBasedOnEncryptionStatus(file: OCFile): String? {
        val entity = fileDao.getFileById(file.fileId) ?: return file.fileName

        return if (file.isEncrypted) entity.encryptedName else entity.name
    }

    fun getFilenameConsideringOfflineOperation(file: OCFile): String {
        val operationType = offlineOperationDao.getByPath(file.decryptedRemotePath)?.type
        val renameOperation = operationType as? OfflineOperationType.RenameFile

        return renameOperation?.newName ?: file.decryptedFileName
    }

    fun renameOfflineOperation(file: OCFile, newFolderName: String) {
        val entity = offlineOperationDao.getByPath(file.decryptedRemotePath) ?: return
        val parentFolder = getFileById(file.parentId) ?: return
        val parentPath = parentFolder.decryptedRemotePath
        val pathSuffix = if (file.isFolder) OCFile.PATH_SEPARATOR else ""
        val newPath = parentPath + newFolderName + pathSuffix

        when (val operationType = entity.type) {
            is OfflineOperationType.CreateFolder -> operationType.path = newPath

            is OfflineOperationType.CreateFile -> {
                operationType.remotePath = newPath
                operationType.mimeType = file.mimeType
            }

            else -> Unit
        }

        entity.path = newPath
        entity.filename = newFolderName
        offlineOperationDao.update(entity)

        moveLocalFile(file, newPath, parentPath)
        offlineOperationsRepository.updateNextOperations(entity)
    }

    fun keepOfflineOperationAndServerFile(entity: OfflineOperationEntity, file: OCFile?, client: OwnCloudClient) {
        if (file == null) return

        val oldFileName = entity.filename ?: return
        val parentRemotePath = file.parentRemotePath
        if (parentRemotePath.isNullOrEmpty()) return

        val newPath = getRemotePathForConflictResolution(client, parentRemotePath, oldFileName) ?: return

        offlineOperationsRepository.updateOperationForKeepBoth(entity, newPath)
    }

    fun isPendingOfflineCreation(file: OCFile): Boolean {
        if (!file.isOfflineOperation) return false

        val path = file.decryptedRemotePath ?: return false

        return offlineOperationDao.getByPath(path)?.isCreate() == true
    }
    // endregion

    // region Folder content
    fun getAllFilesRecursivelyInsideFolder(file: OCFile?): List<OCFile> {
        if (file == null || !file.fileExists()) return ArrayList()

        if (!file.isFolder) {
            return if (file.isAPKorAAB) ArrayList() else arrayListOf(file)
        }

        return getFolderContent(file.fileId, false).flatMapTo(ArrayList()) { item ->
            if (!item.isFolder && !item.isAPKorAAB) listOf(item) else getAllFilesRecursivelyInsideFolder(item)
        }
    }

    fun getFolderContent(ocFile: OCFile?, onlyOnDevice: Boolean): List<OCFile> {
        if (ocFile == null || !ocFile.isFolder || !ocFile.fileExists()) return ArrayList()

        return getFolderContent(ocFile.fileId, onlyOnDevice)
    }

    private fun getFolderContent(parentId: Long, onlyOnDevice: Boolean): List<OCFile> {
        Log_OC.d(TAG, "getFolderContent - start")

        val folderContent = fileDao.getFolderContent(parentId)
            .map(::createFileInstance)
            .filter { !onlyOnDevice || it.existsOnDevice() }

        Log_OC.d(TAG, "getFolderContent - finished")

        return folderContent
    }

    fun findDuplicatedFile(parentFolder: OCFile?, newFile: ServerFileInterface): OCFile? =
        getFolderContent(parentFolder, false).firstOrNull { it.fileName == newFile.fileName }

    fun getFolderImages(folder: OCFile?, onlyOnDevice: Boolean): List<OCFile> {
        if (folder == null) return ArrayList()

        return getFolderContent(folder, onlyOnDevice).filter { it.matchesMimeType(MimeTypeUtil::isImage) }
    }

    fun getFolderImagesAndVideos(folder: OCFile?, onlyOnDevice: Boolean): List<OCFile> {
        if (folder == null) return ArrayList()

        val mediaFileNames = HashSet<String?>()

        return getFolderContent(folder, onlyOnDevice).filter {
            val isImageOrVideo = it.matchesMimeType(MimeTypeUtil::isImage) || MimeTypeUtil.isVideo(it.mimeType)
            isImageOrVideo && mediaFileNames.add(it.fileName)
        }
    }

    fun getNonEncryptedSubfolders(id: Long, accountName: String): List<OCFile> =
        fileDao.getNonEncryptedSubfolders(id, accountName).map(::createFileInstance)

    val allFiles: List<OCFile>
        get() = fileDao.getAllFiles(user.accountName).map(::createFileInstance)

    val favoriteFiles: List<OCFile>
        get() = fileDao.getFavoriteFilesNonBlocking(user.accountName).map(::createFileInstance)

    fun getFilesWithSyncConflict(user: User): List<OCFile> =
        fileDao.getFilesWithSyncConflict(user.accountName).map(::createFileInstance)

    fun deleteAllFiles() {
        fileDao.deleteAllFiles(user.accountName)
    }
    // endregion

    // region Save file
    fun saveFile(ocFile: OCFile): Boolean {
        Log_OC.d(TAG, "saving file ${ocFile.fileName} into ${ocFile.remotePath}")

        val cv = createContentValuesForFile(ocFile)
        if (ocFile.isFolder) {
            cv.remove(ProviderTableMeta.FILE_ETAG)
            cv.remove(ProviderTableMeta.FILE_STORAGE_PATH)

            if (isInternalTwoWaySyncAndNeverSynced(ocFile)) {
                ocFile.createStoragePath(user.accountName)
            }
        }

        val existingIdForPath = fileDao.getFileByEncryptedRemotePath(ocFile.remotePath, user.accountName)?.id
        if (existingIdForPath == null && !fileExists(ocFile.fileId)) {
            insertFile(ocFile, cv)
            return false
        }

        existingIdForPath?.let { ocFile.fileId = it }

        updateRows(ProviderTableMeta.CONTENT_URI, cv, ID_SELECTION, arrayOf(ocFile.fileId.toString())) {
            logException(FAILED_TO_INSERT_MSG, it)
        }

        return true
    }

    private fun insertFile(ocFile: OCFile, cv: ContentValues) {
        val resultUri = insertRow(ProviderTableMeta.CONTENT_URI_FILE, cv) {
            logException(FAILED_TO_INSERT_MSG, it)
        }

        resultUri?.let { ocFile.fileId = it.insertedRowId() }
    }

    private fun isInternalTwoWaySyncAndNeverSynced(folder: OCFile): Boolean =
        folder.internalFolderSyncTimestamp == INTERNAL_TWO_WAY_SYNC_NEVER_SYNCED

    @Throws(RemoteOperationFailedException::class)
    fun saveFileWithParent(ocFile: OCFile, context: Context): OCFile {
        val isParentMissing = ocFile.parentId == ROOT_PARENT_ID.toLong() && OCFile.ROOT_PATH != ocFile.remotePath
        if (isParentMissing) {
            ocFile.parentId = getOrFetchParentFolder(ocFile.remotePath, context).fileId
        }

        saveFile(ocFile)

        return ocFile
    }

    private fun getOrFetchParentFolder(remotePath: String, context: Context): OCFile {
        val parentPath = FileStorageUtils.getParentPath(remotePath)
        getFileByEncryptedRemotePath(parentPath)?.let { return it }

        Log_OC.d(TAG, "Parent not found locally, fetching: $parentPath")
        val remoteParent = FileStorageUtils.fillOCFile(fetchRemoteFolder(parentPath, context))

        return saveFileWithParent(remoteParent, context)
    }

    @Suppress("DEPRECATION")
    private fun fetchRemoteFolder(remotePath: String?, context: Context): RemoteFile {
        val result = ReadFileRemoteOperation(remotePath).execute(user, context)
        result.data(RemoteFile::class.java)?.let { return it }

        val message = "Cannot fetch folder: $remotePath / ${result.getLogMessage(context)}"
        Log_OC.e(TAG, message)

        val exception = result.exception ?: throw RemoteOperationFailedException(message)
        throw RemoteOperationFailedException(message, exception)
    }

    fun saveNewFile(newFile: OCFile) {
        val parentPath = File(newFile.remotePath).parent
        val remoteParentPath = if (parentPath.endsWith(OCFile.PATH_SEPARATOR)) {
            parentPath
        } else {
            parentPath + OCFile.PATH_SEPARATOR
        }
        val parent = requireNotNull(getFileByEncryptedRemotePath(remoteParentPath)) {
            "Saving a new file in an unexisting folder"
        }

        newFile.parentId = parent.fileId
        saveFile(newFile)
    }

    private fun createRootDir(): OCFile {
        val root = OCFile(OCFile.ROOT_PATH)
        root.mimeType = MimeType.DIRECTORY
        root.parentId = ROOT_PARENT_ID.toLong()
        saveFile(root)

        return root
    }

    fun updateE2EECounter(file: OCFile, metadata: DecryptedFolderMetadataFile) {
        updateE2EECounter(file, metadata.metadata.counter)
    }

    fun updateE2EECounter(file: OCFile, counter: Long) {
        Log_OC.d(TAG, "e2ee counter stored: $counter for ${file.decryptedRemotePath}")
        file.setE2eCounter(counter)

        saveFile(file)
    }
    // endregion

    // region Content values
    @SuppressFBWarnings("CE")
    private fun createContentValuesBase(fileOrFolder: OCFile) = ContentValues().apply {
        put(ProviderTableMeta.FILE_MODIFIED, fileOrFolder.modificationTimestamp)
        put(
            ProviderTableMeta.FILE_MODIFIED_AT_LAST_SYNC_FOR_DATA,
            fileOrFolder.modificationTimestampAtLastSyncForData
        )
        put(ProviderTableMeta.FILE_PARENT, fileOrFolder.parentId)
        put(ProviderTableMeta.FILE_UPLOADED, fileOrFolder.uploadTimestamp)
        put(ProviderTableMeta.FILE_CREATION, fileOrFolder.creationTimestamp)
        put(ProviderTableMeta.FILE_CONTENT_TYPE, fileOrFolder.mimeType)
        put(ProviderTableMeta.FILE_NAME, fileOrFolder.fileName)
        put(ProviderTableMeta.FILE_PATH, fileOrFolder.remotePath)
        put(ProviderTableMeta.FILE_PATH_DECRYPTED, fileOrFolder.decryptedRemotePath)
        put(ProviderTableMeta.FILE_ACCOUNT_OWNER, user.accountName)
        put(ProviderTableMeta.FILE_IS_ENCRYPTED, fileOrFolder.isEncrypted)
        put(ProviderTableMeta.FILE_IS_READ_ONLY, fileOrFolder.isReadOnly)
        put(ProviderTableMeta.FILE_LAST_SYNC_DATE, fileOrFolder.lastSyncDateForProperties)
        put(ProviderTableMeta.FILE_LAST_SYNC_DATE_FOR_DATA, fileOrFolder.lastSyncDateForData)
        put(ProviderTableMeta.FILE_ETAG, fileOrFolder.etag)
        put(ProviderTableMeta.FILE_ETAG_ON_SERVER, fileOrFolder.etagOnServer)
        put(ProviderTableMeta.FILE_SHARED_VIA_LINK, fileOrFolder.isSharedViaLink.toInt())
        put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, fileOrFolder.isSharedWithSharee.toInt())
        fileOrFolder.mountType?.let { put(ProviderTableMeta.FILE_MOUNT_TYPE, it.ordinal) }
        put(ProviderTableMeta.FILE_PERMISSIONS, fileOrFolder.permissions)
        put(ProviderTableMeta.FILE_REMOTE_ID, fileOrFolder.remoteId)
        put(ProviderTableMeta.FILE_LOCAL_ID, fileOrFolder.localId)
        put(ProviderTableMeta.FILE_FAVORITE, fileOrFolder.isFavorite)
        put(ProviderTableMeta.FILE_HIDDEN, fileOrFolder.shouldHide())
        put(ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT, fileOrFolder.unreadCommentsCount)
        put(ProviderTableMeta.FILE_OWNER_ID, fileOrFolder.ownerId)
        put(ProviderTableMeta.FILE_OWNER_DISPLAY_NAME, fileOrFolder.ownerDisplayName)
        put(ProviderTableMeta.FILE_NOTE, fileOrFolder.note)
        put(ProviderTableMeta.FILE_SHAREES, gson.toJson(fileOrFolder.sharees))
        put(ProviderTableMeta.FILE_TAGS, gson.toJson(fileOrFolder.tags))
        put(ProviderTableMeta.FILE_RICH_WORKSPACE, fileOrFolder.richWorkspace)
        put(ProviderTableMeta.FILE_INTERNAL_TWO_WAY_SYNC_TIMESTAMP, fileOrFolder.internalFolderSyncTimestamp)
        put(ProviderTableMeta.FILE_INTERNAL_TWO_WAY_SYNC_RESULT, fileOrFolder.internalFolderSyncResult)
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private fun createContentValuesForFile(file: OCFile) = createContentValuesBase(file).apply {
        put(ProviderTableMeta.FILE_CONTENT_LENGTH, file.fileLength)
        put(ProviderTableMeta.FILE_ENCRYPTED_NAME, file.encryptedFileName)
        put(ProviderTableMeta.FILE_STORAGE_PATH, file.storagePath)
        put(ProviderTableMeta.FILE_UPDATE_THUMBNAIL, file.isUpdateThumbnailNeeded)
        put(ProviderTableMeta.FILE_IS_DOWNLOADING, file.isDownloading)
        put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, file.etagInConflict)
        put(ProviderTableMeta.FILE_HAS_PREVIEW, file.isPreviewAvailable.toInt())
        put(ProviderTableMeta.FILE_LOCKED, file.isLocked)
        put(ProviderTableMeta.FILE_LOCK_TYPE, file.lockType?.value ?: NO_LOCK_TYPE)
        put(ProviderTableMeta.FILE_LOCK_OWNER, file.lockOwnerId)
        put(ProviderTableMeta.FILE_LOCK_OWNER_DISPLAY_NAME, file.lockOwnerDisplayName)
        put(ProviderTableMeta.FILE_LOCK_OWNER_EDITOR, file.lockOwnerEditor)
        put(ProviderTableMeta.FILE_LOCK_TIMESTAMP, file.lockTimestamp)
        put(ProviderTableMeta.FILE_LOCK_TIMEOUT, file.lockTimeout)
        put(ProviderTableMeta.FILE_LOCK_TOKEN, file.lockToken)
        put(ProviderTableMeta.FILE_METADATA_SIZE, gson.toJson(file.imageDimension))
        put(ProviderTableMeta.FILE_METADATA_GPS, gson.toJson(file.geoLocation))
        put(ProviderTableMeta.FILE_METADATA_LIVE_PHOTO, file.linkedFileIdForLivePhoto)
        put(ProviderTableMeta.FILE_E2E_COUNTER, file.e2eCounter)
    }

    private fun Boolean.toInt(): Int = if (this) 1 else 0
    // endregion

    // region File instance mapping
    fun createFileInstance(fileEntity: FileEntity): OCFile = OCFile(fileEntity.path).apply {
        decryptedRemotePath = fileEntity.pathDecrypted
        fileId = fileEntity.id ?: 0
        parentId = fileEntity.parent ?: 0
        mimeType = fileEntity.contentType
        storagePath = fileEntity.storagePath
        bindExistingLocalFolder()
        fileLength = fileEntity.contentLength ?: 0
        uploadTimestamp = fileEntity.uploaded ?: 0
        creationTimestamp = fileEntity.creation ?: 0
        modificationTimestamp = fileEntity.modified ?: 0
        modificationTimestampAtLastSyncForData = fileEntity.modifiedAtLastSyncForData ?: 0
        lastSyncDateForProperties = fileEntity.lastSyncDate ?: 0
        lastSyncDateForData = fileEntity.lastSyncDateForData ?: 0
        etag = fileEntity.etag
        etagOnServer = fileEntity.etagOnServer
        isSharedViaLink = fileEntity.sharedViaLink == 1
        isSharedWithSharee = fileEntity.sharedWithSharee == 1
        permissions = fileEntity.permissions
        remoteId = fileEntity.remoteId
        localId = fileEntity.localId
        isUpdateThumbnailNeeded = fileEntity.updateThumbnail == 1
        isDownloading = fileEntity.isDownloading == 1
        etagInConflict = fileEntity.etagInConflict
        isFavorite = fileEntity.favorite == 1
        isEncrypted = fileEntity.isEncrypted == 1
        isReadOnly = fileEntity.isReadOnly == 1
        fileEntity.mountType?.let { mountType = MountType.entries[it] }
        isPreviewAvailable = fileEntity.hasPreview == 1
        unreadCommentsCount = fileEntity.unreadCommentsCount ?: 0
        ownerId = fileEntity.ownerId
        ownerDisplayName = fileEntity.ownerDisplayName
        note = fileEntity.note
        richWorkspace = fileEntity.richWorkspace
        applyLockState(fileEntity)
        setLivePhoto(fileEntity.metadataLivePhoto)
        isHidden = fileEntity.hidden == 1
        setE2eCounter(fileEntity.e2eCounter)
        internalFolderSyncTimestamp = fileEntity.internalTwoWaySync ?: NO_INTERNAL_TWO_WAY_SYNC
        applyJsonFields(fileEntity)
    }

    private fun OCFile.bindExistingLocalFolder() {
        if (storagePath != null || !isFolder) return

        val localFolder = File(FileStorageUtils.getDefaultSavePathFor(user.accountName, this))
        if (!localFolder.exists()) return

        storagePath = localFolder.absolutePath
        lastSyncDateForData = localFolder.lastModified()
    }

    private fun OCFile.applyLockState(fileEntity: FileEntity) {
        isLocked = fileEntity.locked == 1

        val lockTypeValue = fileEntity.lockType ?: 0
        lockType = if (lockTypeValue != NO_LOCK_TYPE) FileLockType.fromValue(lockTypeValue) else null
        lockOwnerId = fileEntity.lockOwner
        lockOwnerDisplayName = fileEntity.lockOwnerDisplayName
        lockOwnerEditor = fileEntity.lockOwnerEditor
        lockTimestamp = fileEntity.lockTimestamp ?: 0
        lockTimeout = (fileEntity.lockTimeout ?: 0).toLong()
        lockToken = fileEntity.lockToken
    }

    private fun OCFile.applyJsonFields(fileEntity: FileEntity) {
        sharees = parseJsonArray(fileEntity.sharees, Array<ShareeUser>::class.java)
        tags = parseJsonArray(fileEntity.tags, Array<Tag>::class.java)

        val metadataSize = fileEntity.metadataSize
        if (!metadataSize.isNullOrEmpty() && !metadataSize.equals(JSON_NULL_STRING, ignoreCase = true)) {
            gson.fromJson(metadataSize, ImageDimension::class.java)?.let { imageDimension = it }
        }

        val metadataGPS = fileEntity.metadataGPS
        if (!metadataGPS.isNullOrEmpty() && metadataGPS != JSON_NULL_STRING) {
            gson.fromJson(metadataGPS, GeoLocation::class.java)?.let { geoLocation = it }
        }
    }

    private fun <T> parseJsonArray(json: String?, type: Class<Array<T>>): MutableList<T> {
        if (json.isNullOrEmpty() || json == JSON_NULL_STRING || json == JSON_EMPTY_ARRAY) return ArrayList()

        return try {
            gson.fromJson(json, type).toCollection(ArrayList())
        } catch (e: JsonSyntaxException) {
            ArrayList()
        }
    }
    // endregion

    // region Remove file/folder
    fun removeFile(ocFile: OCFile?, removeDBData: Boolean, removeLocalCopy: Boolean): Boolean {
        if (ocFile == null) {
            Log_OC.e(TAG, "oc file is null, cannot delete it")
            return false
        }

        if (ocFile.isFolder) {
            Log_OC.d(TAG, "deleting folder")
            return removeFolder(ocFile, removeDBData, removeLocalCopy)
        }

        if (removeDBData) {
            Log_OC.d(TAG, "deleting db data of file")
            val isDeleted = fileDao.deleteFileByRemotePath(user.accountName, ocFile.remotePath) > 0
            if (!isDeleted) return false
        }

        Log_OC.d(TAG, "deleting local copy of file")

        return removeLocalCopyIfNeeded(ocFile, removeLocalCopy, removeDBData)
    }

    private fun removeLocalCopyIfNeeded(ocFile: OCFile, removeLocalCopy: Boolean, removeDBData: Boolean): Boolean {
        if (!removeLocalCopy) {
            Log_OC.w(TAG, "removeLocalCopyIfNeeded: removeLocalCopy=false")
            return true
        }

        val localPath = ocFile.storagePath
        if (localPath.toFile() == null) {
            Log_OC.w(TAG, "removeLocalCopyIfNeeded: file does not exist -> skip")
            return true
        }

        if (ocFile.isFolder) {
            Log_OC.w(TAG, "removeLocalCopyIfNeeded: file is folder -> skip")
            return true
        }

        val expectedPath = FileStorageUtils.getDefaultSavePathFor(user.accountName, ocFile)
        if (!localPath.equals(expectedPath, ignoreCase = true)) {
            Log_OC.w(
                TAG,
                "removeLocalCopyIfNeeded: Path mismatch! Expected $expectedPath but found $localPath. " +
                    "Deleting only the file at the expected path."
            )
        }

        Log_OC.d(TAG, "removeLocalCopyIfNeeded: deleting local file -> $expectedPath")
        val isDeleted = deleteLocalFile(expectedPath)
        Log_OC.d(TAG, "removeLocalCopyIfNeeded: file deletion result=$isDeleted")
        if (!isDeleted) return false

        deleteFileInMediaScan(expectedPath)

        if (!removeDBData) {
            Log_OC.d(TAG, "removeLocalCopyIfNeeded: updating DB after local deletion")
            ocFile.storagePath = null
            saveFile(ocFile)
            saveConflict(ocFile, null)
        }

        return true
    }

    private fun deleteLocalFile(path: String): Boolean = try {
        File(path).delete()
    } catch (e: Exception) {
        Log_OC.e(TAG, "removeLocalCopyIfNeeded: deletion error: ", e)
        false
    }

    fun removeFolder(folder: OCFile?, removeDBData: Boolean, removeLocalContent: Boolean): Boolean {
        if (folder == null) {
            Log_OC.d(TAG, "removeFolder: folder is null")
            return false
        }

        if (!folder.isFolder) {
            Log_OC.d(TAG, "removeFolder: not a folder -> ${folder.remotePath}")
            return false
        }

        Log_OC.d(
            TAG,
            "removeFolder: start -> ${folder.remotePath} | removeDBData=$removeDBData | " +
                "removeLocalContent=$removeLocalContent"
        )

        var success = true

        if (removeDBData && folder.fileId != UNKNOWN_FILE_ID) {
            Log_OC.d(TAG, "removeFolder: removing from DB -> fileId=${folder.fileId}")
            success = fileDao.deleteFolderWithDescendants(user.accountName, folder.fileId) > 0
            Log_OC.d(TAG, "removeFolder: DB removal result=$success")
        }

        if (success && removeLocalContent) {
            Log_OC.d(TAG, "removeFolder: removing local content -> ${folder.storagePath}")
            success = removeLocalFolderContent(folder)
            Log_OC.d(TAG, "removeFolder: local removal result=$success")
        }

        Log_OC.d(TAG, "removeFolder: finished -> result=$success")

        return success
    }

    private fun removeLocalFolderContent(folder: OCFile): Boolean {
        val localFolderPath = FileStorageUtils.getDefaultSavePathFor(user.accountName, folder)
        val localFolder = File(localFolderPath)
        if (!localFolder.exists()) {
            Log_OC.d(TAG, "removeLocalFolder: local folder does not exist -> $localFolderPath")
            return true
        }

        Log_OC.d(TAG, "removeLocalFolder: start -> $localFolderPath")

        val files = getFolderContent(folder.fileId, false)
        Log_OC.d(TAG, "removeLocalFolder: found ${files.size} entries in DB")

        var success = files.all { removeLocalFolderEntry(it) }
        if (success) {
            Log_OC.d(TAG, "removeLocalFolder: deleting folder -> ${localFolder.absolutePath}")
            success = removeLocalFolder(localFolder)
            Log_OC.d(TAG, "removeLocalFolder: folder deletion result=$success")
        }

        Log_OC.d(TAG, "removeLocalFolder: finished -> result=$success")

        return success
    }

    private fun removeLocalFolderEntry(ocFile: OCFile): Boolean {
        if (ocFile.isFolder) {
            Log_OC.d(TAG, "removeLocalFolder: removing subfolder -> ${ocFile.remotePath}")
            val isRemoved = removeLocalFolderContent(ocFile)
            Log_OC.d(TAG, "removeLocalFolder: subfolder removal result=$isRemoved")
            return isRemoved
        }

        if (!ocFile.isDown) return true

        val storagePath = ocFile.storagePath
        Log_OC.d(TAG, "removeLocalFolder: deleting file -> $storagePath")
        val isDeleted = File(storagePath).delete()
        Log_OC.d(TAG, "removeLocalFolder: file deletion result=$isDeleted")

        if (isDeleted) {
            deleteFileInMediaScan(storagePath)
            ocFile.storagePath = null
            saveFile(ocFile)
        }

        return isDeleted
    }

    fun removeLocalFolder(localFolder: File?): Boolean {
        if (localFolder == null) {
            Log_OC.d(TAG, "removeLocalFolder(File): folder is null")
            return false
        }

        if (!localFolder.exists()) {
            Log_OC.d(TAG, "removeLocalFolder(File): folder does not exist -> ${localFolder.absolutePath}")
            return true
        }

        Log_OC.d(TAG, "removeLocalFolder(File): start -> ${localFolder.absolutePath}")

        val failedChild = localFolder.listFiles().orEmpty().firstOrNull { !removeLocalChild(it) }
        if (failedChild != null) {
            Log_OC.d(TAG, "removeLocalFolder(File): failed at -> ${failedChild.absolutePath}")
            return false
        }

        val isFolderDeleted = localFolder.delete()
        Log_OC.d(
            TAG,
            "removeLocalFolder(File): deleting folder -> ${localFolder.absolutePath} result=$isFolderDeleted"
        )

        return isFolderDeleted
    }

    private fun removeLocalChild(child: File): Boolean {
        if (child.isDirectory) return removeLocalFolder(child)

        val isDeleted = child.delete()
        Log_OC.d(TAG, "removeLocalFolder(File): deleting file -> ${child.absolutePath} result=$isDeleted")

        return isDeleted
    }

    fun removeLocalFiles(user: User, storageManager: FileDataStorageManager) {
        val tempDir = File(FileStorageUtils.getTemporalPath(user.accountName))
        val saveDir = File(FileStorageUtils.getSavePath(user.accountName))
        FileStorageUtils.deleteRecursively(tempDir, storageManager)

        FileStorageUtils.deleteRecursively(saveDir, storageManager)
    }
    // endregion

    // region Move/copy
    @Suppress("DEPRECATION")
    fun moveLocalFile(ocFile: OCFile?, targetPath: String, targetParentPath: String) {
        Log_OC.d(
            TAG,
            "moveLocalFile ==> ocFile: ${ocFile?.remotePath} targetPath: $targetPath " +
                "targetParentPath: $targetParentPath"
        )

        if (ocFile == null) {
            Log_OC.e(TAG, "moveLocalFile: file is null, skipping")
            return
        }

        if (!canMoveLocalFile(ocFile, targetPath)) return

        val targetParent = getFileByPath(targetParentPath)
        if (targetParent == null) {
            Log_OC.e(TAG, "moveLocalFile: target parent folder not found: $targetParentPath")
            return
        }

        if (!targetParent.isFolder) {
            Log_OC.e(TAG, "moveLocalFile: target parent is not a folder: $targetParentPath")
            return
        }

        val oldPath: String = ocFile.remotePath
        val accountName = user.accountName
        val defaultSavePath = FileStorageUtils.getSavePath(accountName)
        val originalMediaPaths = moveFilesInDb(oldPath, targetPath, defaultSavePath, targetParent.fileId, accountName)

        if (!moveLocalFiles(accountName, ocFile, defaultSavePath, targetPath)) return

        val oldStoragePrefixLength = (defaultSavePath + oldPath).length
        originalMediaPaths.forEach { originalMediaPath ->
            deleteFileInMediaScan(originalMediaPath)
            triggerMediaScan(defaultSavePath + targetPath + originalMediaPath.substring(oldStoragePrefixLength))
        }
    }

    private fun canMoveLocalFile(ocFile: OCFile, targetPath: String): Boolean {
        if (!ocFile.fileExists()) {
            Log_OC.e(TAG, "moveLocalFile: file does not exist, skipping")
            return false
        }

        if (OCFile.ROOT_PATH == ocFile.fileName) {
            Log_OC.w(TAG, "moveLocalFile: cannot move root path")
            return false
        }

        if (ocFile.remotePath == targetPath) {
            Log_OC.w(TAG, "moveLocalFile: source and target paths are identical, skipping")
            return false
        }

        return true
    }

    private fun moveFilesInDb(
        oldPath: String,
        targetPath: String,
        defaultSavePath: String,
        targetParentId: Long,
        accountName: String
    ): List<String> {
        val entities = fileDao.getFolderWithDescendants("$oldPath%", accountName).filter {
            isSameOrDescendantPath(it.path, oldPath)
        }
        val oldStoragePrefix = defaultSavePath + oldPath
        val newStoragePrefix = defaultSavePath + targetPath

        val originalMediaPaths = entities
            .filter { MimeTypeUtil.isMedia(it.contentType) && it.storagePath?.startsWith(oldStoragePrefix) == true }
            .mapNotNull { it.storagePath }

        val updated = entities.map { entity ->
            val currentPath = entity.path.orEmpty()
            val newPath = targetPath + currentPath.substring(oldPath.length)
            val storagePath = entity.storagePath
            entity.copy(
                path = newPath,
                pathDecrypted = if (entity.isEncrypted == 1) entity.pathDecrypted else newPath,
                storagePath = if (storagePath?.startsWith(oldStoragePrefix) == true) {
                    newStoragePrefix + storagePath.substring(oldStoragePrefix.length)
                } else {
                    storagePath
                },
                parent = if (currentPath == oldPath) targetParentId else entity.parent
            )
        }

        fileDao.updateAll(updated)

        return originalMediaPaths
    }

    private fun isSameOrDescendantPath(path: String?, ancestorPath: String): Boolean {
        if (path == null) return false
        if (path == ancestorPath) return true

        return ancestorPath.endsWith(OCFile.PATH_SEPARATOR) && path.startsWith(ancestorPath)
    }

    private fun moveLocalFiles(
        accountName: String,
        ocFile: OCFile,
        defaultSavePath: String,
        targetPath: String
    ): Boolean {
        val localFile = File(FileStorageUtils.getDefaultSavePathFor(accountName, ocFile))
        if (!localFile.exists()) {
            Log_OC.d(TAG, "moveLocalFile: no local file to move at ${localFile.absolutePath}")
            return false
        }

        val targetFile = File(defaultSavePath + targetPath)
        createParentFolder(targetFile, "moveLocalFile: failed to create parent folder ")

        if (!localFile.renameTo(targetFile)) {
            Log_OC.e(TAG, "moveLocalFile: failed to rename ${localFile.absolutePath} to ${targetFile.absolutePath}")
            return false
        }

        return true
    }

    fun copyLocalFile(ocFile: OCFile, targetPath: String?) {
        if (!ocFile.fileExists() || OCFile.ROOT_PATH == ocFile.fileName) return

        val localFile = File(FileStorageUtils.getDefaultSavePathFor(user.accountName, ocFile))
        val defaultSavePath = FileStorageUtils.getSavePath(user.accountName)
        val isCopied = localFile.exists() && copyToTarget(localFile, File(defaultSavePath + targetPath))

        Log_OC.d(TAG, "Local file COPIED : $isCopied")
    }

    private fun copyToTarget(localFile: File, targetFile: File): Boolean {
        createParentFolder(targetFile, "Unable to create parent folder ")

        return FileStorageUtils.copyFile(localFile, targetFile)
    }

    private fun createParentFolder(targetFile: File, errorMessage: String) {
        val targetFolder = targetFile.parentFile ?: return
        if (!targetFolder.exists() && !targetFolder.mkdirs()) {
            Log_OC.e(TAG, errorMessage + targetFolder.absolutePath)
        }
    }

    @Throws(RemoteException::class, OperationApplicationException::class)
    fun migrateStoredFiles(sourcePath: String, destinationPath: String) {
        val cursor = checkNotNull(queryStoredFiles())
        val operations = cursor.use { storedFiles ->
            storedFiles.mapRows { createStoragePathMigration(it, sourcePath, destinationPath) }
                .filterNotNullTo(ArrayList())
        }

        applyBatchOrThrow(operations)
    }

    @Throws(RemoteException::class)
    private fun queryStoredFiles(): Cursor? {
        val selection = ProviderTableMeta.FILE_STORAGE_PATH + " IS NOT NULL"
        val selectionArgs = emptyArray<String>()
        val resolver = contentResolver
        if (resolver != null) {
            return resolver.query(ProviderTableMeta.CONTENT_URI_FILE, null, selection, selectionArgs, null)
        }

        return try {
            requireContentProviderClient().query(
                ProviderTableMeta.CONTENT_URI_FILE,
                arrayOf(ProviderTableMeta._ID, ProviderTableMeta.FILE_STORAGE_PATH),
                selection,
                selectionArgs,
                null
            )
        } catch (e: RemoteException) {
            Log_OC.e(TAG, e.message, e)
            throw e
        }
    }

    private fun createStoragePathMigration(
        cursor: Cursor,
        sourcePath: String,
        destinationPath: String
    ): ContentProviderOperation? {
        val fileId = cursor.longValue(ProviderTableMeta._ID).toString()
        val oldFileStoragePath = cursor.stringValue(ProviderTableMeta.FILE_STORAGE_PATH)
        if (oldFileStoragePath == null || !oldFileStoragePath.startsWith(sourcePath)) return null

        val newFileStoragePath = destinationPath + oldFileStoragePath.substring(sourcePath.length)
        val cv = ContentValues().apply { put(ProviderTableMeta.FILE_STORAGE_PATH, newFileStoragePath) }

        return ContentProviderOperation.newUpdate(ProviderTableMeta.CONTENT_URI)
            .withValues(cv)
            .withSelection(ID_SELECTION, arrayOf(fileId))
            .build()
    }
    // endregion

    // region Folder sync
    fun createDirectoryTree(remotePath: String, createdRemoteFolder: RemoteFile?) {
        val root = getFileByEncryptedRemotePath(OCFile.ROOT_PATH)
        if (root == null) {
            Log_OC.e(TAG, "createDirectoryTree: root folder not found, skipping $remotePath")
            return
        }

        val folderPaths = remotePath
            .split(OCFile.PATH_SEPARATOR)
            .filter { it.isNotEmpty() }
            .runningFold(OCFile.ROOT_PATH) { parentPath, name -> parentPath + name + OCFile.PATH_SEPARATOR }
            .drop(1)

        folderPaths.foldIndexed(root) { index, parent, path ->
            getFileByEncryptedRemotePath(path) ?: run {
                val metadata = if (index == folderPaths.lastIndex) createdRemoteFolder else null
                saveNewFolder(path, parent, metadata)
            }
        }
    }

    private fun saveNewFolder(remotePath: String, parent: OCFile, metadata: RemoteFile?): OCFile =
        OCFile(remotePath).apply {
            mimeType = MimeType.DIRECTORY
            parentId = parent.fileId
            remoteId = metadata?.remoteId
            permissions = metadata?.permissions
            modificationTimestamp = System.currentTimeMillis()
            isEncrypted = FileStorageUtils.checkEncryptionStatus(this, this@FileDataStorageManager)
            saveFile(this)
            Log_OC.d(TAG, "createDirectoryTree: created $remotePath in Database")
        }

    fun saveFolder(folder: OCFile, updatedFiles: List<OCFile>, filesToRemove: Collection<OCFile>) {
        Log_OC.d(
            TAG,
            "Saving folder ${folder.remotePath} with ${updatedFiles.size} children and " +
                "${filesToRemove.size} files to remove"
        )

        val removedChildren = filesToRemove.filter { it.parentId == folder.fileId && !isPendingOfflineCreation(it) }
        removedChildren.forEach { deleteLocalCopy(it) }

        db.runInTransaction {
            upsertFolderContent(folder, updatedFiles)
            removedChildren.forEach { deleteFromDatabase(it) }
            fileDao.updateFolderMetadata(folder.toFolderMetadataUpdate(user.accountName))
        }

        MainApp.getAppContext().contentResolver.notifyChange(ProviderTableMeta.CONTENT_URI, null)
    }

    private fun upsertFolderContent(folder: OCFile, remoteFiles: List<OCFile>) {
        remoteFiles.forEach { file ->
            val existingFile = findExistingFile(file)
            val remoteEntity = file.toFileEntity(user.accountName)
            val entity = remoteEntity.copy(
                id = existingFile?.id,
                parent = folder.fileId,
                mountType = remoteEntity.mountType ?: existingFile?.mountType
            )

            val insertedRowId = fileDao.upsert(entity)
            if (existingFile == null) {
                file.fileId = insertedRowId
            }
        }
    }

    private fun findExistingFile(file: OCFile): FileEntity? =
        fileDao.getFileById(file.fileId) ?: fileDao.getFileByEncryptedRemotePath(file.remotePath, user.accountName)

    private fun deleteLocalCopy(file: OCFile) {
        if (file.isFolder) {
            val localFolder = File(FileStorageUtils.getDefaultSavePathFor(user.accountName, file))
            if (localFolder.exists()) {
                removeLocalFolder(localFolder)
            }
            return
        }

        if (!file.isDown) return

        val storagePath = file.storagePath
        if (File(storagePath).delete() && MimeTypeUtil.isMedia(file.mimeType)) {
            triggerMediaScan(storagePath, file)
        }
    }

    private fun deleteFromDatabase(file: OCFile) {
        if (file.isFolder) {
            fileDao.deleteFolder(file.fileId, user.accountName, file.remotePath)
            return
        }

        fileDao.deleteFile(file.fileId, user.accountName, file.remotePath)
    }
    // endregion

    // region Shares
    fun saveShare(share: OCShare): Boolean {
        val contentValues = createContentValueForShare(share)

        if (shareExistsForRemoteId(share.remoteId)) {
            updateRows(
                ProviderTableMeta.CONTENT_URI_SHARE,
                contentValues,
                ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED + AND + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
                arrayOf(share.remoteId.toString(), user.accountName)
            ) { logException(FAILED_TO_INSERT_MSG, it) }
            return true
        }

        val resultUri = insertRow(ProviderTableMeta.CONTENT_URI_SHARE, contentValues) {
            logException(FAILED_TO_INSERT_MSG, it)
        }
        resultUri?.let { share.id = it.insertedRowId() }

        return false
    }

    fun getShareById(id: Long): OCShare? = getShareCursorForValue(ProviderTableMeta._ID, id.toString())?.use {
        if (it.moveToFirst()) createShareInstance(it) else null
    }

    private fun shareExistsForRemoteId(remoteId: Long): Boolean {
        val cursor = getShareCursorForValue(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, remoteId.toString())

        return cursor?.use { it.moveToFirst() } == true
    }

    private fun getShareCursorForValue(key: String, value: String): Cursor? = queryRows(
        ProviderTableMeta.CONTENT_URI_SHARE,
        null,
        key + AND + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + "=?",
        arrayOf(value, user.accountName)
    ) { Log_OC.w(TAG, "Could not get details, assuming share does not exist: " + it.message) }

    fun getSharesByPathAndType(path: String?, type: ShareType, shareWith: String?): List<OCShare> {
        if (path == null) return ArrayList()

        val isPublicLink = ShareType.PUBLIC_LINK == type
        val baseSelection = ProviderTableMeta.OCSHARES_PATH + AND +
            ProviderTableMeta.OCSHARES_SHARE_TYPE + AND +
            ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = ?"
        val selection = if (isPublicLink) {
            baseSelection
        } else {
            baseSelection + " AND " + ProviderTableMeta.OCSHARES_SHARE_WITH + " = ?"
        }

        val baseArgs = arrayOf(path, type.value.toString(), user.accountName)
        val selectionArgs = if (isPublicLink) baseArgs else baseArgs + (shareWith ?: "")

        val cursor = queryRows(ProviderTableMeta.CONTENT_URI_SHARE, null, selection, selectionArgs) {
            logException("Could not get file details: ", it)
        }

        return readShares(cursor)
    }

    val shares: List<OCShare>
        get() {
            val cursor = queryRows(
                ProviderTableMeta.CONTENT_URI_SHARE,
                null,
                ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = ?",
                arrayOf(user.accountName)
            ) { logException("Could not get list of shares: ", it) }

            return readShares(cursor)
        }

    fun getSharesWithForAFile(filePath: String?, accountName: String?): List<OCShare> {
        if (filePath == null || accountName == null) return ArrayList()

        val shareTypeSelection = SHARE_WITH_TYPES.joinToString(" OR ") {
            ProviderTableMeta.OCSHARES_SHARE_TYPE + " = ?"
        }
        val selection = ProviderTableMeta.OCSHARES_PATH + AND +
            ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + AND +
            " (" + shareTypeSelection + " ) "
        val selectionArgs = arrayOf(filePath, accountName) + SHARE_WITH_TYPES.map { it.value.toString() }

        val cursor = queryRows(ProviderTableMeta.CONTENT_URI_SHARE, null, selection, selectionArgs) {
            logException("Could not get list of shares with: ", it)
        }

        return readShares(cursor)
    }

    private fun readShares(cursor: Cursor?): List<OCShare> =
        cursor?.use { it.mapRows(::createShareInstance) } ?: ArrayList()

    private fun createContentValueForShare(share: OCShare) = ContentValues().apply {
        put(ProviderTableMeta.OCSHARES_FILE_SOURCE, share.fileSource)
        put(ProviderTableMeta.OCSHARES_ITEM_SOURCE, share.itemSource)
        share.shareType?.let { put(ProviderTableMeta.OCSHARES_SHARE_TYPE, it.value) }
        put(ProviderTableMeta.OCSHARES_SHARE_WITH, share.shareWith)
        put(ProviderTableMeta.OCSHARES_PATH, share.path)
        put(ProviderTableMeta.OCSHARES_PERMISSIONS, share.permissions)
        put(ProviderTableMeta.OCSHARES_SHARED_DATE, share.sharedDate)
        put(ProviderTableMeta.OCSHARES_EXPIRATION_DATE, share.expirationDate)
        put(ProviderTableMeta.OCSHARES_TOKEN, share.token)
        put(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME, share.sharedWithDisplayName)
        put(ProviderTableMeta.OCSHARES_IS_DIRECTORY, share.isFolder.toInt())
        put(ProviderTableMeta.OCSHARES_USER_ID, share.userId)
        put(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED, share.remoteId)
        put(ProviderTableMeta.OCSHARES_ACCOUNT_OWNER, user.accountName)
        put(ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED, share.isPasswordProtected.toInt())
        put(ProviderTableMeta.OCSHARES_NOTE, share.note)
        put(ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD, share.isHideFileDownload)
        put(ProviderTableMeta.OCSHARES_SHARE_LINK, share.shareLink)
        put(ProviderTableMeta.OCSHARES_SHARE_LABEL, share.label)
        putDownloadLimit(share.fileDownloadLimit)
        put(ProviderTableMeta.OCSHARES_ATTRIBUTES, share.attributes)
    }

    private fun ContentValues.putDownloadLimit(downloadLimit: FileDownloadLimit?) {
        if (downloadLimit == null) {
            putNull(ProviderTableMeta.OCSHARES_DOWNLOADLIMIT_LIMIT)
            putNull(ProviderTableMeta.OCSHARES_DOWNLOADLIMIT_COUNT)
            return
        }

        put(ProviderTableMeta.OCSHARES_DOWNLOADLIMIT_LIMIT, downloadLimit.limit)
        put(ProviderTableMeta.OCSHARES_DOWNLOADLIMIT_COUNT, downloadLimit.count)
    }

    private fun createShareInstance(cursor: Cursor): OCShare {
        val shareToken = cursor.stringValue(ProviderTableMeta.OCSHARES_TOKEN)

        return OCShare(cursor.stringValue(ProviderTableMeta.OCSHARES_PATH)).apply {
            id = cursor.longValue(ProviderTableMeta._ID)
            fileSource = cursor.longValue(ProviderTableMeta.OCSHARES_FILE_SOURCE)
            itemSource = cursor.longValue(ProviderTableMeta.OCSHARES_ITEM_SOURCE)
            shareType = ShareType.fromValue(cursor.intValue(ProviderTableMeta.OCSHARES_SHARE_TYPE))
            shareWith = cursor.stringValue(ProviderTableMeta.OCSHARES_SHARE_WITH)
            permissions = cursor.intValue(ProviderTableMeta.OCSHARES_PERMISSIONS)
            sharedDate = cursor.longValue(ProviderTableMeta.OCSHARES_SHARED_DATE)
            expirationDate = cursor.longValue(ProviderTableMeta.OCSHARES_EXPIRATION_DATE)
            token = shareToken
            sharedWithDisplayName = cursor.stringValue(ProviderTableMeta.OCSHARES_SHARE_WITH_DISPLAY_NAME)
            isFolder = cursor.intValue(ProviderTableMeta.OCSHARES_IS_DIRECTORY) == 1
            userId = cursor.stringValue(ProviderTableMeta.OCSHARES_USER_ID)
            remoteId = cursor.longValue(ProviderTableMeta.OCSHARES_ID_REMOTE_SHARED)
            isPasswordProtected = cursor.intValue(ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED) == 1
            note = cursor.stringValue(ProviderTableMeta.OCSHARES_NOTE)
            isHideFileDownload = cursor.intValue(ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD) == 1
            shareLink = cursor.stringValue(ProviderTableMeta.OCSHARES_SHARE_LINK)
            label = cursor.stringValue(ProviderTableMeta.OCSHARES_SHARE_LABEL)
            getDownloadLimitFromCursor(cursor, shareToken)?.let { fileDownloadLimit = it }
            attributes = cursor.stringValue(ProviderTableMeta.OCSHARES_ATTRIBUTES)
        }
    }

    private fun getDownloadLimitFromCursor(cursor: Cursor, token: String?): FileDownloadLimit? {
        if (token == null) return null

        val limit = cursor.intValueOrDefault(ProviderTableMeta.OCSHARES_DOWNLOADLIMIT_LIMIT)
        val count = cursor.intValueOrDefault(ProviderTableMeta.OCSHARES_DOWNLOADLIMIT_COUNT)
        if (limit == DEFAULT_CURSOR_INT_VALUE || count == DEFAULT_CURSOR_INT_VALUE) return null

        return FileDownloadLimit(token, limit, count)
    }

    private fun resetShareFlagInAFile(filePath: String) {
        val contentValues = ContentValues().apply {
            put(ProviderTableMeta.FILE_SHARED_VIA_LINK, false)
            put(ProviderTableMeta.FILE_SHARED_WITH_SHAREE, false)
        }
        val where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + " = ?"

        updateRows(ProviderTableMeta.CONTENT_URI, contentValues, where, arrayOf(user.accountName, filePath)) {
            logException("Exception in resetShareFlagsInFolder ", it)
        }
    }

    fun removeShare(share: OCShare) {
        val where = ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + AND + ProviderTableMeta._ID + " = ?"

        deleteRows(ProviderTableMeta.CONTENT_URI_SHARE, where, arrayOf(user.accountName, share.id.toString())) {
            Log_OC.d(TAG, it.message, it)
        }
    }

    fun saveSharesFromRemoteFile(shares: List<RemoteFile>?): Boolean {
        if (shares.isNullOrEmpty()) return false

        val uniquePaths = shares.mapNotNullTo(HashSet()) { it.remotePath }
        val sharesChanged = areShareesChanged(shares)

        val resetOperations = uniquePaths.mapTo(ArrayList()) { path ->
            resetShareFlagInAFile(path)
            createRemoveSharesInFileOperation(path)
        }
        if (resetOperations.isNotEmpty()) {
            applyBatch(resetOperations)
        }

        val insertOperations = shares
            .flatMap { ShareeEntry.getContentValues(it, user.accountName).orEmpty() }
            .mapTo(ArrayList(), ::createInsertShareOperation)
        if (insertOperations.isNotEmpty()) {
            applyBatch(insertOperations)
        }

        return sharesChanged
    }

    private fun areShareesChanged(remoteFiles: List<RemoteFile>): Boolean {
        if (remoteFiles.isEmpty()) return false

        val newShareesByPath = remoteFiles.asSequence()
            .filter { it.remotePath != null }
            .groupBy { it.remotePath as String }
            .mapValues { (_, files) ->
                files.asSequence()
                    .flatMap { file -> file.sharees.orEmpty().asSequence() }
                    .mapNotNull { sharee -> sharee.shareType?.let { "${sharee.userId}:${it.value}" } }
                    .toSet()
            }

        val existingShareesByPath = newShareesByPath.keys
            .chunked(SHARE_PATH_QUERY_CHUNK_SIZE)
            .flatMap { chunk -> shareDao.getShareeKeys(chunk, user.accountName, ShareeKey.shareableShareTypeValues) }
            .groupBy(ShareeKey::path) { "${it.shareWith}:${it.shareType}" }
            .mapValues { (_, keys) -> keys.toSet() }

        return newShareesByPath.keys.any { path ->
            newShareesByPath[path].orEmpty() != existingShareesByPath[path].orEmpty()
        }
    }

    fun saveSharesDB(shares: List<OCShare>) {
        val operations = ArrayList<ContentProviderOperation>()

        shares.mapNotNull { it.path }.distinct().forEach { filePath ->
            resetShareFlagInAFile(filePath)
            operations.add(createRemoveSharesInFileOperation(filePath))
        }

        shares.mapTo(operations) { createInsertShareOperation(createContentValueForShare(it)) }
        if (operations.isEmpty()) return

        Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size))
        applyBatch(operations)
    }

    suspend fun saveShares(shares: List<OCShare>, accountName: String) {
        withContext(Dispatchers.IO) {
            shareDao.insertAll(shares.map { it.toEntity(accountName) })
        }
    }

    fun removeSharesForFile(remotePath: String?) {
        if (remotePath == null) return

        resetShareFlagInAFile(remotePath)

        val operations = arrayListOf(createRemoveSharesInFileOperation(remotePath))
        Log_OC.d(TAG, String.format(Locale.ENGLISH, SENDING_TO_FILECONTENTPROVIDER_MSG, operations.size))
        applyBatch(operations)
    }

    private fun createInsertShareOperation(contentValues: ContentValues): ContentProviderOperation =
        ContentProviderOperation.newInsert(ProviderTableMeta.CONTENT_URI_SHARE)
            .withValues(contentValues)
            .build()

    private fun createRemoveSharesInFileOperation(filePath: String): ContentProviderOperation {
        val where = ProviderTableMeta.OCSHARES_PATH + AND + ProviderTableMeta.OCSHARES_ACCOUNT_OWNER + " = ?"

        return ContentProviderOperation.newDelete(ProviderTableMeta.CONTENT_URI_SHARE)
            .withSelection(where, arrayOf(filePath, user.accountName))
            .build()
    }
    // endregion

    // region Media scan
    fun deleteFileInMediaScan(path: String) {
        val mimeType = FileStorageUtils.getMimeTypeFromName(path)
        val mediaUri = when {
            MimeTypeUtil.isImage(mimeType) -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            MimeTypeUtil.isAudio(mimeType) -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            MimeTypeUtil.isVideo(mimeType) -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> return
        }

        @Suppress("DEPRECATION")
        val where = MediaStore.MediaColumns.DATA + "=?"
        deleteRows(mediaUri, where, arrayOf(path)) {
            logException("Exception deleting media file in MediaStore ", it)
        }
    }
    // endregion

    // region Conflicts
    @SuppressFBWarnings("PSC")
    fun saveConflict(ocFile: OCFile, etagInConflict: String?) {
        val cv = ContentValues().apply {
            put(ProviderTableMeta.FILE_ETAG_IN_CONFLICT, if (ocFile.isDown) etagInConflict else null)
        }

        val fileIdArgs = arrayOf(ocFile.fileId.toString())
        val updated = updateRows(ProviderTableMeta.CONTENT_URI_FILE, cv, ID_SELECTION, fileIdArgs) {
            logException(FAILED_SAVING_CONFLICT_MSG, it)
        }

        Log_OC.d(TAG, "Number of files updated with CONFLICT: $updated")
        if (updated <= 0) return

        if (etagInConflict != null && ocFile.isDown) {
            markAncestorsInConflict(ocFile, cv)
        } else {
            clearConflictInAncestors(ocFile, cv)
        }
    }

    private fun markAncestorsInConflict(ocFile: OCFile, cv: ContentValues) {
        val ancestorIds = HashSet<String>()
        var parentId = ocFile.parentId
        while (parentId != ROOT_PARENT_ID.toLong()) {
            ancestorIds.add(parentId.toString())
            parentId = getFileById(parentId)?.parentId ?: break
        }

        if (ancestorIds.isEmpty()) return

        val selection = ancestorIds.joinToString(", ", prefix = ProviderTableMeta._ID + " IN (", postfix = ")") {
            "?"
        }

        updateRows(ProviderTableMeta.CONTENT_URI_FILE, cv, selection, ancestorIds.toTypedArray()) {
            logException(FAILED_SAVING_CONFLICT_MSG, it)
        }
    }

    private fun clearConflictInAncestors(ocFile: OCFile, cv: ContentValues) {
        var parentPath = parentFolderPath(ocFile.remotePath.removeSuffix(OCFile.PATH_SEPARATOR))
        Log_OC.d(TAG, "checking parents to remove conflict; STARTING with $parentPath")

        while (parentPath.isNotEmpty()) {
            clearConflictIfNoDescendantIsInConflict(parentPath, cv)

            parentPath = parentFolderPath(parentPath.dropLast(1))
            Log_OC.d(TAG, "checking parents to remove conflict; NEXT $parentPath")
        }
    }

    private fun parentFolderPath(path: String): String = path.substring(0, path.lastIndexOf(OCFile.PATH_SEPARATOR) + 1)

    private fun clearConflictIfNoDescendantIsInConflict(folderPath: String, cv: ContentValues) {
        val whereForDescendantsInConflict = ProviderTableMeta.FILE_ETAG_IN_CONFLICT + " IS NOT NULL AND " +
            ProviderTableMeta.FILE_CONTENT_TYPE + " != '" + MimeType.DIRECTORY + "' AND " +
            ProviderTableMeta.FILE_ACCOUNT_OWNER + AND +
            ProviderTableMeta.FILE_PATH + " LIKE ?"

        val descendantsInConflict = queryRows(
            ProviderTableMeta.CONTENT_URI_FILE,
            arrayOf(ProviderTableMeta.FILE_PATH),
            whereForDescendantsInConflict,
            arrayOf(user.accountName, "$folderPath%")
        ) { logException("Failed querying for descendents in conflict ", it) }

        val conflictCount = descendantsInConflict
            ?.use { cursor -> cursor.mapRows { it.stringValue(ProviderTableMeta.FILE_PATH) } }
            .orEmpty()
            .count { it?.startsWith(folderPath) == true }
        if (conflictCount > 0) {
            Log_OC.d(TAG, "STILL $conflictCount in $folderPath")
            return
        }

        Log_OC.d(TAG, "NO MORE conflicts in $folderPath")
        val where = ProviderTableMeta.FILE_ACCOUNT_OWNER + AND + ProviderTableMeta.FILE_PATH + "=?"
        updateRows(ProviderTableMeta.CONTENT_URI_FILE, cv, where, arrayOf(user.accountName, folderPath)) {
            logException(FAILED_SAVING_CONFLICT_MSG, it)
        }
    }
    // endregion

    // region Capabilities
    fun saveCapabilities(capability: OCCapability) {
        val accountName = user.accountName
        val existingId = capabilityDao.getByAccountName(accountName)?.id
        val rowId = capabilityDao.upsert(capability.toEntity(accountName, existingId))
        if (existingId != null) return

        capability.id = rowId
        capability.accountName = accountName
    }

    fun getE2EEVersion(user: User): String = getE2EEVersionObject(user).value

    fun getE2EEVersionObject(user: User): E2EVersion {
        val serverE2EEVersion = getCapability(user).endToEndEncryptionApiVersion

        return E2EVersionHelper.getMaxCompatibleE2EEVersion(serverE2EEVersion)
    }

    fun getCapability(user: User): OCCapability = getCapability(user.accountName)

    fun getCapability(accountName: String): OCCapability = capabilityDao.getByAccountName(accountName).toOCCapability()

    fun capabilityExistsForAccount(accountName: String): Boolean = capabilityDao.getByAccountName(accountName) != null
    // endregion

    // region Virtual folders and gallery
    fun deleteVirtuals(type: VirtualFolderType?) {
        deleteRows(ProviderTableMeta.CONTENT_URI_VIRTUAL, VIRTUAL_TYPE_SELECTION, arrayOf(type.toString())) {
            logException(FAILED_TO_INSERT_MSG, it)
        }
    }

    fun saveVirtuals(values: List<ContentValues>) {
        val contentUriVirtual = ProviderTableMeta.CONTENT_URI_VIRTUAL
        val arrayValues = values.toTypedArray()

        val resolver = contentResolver
        if (resolver != null) {
            resolver.bulkInsert(contentUriVirtual, arrayValues)
            return
        }

        try {
            requireContentProviderClient().bulkInsert(contentUriVirtual, arrayValues)
        } catch (e: RemoteException) {
            logException("saveVirtuals", e)
        }
    }

    fun getVirtualFolderContent(type: VirtualFolderType?, onlyImages: Boolean): List<OCFile> {
        val cursor = queryRows(
            ProviderTableMeta.CONTENT_URI_VIRTUAL,
            null,
            VIRTUAL_TYPE_SELECTION,
            arrayOf(type.toString())
        ) { Log_OC.e(TAG, it.message, it) }

        val files = cursor?.use { it.mapRows(::createFileInstanceFromVirtual) }.orEmpty().filterNotNull()
        val filteredFiles = if (onlyImages) files.filter { it.matchesMimeType(MimeTypeUtil::isImage) } else files

        return filteredFiles.toMutableList().apply { sort() }
    }

    private fun createFileInstanceFromVirtual(cursor: Cursor): OCFile? =
        getFileById(cursor.longValue(ProviderTableMeta.VIRTUAL_OCFILE_ID))

    val allGalleryItems: List<OCFile>
        get() = getGalleryItems(0, Long.MAX_VALUE)

    fun getGalleryItems(startDate: Long, endDate: Long): List<OCFile> {
        Log_OC.d(TAG, "getGalleryItems - start: $startDate, $endDate")

        val fileEntities = fileDao.getGalleryItems(startDate, endDate, user.accountName)
        Log_OC.d(TAG, "getGalleryItems - query complete, list size: ${fileEntities.size}")

        val files = fileEntities.map(::createFileInstance)
        Log_OC.d(TAG, "getGalleryItems - finished")

        return files
    }

    suspend fun getGalleryItemsPageSuspended(pathPrefix: String, mimeFilter: String?, limit: Int): List<OCFile> {
        val result = ArrayList<OCFile>(minOf(limit, GALLERY_DB_CHUNK_SIZE))
        var offset = 0

        while (offset < limit) {
            val chunkLimit = minOf(GALLERY_DB_CHUNK_SIZE, limit - offset)
            val entities = fileDao.getGalleryItemsPageSuspended(
                user.accountName,
                pathPrefix,
                mimeFilter,
                chunkLimit,
                offset
            )

            entities.mapTo(result, ::createFileInstance)
            offset += entities.size

            if (entities.size < chunkLimit) break
        }

        return result
    }
    // endregion

    // region Encryption
    fun retrieveRemotePathConsideringEncryption(file: OCFile): String? {
        if (!file.isEncrypted) return file.remotePath

        return getFileByEncryptedRemotePath(file.remotePath)?.remotePath
            ?: getEncryptedRemotePath(file.decryptedRemotePath ?: file.remotePath)
    }

    fun getEncryptedRemotePath(decryptedRemotePath: String): String? =
        getFileByDecryptedRemotePath(decryptedRemotePath)?.remotePath

    fun setReadOnly(file: OCFile?, readOnly: Boolean) {
        if (file == null) return

        val root = getEncryptedRootFolder(file) ?: return
        fileDao.setReadOnly(user.accountName, root.remotePath, readOnly.toInt())

        if (root.remotePath == file.remotePath) {
            file.isReadOnly = readOnly
        }
    }

    fun isReadOnly(file: OCFile?): Boolean = getEncryptedRootFolder(file)?.isReadOnly == true

    private fun getEncryptedRootFolder(file: OCFile?): OCFile? {
        if (file == null || !file.isEncrypted) return null

        return generateSequence(getFileById(file.fileId)) { getFileById(it.parentId) }
            .takeWhile { it.isEncrypted }
            .lastOrNull()
    }
    // endregion

    // region Internal two-way sync
    fun getInternalTwoWaySyncFolders(user: User): List<OCFile> = fileDao
        .getInternalTwoWaySyncFolders(user.accountName)
        .map(::createFileInstance)
        .filter { it.isFolder && !it.isRootDirectory }

    fun isPartOfInternalTwoWaySync(file: OCFile): Boolean {
        if (file.isInternalFolderSync) return true

        return generateSequence(file) { getFileById(it.parentId) }
            .takeWhile { OCFile.ROOT_PATH != it.decryptedRemotePath }
            .any { it.isInternalFolderSync }
    }
    // endregion

    // region Content provider access
    private fun requireContentProviderClient(): ContentProviderClient = checkNotNull(contentProviderClient) {
        "Neither a ContentResolver nor a ContentProviderClient is available"
    }

    private inline fun updateRows(
        uri: Uri,
        values: ContentValues,
        where: String?,
        whereArgs: Array<out String?>?,
        onError: (RemoteException) -> Unit
    ): Int {
        contentResolver?.let { return it.update(uri, values, where, whereArgs) }

        return try {
            requireContentProviderClient().update(uri, values, where, whereArgs)
        } catch (e: RemoteException) {
            onError(e)
            0
        }
    }

    private inline fun insertRow(uri: Uri, values: ContentValues, onError: (RemoteException) -> Unit): Uri? {
        contentResolver?.let { return it.insert(uri, values) }

        return try {
            requireContentProviderClient().insert(uri, values)
        } catch (e: RemoteException) {
            onError(e)
            null
        }
    }

    private inline fun deleteRows(
        uri: Uri,
        where: String?,
        whereArgs: Array<out String?>?,
        onError: (RemoteException) -> Unit
    ): Int {
        contentResolver?.let { return it.delete(uri, where, whereArgs) }

        return try {
            requireContentProviderClient().delete(uri, where, whereArgs)
        } catch (e: RemoteException) {
            onError(e)
            0
        }
    }

    private inline fun queryRows(
        uri: Uri,
        projection: Array<out String?>?,
        selection: String?,
        selectionArgs: Array<out String?>?,
        onError: (RemoteException) -> Unit
    ): Cursor? {
        contentResolver?.let { return it.query(uri, projection, selection, selectionArgs, null) }

        return try {
            requireContentProviderClient().query(uri, projection, selection, selectionArgs, null)
        } catch (e: RemoteException) {
            onError(e)
            null
        }
    }

    private fun applyBatch(operations: ArrayList<ContentProviderOperation>) {
        try {
            applyBatchOrThrow(operations)
        } catch (e: OperationApplicationException) {
            logException(EXCEPTION_MSG, e)
        } catch (e: RemoteException) {
            logException(EXCEPTION_MSG, e)
        }
    }

    @Throws(RemoteException::class, OperationApplicationException::class)
    private fun applyBatchOrThrow(operations: ArrayList<ContentProviderOperation>) {
        val resolver = contentResolver
        if (resolver != null) {
            resolver.applyBatch(MainApp.authority, operations)
            return
        }

        requireContentProviderClient().applyBatch(operations)
    }

    private fun logException(message: String, e: Exception) {
        Log_OC.e(TAG, message + e.message, e)
    }

    private fun Uri.insertedRowId(): Long = pathSegments[1].toLong()

    private inline fun <T> Cursor.mapRows(transform: (Cursor) -> T): MutableList<T> {
        val rows = ArrayList<T>(count)
        while (moveToNext()) {
            rows.add(transform(this))
        }

        return rows
    }

    private fun Cursor.stringValue(columnName: String): String? = getString(getColumnIndexOrThrow(columnName))

    private fun Cursor.intValue(columnName: String): Int = getInt(getColumnIndexOrThrow(columnName))

    private fun Cursor.longValue(columnName: String): Long = getLong(getColumnIndexOrThrow(columnName))

    private fun Cursor.intValueOrDefault(columnName: String): Int {
        val index = getColumnIndex(columnName)
        if (index == DEFAULT_CURSOR_INT_VALUE) return DEFAULT_CURSOR_INT_VALUE

        return getInt(index)
    }
    // endregion

    companion object {
        @JvmField
        val TAG: String = FileDataStorageManager::class.java.simpleName

        const val ROOT_PARENT_ID: Int = 0

        private const val UNKNOWN_FILE_ID = -1L
        private const val NO_LOCK_TYPE = -1
        private const val NO_INTERNAL_TWO_WAY_SYNC = -1L
        private const val INTERNAL_TWO_WAY_SYNC_NEVER_SYNCED = 0L
        private const val DEFAULT_CURSOR_INT_VALUE = -1

        private const val AND = " = ? AND "
        private const val ID_SELECTION = ProviderTableMeta._ID + "=?"
        private const val VIRTUAL_TYPE_SELECTION = ProviderTableMeta.VIRTUAL_TYPE + "=?"
        private const val FAILED_TO_INSERT_MSG = "Fail to insert insert file to database "
        private const val FAILED_SAVING_CONFLICT_MSG = "Failed saving conflict in database "
        private const val SENDING_TO_FILECONTENTPROVIDER_MSG = "Sending %d operations to FileContentProvider"
        private const val EXCEPTION_MSG = "Exception in batch of operations "
        private const val JSON_NULL_STRING = "null"
        private const val JSON_EMPTY_ARRAY = "[]"

        private const val SHARE_PATH_QUERY_CHUNK_SIZE = 400
        private const val GALLERY_DB_CHUNK_SIZE = 500
        private const val MAX_CONFLICT_RESOLUTION_ATTEMPTS = 10
        private const val EXTENSION_SEPARATOR = "."
        private const val FIRST_CONFLICT_SUFFIX = " (1)"
        private val CONFLICT_COUNTER_REGEX = Regex("""(.*)\((\d+)\)$""", RegexOption.MULTILINE)

        private val SHARE_WITH_TYPES = listOf(
            ShareType.USER,
            ShareType.GROUP,
            ShareType.EMAIL,
            ShareType.FEDERATED,
            ShareType.FEDERATED_GROUP,
            ShareType.ROOM,
            ShareType.CIRCLE
        )

        // region Temp encrypted folder
        @JvmStatic
        fun clearTempEncryptedFolder(accountName: String?) {
            val tempEncryptedFolder = File(FileStorageUtils.getTemporalEncryptedFolderPath(accountName))
            if (!tempEncryptedFolder.exists()) {
                Log_OC.d(TAG, "tempEncryptedFolder does not exist")
                return
            }

            try {
                FileUtils.cleanDirectory(tempEncryptedFolder)
                Log_OC.d(TAG, "tempEncryptedFolder cleared")
            } catch (exception: IOException) {
                Log_OC.d(TAG, "Error caught at clearTempEncryptedFolder: $exception")
            }
        }

        @JvmStatic
        fun createTempEncryptedFolder(accountName: String?): File {
            val tempEncryptedFolder = File(FileStorageUtils.getTemporalEncryptedFolderPath(accountName))

            if (tempEncryptedFolder.exists()) {
                Log_OC.d(TAG, "tempEncryptedFolder already exists")
            } else {
                val isTempEncryptedFolderCreated = tempEncryptedFolder.mkdirs()
                Log_OC.d(TAG, "tempEncryptedFolder created$isTempEncryptedFolderCreated")
            }

            return tempEncryptedFolder
        }
        // endregion

        // region Media scan
        @JvmStatic
        fun triggerMediaScan(path: String?) {
            triggerMediaScan(MainApp.getAppContext(), path, null)
        }

        @JvmStatic
        fun triggerMediaScan(path: String?, file: OCFile?) {
            triggerMediaScan(MainApp.getAppContext(), path, file)
        }

        @JvmStatic
        fun triggerMediaScan(context: Context?, path: String?, file: OCFile?) {
            if (path.isNullOrEmpty()) return

            val mimeTypes = file?.mimeType?.let { arrayOf(it) }
            MediaScannerConnection.scanFile(context, arrayOf(path), mimeTypes) { scannedPath, scannedUri ->
                if (scannedUri != null) {
                    Log_OC.d(TAG, "Media scan completed for $scannedPath")
                } else {
                    Log_OC.w(TAG, "Media scan failed for $scannedPath")
                }
            }
        }
        // endregion

        // region Conflict resolution
        @JvmStatic
        fun getRemotePathForConflictResolution(client: OwnCloudClient, remotePath: String, fileName: String): String? {
            var name = fileName
            repeat(MAX_CONFLICT_RESOLUTION_ATTEMPTS) {
                name = generateFileNameForConflictResolution(name)
                val newPath = "$remotePath$name"

                val result = ExistenceCheckRemoteOperation(newPath, false).execute(client)
                if (!result.isSuccess && result.httpCode != HTTPStatusCodes.NOT_FOUND.code) return null

                when (RemoteFileExistence.fromExistenceCheck(result)) {
                    RemoteFileExistence.DOES_NOT_EXIST -> return newPath
                    RemoteFileExistence.UNAUTHORIZED -> return null
                    else -> Unit
                }
            }

            return null
        }

        @JvmStatic
        fun generateFileNameForConflictResolution(fileName: String): String {
            val isFolder = fileName.endsWith(OCFile.PATH_SEPARATOR)
            val separator = when {
                isFolder -> OCFile.PATH_SEPARATOR
                fileName.indexOf(EXTENSION_SEPARATOR) > 0 -> EXTENSION_SEPARATOR
                else -> ""
            }

            val baseName = if (separator.isNotEmpty()) fileName.substringBeforeLast(separator) else fileName
            val suffix = if (separator.isNotEmpty()) fileName.substringAfterLast(separator, "") else ""
            val nameLastPart = if (suffix.isNotEmpty() || isFolder) "$separator$suffix" else suffix

            return incrementConflictCounter(baseName) + nameLastPart
        }

        private fun incrementConflictCounter(name: String): String {
            if (!CONFLICT_COUNTER_REGEX.matches(name)) return name + FIRST_CONFLICT_SUFFIX

            return CONFLICT_COUNTER_REGEX.replace(name) { match ->
                val baseName = match.groups[1]?.value
                val number = match.groups[2]?.value?.toBigIntegerOrNull() ?: BigInteger.ZERO
                "$baseName(${number + BigInteger.ONE})"
            }
        }
        // endregion
    }
}
