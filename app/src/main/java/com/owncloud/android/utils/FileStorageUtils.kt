/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2016-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.annotation.VisibleForTesting
import androidx.core.app.ActivityCompat
import com.nextcloud.client.preferences.SubFolderRule
import com.nextcloud.utils.extensions.StringConstants
import com.nextcloud.utils.extensions.getShareeList
import com.nextcloud.utils.extensions.sharedViaLink
import com.nextcloud.utils.extensions.sharedWithSharee
import com.nextcloud.utils.extensions.tags
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.ui.helpers.FileOperationsHelper
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.apache.commons.io.FilenameUtils
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Suppress("TooManyFunctions")
object FileStorageUtils {
    private val TAG: String = FileStorageUtils::class.java.simpleName
    private const val AUTO_UPLOAD_TAG = "AutoUpload"

    private const val PATTERN_YYYY_MM = "yyyy/MM/"
    private const val PATTERN_YYYY = "yyyy/"
    private const val PATTERN_YYYY_MM_DD = "yyyy/MM/dd/"
    private const val DEFAULT_FALLBACK_STORAGE_PATH = "/storage/sdcard0"
    private const val DEFAULT_EXT_SD_CARD_PATH = "/storage/sdcard1"
    private const val ANDROID_DATA_FOLDER = "/Android/data"
    private const val EXTERNAL_FILES_TYPE = "external"
    private const val TEMP_ENCRYPTED_FOLDER = "temp_encrypted_folder"
    private const val ACCOUNT_NAME_ALLOWED_CHARS = "@"

    private const val ENV_EXTERNAL_STORAGE = "EXTERNAL_STORAGE"
    private const val ENV_SECONDARY_STORAGE = "SECONDARY_STORAGE"
    private const val ENV_EMULATED_STORAGE_TARGET = "EMULATED_STORAGE_TARGET"

    private const val FILE_SAVE_POLL_INTERVAL_MS = 1000L
    private const val UNKNOWN_AVAILABLE_SPACE = -1L

    private const val FIRST_PRINTABLE_CHAR = ' '
    private const val DELETE_CHAR = '\u007F'

    private val BIDI_CONTROL_CHARACTERS = setOf(
        '\u202A', '\u202B', '\u202C', '\u202D', '\u202E',
        '\u200E', '\u200F', '\u2066', '\u2067', '\u2068',
        '\u2069', '\u061C'
    )

    private val INVALID_EXT_FILENAME_CHARS = setOf('"', '*', ':', '/', '<', '>', '?', '\\', '|', DELETE_CHAR)

    private val REPEATED_PATH_SEPARATORS = Regex("${OCFile.PATH_SEPARATOR}+")

    private val MODIFICATION_TIMESTAMP_DESCENDING = compareByDescending<OCFile> { it.modificationTimestamp }

    @JvmStatic
    fun containsBidiControlCharacters(filename: String?): Boolean {
        if (filename == null) return false

        val decoded = try {
            URLDecoder.decode(filename, Charsets.UTF_8.name())
        } catch (_: IllegalArgumentException) {
            filename
        }

        return decoded.any { it < FIRST_PRINTABLE_CHAR || it in BIDI_CONTROL_CHARACTERS }
    }

    @JvmStatic
    fun getFilenameAndExtension(filename: String?, isFolder: Boolean, isRTL: Boolean): Pair<String?, String?> {
        if (isFolder) return filename to ""

        val base = FilenameUtils.getBaseName(filename)
        val rawExtension: String = FilenameUtils.getExtension(filename)
        val extension = if (rawExtension.isEmpty()) rawExtension else StringConstants.DOT + rawExtension

        return if (isRTL) extension to base else base to extension
    }

    @JvmStatic
    fun isValidExtFilename(name: String): Boolean = name.all(::isValidExtFilenameChar)

    private fun isValidExtFilenameChar(c: Char): Boolean = c >= FIRST_PRINTABLE_CHAR && c !in INVALID_EXT_FILENAME_CHARS

    @JvmStatic
    fun getSavePath(accountName: String?): String =
        joinPath(MainApp.getStoragePath(), MainApp.getDataFolder(), encodeAccountName(accountName))

