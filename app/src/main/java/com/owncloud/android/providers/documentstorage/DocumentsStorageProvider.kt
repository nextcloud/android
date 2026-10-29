/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.providers.documentstorage

import com.owncloud.android.providers.DiskLruImageCacheFileProvider
import com.owncloud.android.providers.documentstorage.model.Document
import com.owncloud.android.providers.documentstorage.model.DocumentResolver
import com.owncloud.android.providers.documentstorage.modify.DocumentCreator
import com.owncloud.android.providers.documentstorage.modify.DocumentEditor
import com.owncloud.android.providers.documentstorage.modify.DocumentNameValidator
import com.owncloud.android.providers.documentstorage.open.DocumentOpener
import com.owncloud.android.providers.documentstorage.query.DocumentLister
import com.owncloud.android.providers.documentstorage.sync.DocumentNotifier
import com.owncloud.android.providers.documentstorage.sync.FolderRefresher
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.Point
import android.os.Binder
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.os.Process
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import androidx.annotation.VisibleForTesting
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.preferences.AppPreferencesImpl
import com.nextcloud.client.utils.HashUtil
import com.nextcloud.utils.extensions.getSmallThumbnailKey
import com.nextcloud.utils.extensions.showToast
import com.owncloud.android.MainApp
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RemoveFileOperation
import com.owncloud.android.ui.activity.SettingsActivity
import dagger.android.AndroidInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.nextcloud.providers.cursors.FileCursor
import org.nextcloud.providers.cursors.RootCursor
import java.io.FileNotFoundException
import javax.inject.Inject

class DocumentsStorageProvider : DocumentsProvider() {

    @Inject
    lateinit var accountManager: UserAccountManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val resolver = DocumentResolver()
    private val notifier by lazy { DocumentNotifier(nonNullContext) }
    private val folderRefresher by lazy { FolderRefresher(nonNullContext) }
    private val nameValidator by lazy { DocumentNameValidator(nonNullContext, accountManager) }
    private val lister by lazy { DocumentLister(nonNullContext, scope, notifier, folderRefresher) }
    private val opener by lazy { DocumentOpener(nonNullContext, scope) }
    private val creator by lazy {
        DocumentCreator(nonNullContext, accountManager, resolver, nameValidator, folderRefresher, notifier)
    }
    private val editor by lazy { DocumentEditor(nonNullContext, resolver, notifier, folderRefresher, nameValidator) }

    private val nonNullContext: Context
        get() = checkNotNull(context)

    private val isAppLocked: Boolean
        get() = AppPreferencesImpl.fromContext(MainApp.getAppContext()).lockPreference in
            listOf(SettingsActivity.LOCK_PASSCODE, SettingsActivity.LOCK_DEVICE_CREDENTIALS)

    override fun onCreate(): Boolean {
        AndroidInjection.inject(this)
        initStoragePathIfMissing()
        refreshStorageManagers()
        return true
    }

    private fun initStoragePathIfMissing() {
        if (MainApp.getStoragePath() != null) {
            return
        }

        val defaultStoragePath = nonNullContext.filesDir.absolutePath
        MainApp.setStoragePath(AppPreferencesImpl.fromContext(nonNullContext).getStoragePath(defaultStoragePath))
    }

    private fun refreshStorageManagers() {
        resolver.refresh(accountManager.allUsers, nonNullContext.contentResolver)
    }

    override fun queryRoots(projection: Array<String>?): Cursor = withClearedCallingIdentity {
        refreshStorageManagers()

        if (isAppLocked) {
            FileCursor(emptyArray())
        } else {
            RootCursor(projection).apply {
                resolver.storageManagers.forEach { manager ->
                    addRoot(Document.fromPath(manager, OCFile.ROOT_PATH), nonNullContext)
                }
            }
        }
    }

    override fun queryDocument(documentId: String, projection: Array<String>?): Cursor {
        Log_OC.d(TAG, "queryDocument(), id=$documentId")

        return withClearedCallingIdentity {
            val document = resolver.toDocument(documentId)
            document.requireFile()
            FileCursor(projection).apply { addFile(document) }
        }
    }

    @SuppressLint("LongLogTag")
    override fun queryChildDocuments(parentDocumentId: String, projection: Array<String>?, sortOrder: String?): Cursor {
        Log_OC.d(TAG, "queryChildDocuments(), id=$parentDocumentId")

        return withClearedCallingIdentity {
            lister.listChildren(resolver.toDocument(parentDocumentId), projection)
        }
    }

    override fun querySearchDocuments(rootId: String?, query: String, projection: Array<String>?): Cursor {
        Log_OC.d(TAG, "querySearchDocuments(), rootId=$rootId")

        return withClearedCallingIdentity {
            lister.search(resolver.storageManagerFor(rootId), query, projection)
        }
    }

