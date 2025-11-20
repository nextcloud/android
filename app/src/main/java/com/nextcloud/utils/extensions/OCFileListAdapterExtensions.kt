/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.e2e.v1.decrypted.DecryptedFolderMetadataFileV1
import com.owncloud.android.datamodel.e2e.v2.decrypted.DecryptedFolderMetadataFile
import com.owncloud.android.db.ProviderMeta
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.resources.files.model.RemoteFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.ui.adapter.OCFileListAdapter
import com.owncloud.android.ui.adapter.OCShareToOCFileConverter
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.utils.FileStorageUtils
import android.content.ContentValues
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.datamodel.VirtualFolderType

fun OCFileListAdapter.parseAndSaveShares(data: List<Any>): List<OCFile> {
    val shares = data.filterIsInstance<OCShare>()
    val files: List<OCFile> = OCShareToOCFileConverter.buildOCFilesFromShares(shares)
    for (file in files) {
        FileStorageUtils.searchForLocalFileInDefaultPath(file, user.accountName)
    }
    mStorageManager.saveShares(shares)
    return files
}

fun OCFileListAdapter.parseCachedFiles(data: List<Any>): List<OCFile> {
    return data.filterIsInstance<OCFile>()
}

fun OCFileListAdapter.parseAndSaveVirtuals(
    data: List<Any>,
    searchType: SearchType
): List<OCFile> {
    val (type, onlyMedia) = when (searchType) {
        SearchType.FAVORITE_SEARCH -> VirtualFolderType.FAVORITE to false
        SearchType.GALLERY_SEARCH -> VirtualFolderType.GALLERY to true
        else -> VirtualFolderType.NONE to false
    }

    val contentValuesList = mutableListOf<ContentValues>()
    val resultFiles = mutableListOf<OCFile>()

    for (obj in data) {
        try {
            val remoteFile = obj as RemoteFile

            var ocFile = FileStorageUtils.fillOCFile(remoteFile)
            FileStorageUtils.searchForLocalFileInDefaultPath(ocFile, user.accountName)

            ocFile = mStorageManager.saveFileWithParent(ocFile, activity)

            val parent = mStorageManager.getFileById(ocFile.parentId)
            if (parent != null && (ocFile.isEncrypted || parent.isEncrypted)) {
                val metadata = RefreshFolderOperation.getDecryptedFolderMetadata(
                    true,
                    parent,
                    OwnCloudClientFactory.createOwnCloudClient(user.toPlatformAccount(), activity),
                    user,
                    activity
                ) ?: throw IllegalStateException("metadata is null")

                when (metadata) {
                    is DecryptedFolderMetadataFileV1 ->
                        RefreshFolderOperation.updateFileNameForEncryptedFileV1(
                            mStorageManager, metadata, ocFile
                        )

                    is DecryptedFolderMetadataFile ->
                        RefreshFolderOperation.updateFileNameForEncryptedFile(
                            mStorageManager, metadata, ocFile
                        )
                }

                ocFile = mStorageManager.saveFileWithParent(ocFile, activity)
            }

            if (searchType != SearchType.GALLERY_SEARCH && ocFile.isFolder) {
                val op = RefreshFolderOperation(
                    ocFile,
                    System.currentTimeMillis(),
                    true,
                    false,
                    mStorageManager,
                    user,
                    activity
                )
                op.execute(user, activity)
            }

            val passesMediaFilter =
                !onlyMedia || MimeTypeUtil.isImage(ocFile) || MimeTypeUtil.isVideo(ocFile)

            if (passesMediaFilter) {
                if (resultFiles.isEmpty() || !resultFiles.contains(ocFile)) {
                    resultFiles.add(ocFile)
                }
            }

            val cv = ContentValues().apply {
                put(ProviderMeta.ProviderTableMeta.VIRTUAL_TYPE, type.toString())
                put(ProviderMeta.ProviderTableMeta.VIRTUAL_OCFILE_ID, ocFile.fileId)
            }
            contentValuesList.add(cv)

        } catch (_: Exception) {
        }
    }

    // Save timestamp + virtual entries
    preferences.setPhotoSearchTimestamp(System.currentTimeMillis())
    mStorageManager.saveVirtuals(contentValuesList)

    return resultFiles
}