    @JvmStatic
    fun getDefaultSavePathFor(accountName: String?, file: OCFile): String =
        getSavePath(accountName) + file.decryptedRemotePath

    @JvmStatic
    fun getTemporalPath(accountName: String?): String = joinPath(
        MainApp.getStoragePath(),
        MainApp.getDataFolder(),
        StringConstants.TEMP,
        encodeAccountName(accountName)
    )

    @JvmStatic
    fun getTemporalEncryptedFolderPath(accountName: String?): String =
        joinPath(MainApp.getAppContext().filesDir.absolutePath, accountName, TEMP_ENCRYPTED_FOLDER)

    @JvmStatic
    fun getInternalTemporalPath(accountName: String?, context: Context): String =
        getAppTempDirectoryPath(context) + encodeAccountName(accountName)

    @JvmStatic
    fun getAppTempDirectoryPath(context: Context): String =
        joinPath(context.filesDir, MainApp.getDataFolder(), StringConstants.TEMP) + File.separator

    private fun encodeAccountName(accountName: String?): String? = Uri.encode(accountName, ACCOUNT_NAME_ALLOWED_CHARS)

    private fun joinPath(vararg segments: Any?): String = segments.joinToString(File.separator)

    @JvmStatic
    @get:SuppressLint("UsableSpace")
    val usableSpace: Long
        get() = File(MainApp.getStoragePath()).usableSpace

    private fun getSubPathFromDate(date: Long, currentLocale: Locale?, subFolderRule: SubFolderRule?): String {
        if (date == 0L) {
            Log_OC.w(TAG, "FileStorageUtils:getSubPathFromDate date is zero")
            return ""
        }

        val datePattern = when (subFolderRule) {
            SubFolderRule.YEAR -> PATTERN_YYYY
            SubFolderRule.YEAR_MONTH -> PATTERN_YYYY_MM
            SubFolderRule.YEAR_MONTH_DAY -> PATTERN_YYYY_MM_DD
            null -> ""
        }

        return SimpleDateFormat(datePattern, currentLocale)
            .apply { timeZone = TimeZone.getTimeZone(TimeZone.getDefault().id) }
            .format(Date(date))
    }

    private fun stripSyncedFolderPrefix(absolutePath: String, syncedFolderLocalPath: String?): String {
        if (syncedFolderLocalPath.isNullOrEmpty()) return absolutePath

        val prefix = syncedFolderLocalPath.removeSuffix(OCFile.PATH_SEPARATOR)

        return when {
            absolutePath.startsWith(prefix) -> absolutePath.substring(prefix.length)

            absolutePath.startsWith(prefix, ignoreCase = true) -> {
                Log_OC.w(TAG, "local path differs from the synced folder in letter case only, stripping anyway")
                absolutePath.substring(prefix.length)
            }

            else -> {
                Log_OC.e(TAG, "local file is not below its synced folder, dropping the local subfolders")
                OCFile.PATH_SEPARATOR + File(absolutePath).name
            }
        }
    }

    @Suppress("LongParameterList")
    @JvmStatic
    fun getInstantUploadFilePath(
        file: File,
        current: Locale?,
        remotePath: String?,
        syncedFolderLocalPath: String?,
        dateTaken: Long,
        subfolderByDate: Boolean,
        subFolderRule: SubFolderRule?
    ): String {
        val subfolderByDatePath = if (subfolderByDate) getSubPathFromDate(dateTaken, current, subFolderRule) else ""
        Log_OC.w(TAG, "FileStorageUtils:getInstantUploadFilePath subfolderByDate: $subfolderByDate")

        val parentFile = File(stripSyncedFolderPrefix(file.absolutePath, syncedFolderLocalPath)).parentFile
        if (parentFile == null) {
            Log_OC.e(AUTO_UPLOAD_TAG, "Parent folder does not exist!")
        }
        val relativeSubfolderPath = parentFile?.absolutePath.orEmpty()

        return listOf(remotePath, subfolderByDatePath, relativeSubfolderPath, file.name)
            .joinToString(OCFile.PATH_SEPARATOR)
            .replace(REPEATED_PATH_SEPARATORS, OCFile.PATH_SEPARATOR)
    }