    @SuppressLint("LongLogTag")
    override fun openDocument(
        documentId: String,
        mode: String?,
        cancellationSignal: CancellationSignal?
    ): ParcelFileDescriptor? {
        val callerUid = Binder.getCallingUid()
        Log_OC.d(TAG, "openDocument(), id=$documentId, mode=$mode, callerUid=$callerUid")

        if (!editor.isFolderPathValid) {
            Log_OC.d(TAG, "Folder path is not valid, operation is cancelled")
            return null
        }

        return withClearedCallingIdentity {
            try {
                opener.open(resolver.toDocument(documentId), mode, callerUid == Process.SYSTEM_UID)
            } catch (e: Exception) {
                Log_OC.e(TAG, "openDocument() failed, id=$documentId, mode=$mode", e)
                throw e
            }
        }
    }

    override fun openDocumentThumbnail(
        documentId: String,
        sizeHint: Point?,
        signal: CancellationSignal?
    ): AssetFileDescriptor {
        Log_OC.d(TAG, "openDocumentThumbnail(), id=$documentId")

        return withClearedCallingIdentity {
            val document = resolver.toDocument(documentId)
            val file = document.requireFile()

            if (!ThumbnailsCacheManager.containsBitmap(file.getSmallThumbnailKey())) {
                ThumbnailsCacheManager.generateThumbnailFromOCFile(file, document.user, context)
            }

            val descriptor = DiskLruImageCacheFileProvider.getParcelFileDescriptorForOCFile(file)
            AssetFileDescriptor(descriptor, 0, file.fileLength)
        }
    }

    override fun renameDocument(documentId: String, displayName: String): String? {
        Log_OC.d(TAG, "renameDocument(), id=$documentId")

        withClearedCallingIdentity { editor.rename(documentId, displayName) }
        return null
    }

    override fun copyDocument(sourceDocumentId: String, targetParentDocumentId: String): String? {
        Log_OC.d(TAG, "copyDocument(), id=$sourceDocumentId")

        return withClearedCallingIdentity { editor.copy(sourceDocumentId, targetParentDocumentId) }
    }

    override fun moveDocument(
        sourceDocumentId: String,
        sourceParentDocumentId: String,
        targetParentDocumentId: String
    ): String? {
        Log_OC.d(TAG, "moveDocument(), id=$sourceDocumentId")

        return withClearedCallingIdentity {
            editor.move(sourceDocumentId, sourceParentDocumentId, targetParentDocumentId)
        }
    }

    override fun createDocument(documentId: String, mimeType: String?, displayName: String): String? {
        Log_OC.d(TAG, "createDocument(), id=$documentId, name=$displayName, mimeType=$mimeType")

        return withClearedCallingIdentity { creator.create(documentId, mimeType, displayName) }
    }

    override fun removeDocument(documentId: String, parentDocumentId: String?) {
        deleteDocument(documentId)
    }

    override fun deleteDocument(documentId: String) {
        Log_OC.d(TAG, "deleteDocument(), id=$documentId")

        val document = resolver.toDocument(documentId)
        val file = document.requireFile()
        if (!file.canDeleteOrLeaveShare()) {
            nonNullContext.showToast(R.string.document_storage_provider_cannot_delete)
            return
        }

        val parentFolder = document.parent
        recursiveRevokePermission(document)

        withClearedCallingIdentity {
            val result = RemoveFileOperation(file, false, document.user, true, nonNullContext, document.storageManager)
                .execute(document.requireClient(nonNullContext))

            if (!result.isSuccess) {
                throw FileNotFoundException("Failed to delete document with documentId $documentId")
            }
            parentFolder?.let(notifier::notifyChanged)
        }
    }

    private fun recursiveRevokePermission(document: Document) {
        val file = document.file
        if (file?.isFolder == true) {
            val storageManager = document.storageManager
            storageManager.getFolderContent(file, false).forEach { child ->
                recursiveRevokePermission(Document(storageManager, child))
            }
        }

        revokeDocumentPermission(document.documentId)
    }

    override fun isChildDocument(parentDocumentId: String, documentId: String): Boolean {
        Log_OC.d(TAG, "isChildDocument(), parent=$parentDocumentId, id=$documentId")

        return withClearedCallingIdentity {
            try {
                resolver.isChildDocument(parentDocumentId, documentId)
            } catch (e: FileNotFoundException) {
                Log_OC.e(TAG, "failed to check for child document", e)
                false
            }
        }
    }

    private inline fun <T> withClearedCallingIdentity(block: () -> T): T {
        val token = Binder.clearCallingIdentity()
        try {
            return block()
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }

    companion object {
        private val TAG = DocumentsStorageProvider::class.java.simpleName

        @VisibleForTesting
        const val DOCUMENTID_SEPARATOR = "/"

        @JvmStatic
        fun notifyRootsChanged(context: Context) {
            val authority = context.getString(R.string.document_provider_authority)
            val rootsUri = DocumentsContract.buildRootsUri(authority)
            context.contentResolver.notifyChange(rootsUri, null)
        }

        @VisibleForTesting
        fun rootIdForUser(user: User): String = HashUtil.md5Hash(user.accountName)
    }
}