    @JvmStatic
    fun getParentPath(remotePath: String): String? = File(remotePath).parent?.let { parentPath ->
        if (parentPath.endsWith(OCFile.PATH_SEPARATOR)) parentPath else parentPath + OCFile.PATH_SEPARATOR
    }

    @JvmStatic
    fun fillOCFile(remoteFile: RemoteFile?): OCFile {
        val remote = requireNotNull(remoteFile)
        return OCFile(remote.remotePath).apply {
            decryptedRemotePath = remote.remotePath
            creationTimestamp = remote.creationTimestamp
            uploadTimestamp = remote.uploadTimestamp
            val isRemoteDirectory = MimeType.DIRECTORY.equals(remote.mimeType, ignoreCase = true)
            fileLength = if (isRemoteDirectory) remote.size else remote.length
            mimeType = remote.mimeType
            modificationTimestamp = remote.modifiedTimestamp
            etag = remote.etag
            permissions = remote.permissions
            remoteId = remote.remoteId
            localId = remote.localId
            isFavorite = remote.isFavorite
            if (isFolder) {
                isEncrypted = remote.isEncrypted
            }
            mountType = remote.mountType
            isPreviewAvailable = remote.isHasPreview
            unreadCommentsCount = remote.unreadCommentsCount
            ownerId = remote.ownerId
            ownerDisplayName = remote.ownerDisplayName
            note = remote.note

            sharees = remote.getShareeList()
            isSharedWithSharee = remote.sharedWithSharee()
            isSharedViaLink = remote.sharedViaLink()

            richWorkspace = remote.richWorkspace
            isLocked = remote.isLocked
            lockType = remote.lockType
            lockOwnerId = remote.lockOwner
            lockOwnerDisplayName = remote.lockOwnerDisplayName
            lockOwnerEditor = remote.lockOwnerEditor
            lockTimestamp = remote.lockTimestamp
            lockTimeout = remote.lockTimeout
            lockToken = remote.lockToken
            tags = remote.tags()
            imageDimension = remote.imageDimension
            geoLocation = remote.geoLocation
            setLivePhoto(remote.livePhoto)
            isHidden = remote.hidden
        }
    }

    @JvmStatic
    fun fillRemoteFile(ocFile: OCFile): RemoteFile = RemoteFile(ocFile.remotePath).apply {
        creationTimestamp = ocFile.creationTimestamp
        length = ocFile.fileLength
        mimeType = ocFile.mimeType
        modifiedTimestamp = ocFile.modificationTimestamp
        etag = ocFile.etag
        permissions = ocFile.permissions
        remoteId = ocFile.remoteId
        isFavorite = ocFile.isFavorite
    }

    @JvmStatic
    fun sortOcFolderDescDateModifiedWithoutFavoritesFirst(files: List<OCFile>): List<OCFile> =
        files.sortedWith(MODIFICATION_TIMESTAMP_DESCENDING)

    @JvmStatic
    fun sortOcFolderDescDateModified(files: MutableList<OCFile>): MutableList<OCFile> {
        files.sortWith(MODIFICATION_TIMESTAMP_DESCENDING)
        return FileSortOrder.sortCloudFilesByFavourite(files)
    }

    @JvmStatic
    fun getFolderSize(dir: File?): Long = dir
        ?.takeIf { it.isDirectory }
        ?.listFiles()
        ?.sumOf { if (it.isDirectory) getFolderSize(it) else it.length() }
        ?: 0L

    @JvmStatic
    fun getMimeTypeFromName(path: String): String {
        val extension = path.substringAfterLast('.', missingDelimiterValue = "")
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()).orEmpty()
    }

    @JvmStatic
    fun searchForLocalFileInDefaultPath(file: OCFile, accountName: String?) {
        if (file.isFolder || file.storagePath?.let { File(it).exists() } == true) return

        val localFile = File(getDefaultSavePathFor(accountName, file))
        if (!localFile.exists()) return

        file.storagePath = localFile.absolutePath
        file.lastSyncDateForData = localFile.lastModified()
    }

    @JvmStatic
    @SuppressFBWarnings(
        value = ["OBL_UNSATISFIED_OBLIGATION_EXCEPTION_EDGE"],
        justification = "False-positive on the output stream"
    )
    fun copyFile(src: File, target: File): Boolean = try {
        src.inputStream().use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        true
    } catch (_: IOException) {
        false
    }

    @JvmStatic
    fun moveFile(sourceFile: File, targetFile: File): Boolean = copyFile(sourceFile, targetFile) && sourceFile.delete()

    @JvmStatic
    fun copyDirs(sourceFolder: File, targetFolder: File): Boolean {
        if (!targetFolder.mkdirs()) return false

        return sourceFolder.listFiles()?.all { child ->
            val target = File(targetFolder, child.name)
            if (child.isDirectory) copyDirs(child, target) else copyFile(child, target)
        } ?: false
    }

    @Suppress("TooGenericExceptionCaught")
    @JvmStatic
    fun deleteRecursively(file: File, storageManager: FileDataStorageManager) {
        if (file.isDirectory) {
            val children = file.listFiles() ?: return
            children.forEach { deleteRecursively(it, storageManager) }
        }

        storageManager.deleteFileInMediaScan(file.absolutePath)
        try {
            Files.deleteIfExists(file.toPath())
        } catch (e: Exception) {
            Log_OC.e(TAG, "Error deleting file: ${file.absolutePath}", e)
        }
    }

    @JvmStatic
    fun deleteRecursive(file: File): Boolean {
        val allChildrenDeleted = if (file.isDirectory) {
            val children = file.listFiles() ?: return true
            children.fold(true) { allDeleted, child -> deleteRecursive(child) && allDeleted }
        } else {
            true
        }

        return file.delete() && allChildrenDeleted
    }

    @JvmStatic
    fun checkIfFileFinishedSaving(file: OCFile) {
        val realFile = File(file.storagePath)
        if (realFile.lastModified() == file.modificationTimestamp || realFile.length() == file.fileLength) return

        var lastModified = 0L
        var lastSize = 0L
        while (realFile.lastModified() != lastModified && realFile.length() != lastSize) {
            lastModified = realFile.lastModified()
            lastSize = realFile.length()
            try {
                Thread.sleep(FILE_SAVE_POLL_INTERVAL_MS)
            } catch (_: InterruptedException) {
                Log_OC.d(TAG, "Interrupted while waiting for file to finish saving")
                Thread.currentThread().interrupt()
                break
            }
        }
    }

    @JvmStatic
    fun checkEncryptionStatus(file: OCFile?, storageManager: FileDataStorageManager): Boolean {
        if (file == null) {
            Log_OC.e(TAG, "checkEncryptionStatus called with null file")
            return false
        }

        return file.isEncrypted || selfAndAncestorsBelowRoot(file, storageManager).any { it.isEncrypted }
    }

    private fun selfAndAncestorsBelowRoot(file: OCFile, storageManager: FileDataStorageManager): Sequence<OCFile> =
        generateSequence(file) { storageManager.getFileById(it.parentId) }
            .takeWhile { OCFile.ROOT_PATH != it.decryptedRemotePath }

    @JvmStatic
    fun getStorageDirectories(context: Context): List<String> {
        val storageDirectories = mutableListOf<String>()
        if (!checkStoragePermission(context)) {
            storageDirectories += primaryStorageDirectory()
            storageDirectories += secondaryStorageDirectories()
        }

        getExtSdCardPathsForActivity(context).forEach { extSdCardPath ->
            if (extSdCardPath !in storageDirectories && canListFiles(File(extSdCardPath))) {
                storageDirectories += extSdCardPath
            }
        }

        return storageDirectories
    }

    @SuppressFBWarnings(
        value = ["DMI_HARDCODED_ABSOLUTE_FILENAME"],
        justification = "Default Android fallback storage path"
    )
    private fun primaryStorageDirectory(): String {
        val emulatedStorageTarget = System.getenv(ENV_EMULATED_STORAGE_TARGET)
        val externalStorage = System.getenv(ENV_EXTERNAL_STORAGE)

        return when {
            !emulatedStorageTarget.isNullOrEmpty() -> emulatedStorageDirectory(emulatedStorageTarget)
            !externalStorage.isNullOrEmpty() -> externalStorage
            File(DEFAULT_FALLBACK_STORAGE_PATH).exists() -> DEFAULT_FALLBACK_STORAGE_PATH
            else -> Environment.getExternalStorageDirectory().absolutePath
        }
    }

    private fun emulatedStorageDirectory(emulatedStorageTarget: String): String {
        val externalStoragePath = Environment.getExternalStorageDirectory().absolutePath
        val lastFolder = OCFile.PATH_SEPARATOR
            .split(externalStoragePath.toRegex())
            .dropLastWhile { it.isEmpty() }
            .last()
        val userId = lastFolder.takeIf { it.toIntOrNull() != null }

        return if (userId == null) emulatedStorageTarget else emulatedStorageTarget + File.separator + userId
    }

    private fun secondaryStorageDirectories(): List<String> = System.getenv(ENV_SECONDARY_STORAGE)
        ?.takeIf { it.isNotEmpty() }
        ?.split(File.pathSeparator)
        ?.dropLastWhile { it.isEmpty() }
        .orEmpty()

    @JvmStatic
    fun pathToUserFriendlyDisplay(path: String?, context: Context?, resources: Resources): String {
        if (path == null || context == null) return path.orEmpty()

        return getStorageDirectories(context)
            .firstOrNull { path.startsWith(it) }
            ?.let { storageDevice -> userFriendlyDisplay(path, storageDevice, resources) }
            ?: path
    }

    private fun userFriendlyDisplay(path: String, storageDevice: String, resources: Resources): String {
        val storageFolder = path.drop(storageDevice.length + 1)
        val folderDisplayName = StandardDirectory.fromPath(storageFolder)
            ?.let { " " + resources.getString(it.displayName) }
            ?: storageFolder

        val deviceDisplayName = if (storageDevice.startsWith(Environment.getExternalStorageDirectory().absolutePath)) {
            resources.getString(R.string.storage_internal_storage)
        } else {
            File(storageDevice).name
        }

        return resources.getString(R.string.local_folder_friendly_path, deviceDisplayName, folderDisplayName)
    }

    private fun checkStoragePermission(context: Context): Boolean =
        ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    private fun getExtSdCardPathsForActivity(context: Context): List<String> = context
        .getExternalFilesDirs(EXTERNAL_FILES_TYPE)
        .filterNotNull()
        .mapNotNull(::storageRootOf)
        .ifEmpty { listOf(DEFAULT_EXT_SD_CARD_PATH) }

    private fun storageRootOf(externalFilesDir: File): String? {
        val absolutePath = externalFilesDir.absolutePath
        val index = absolutePath.lastIndexOf(ANDROID_DATA_FOLDER)
        if (index < 0) {
            Log_OC.w(TAG, "Unexpected external file dir: $absolutePath")
            return null
        }

        val path = absolutePath.substring(0, index)
        return try {
            File(path).canonicalPath
        } catch (_: IOException) {
            path
        }
    }

    private fun canListFiles(f: File): Boolean = f.canRead() && f.isDirectory

    @Suppress("TooGenericExceptionThrown")
    @JvmStatic
    fun checkIfEnoughSpace(file: OCFile): Boolean {
        val availableSpaceOnDevice = FileOperationsHelper.getAvailableSpaceOnDevice()
        if (availableSpaceOnDevice == UNKNOWN_AVAILABLE_SPACE) {
            throw RuntimeException("Error while computing available space")
        }

        return checkIfEnoughSpace(availableSpaceOnDevice, file)
    }

    @VisibleForTesting
    @JvmStatic
    fun checkIfEnoughSpace(availableSpaceOnDevice: Long, file: OCFile): Boolean = if (file.isFolder) {
        availableSpaceOnDevice > file.fileLength - localFolderSize(file)
    } else {
        availableSpaceOnDevice > file.fileLength
    }

    private fun localFolderSize(file: OCFile): Long = file.storagePath?.let { getFolderSize(File(it)) } ?: 0L
}
