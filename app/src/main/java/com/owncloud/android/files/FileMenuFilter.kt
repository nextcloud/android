/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.files

import android.accounts.AccountManager
import android.content.Context
import androidx.annotation.IdRes
import androidx.core.content.pm.ShortcutManagerCompat
import com.nextcloud.android.files.FileLockingHelper.canUserUnlockFile
import com.nextcloud.client.account.User
import com.nextcloud.client.editimage.EditImageActivity.Companion.canBePreviewed
import com.nextcloud.ui.fileactions.FileAction
import com.nextcloud.utils.EditorUtils
import com.nextcloud.utils.mdm.MDMConfig.sendFilesSupport
import com.nextcloud.utils.mdm.MDMConfig.shareViaLink
import com.nextcloud.utils.mdm.MDMConfig.shareViaUser
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.ui.activity.ComponentsGetter
import com.owncloud.android.ui.helpers.FileOperationsHelper.Companion.isEndToEndEncryptionSetup
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.NextcloudServer

class FileMenuFilter internal constructor(
    private val storageManager: FileDataStorageManager,
    private val editorUtils: EditorUtils,
    private val numberOfAllFiles: Int,
    private val files: Collection<OCFile>,
    private val componentsGetter: ComponentsGetter,
    private val context: Context,
    private val overflowMenu: Boolean,
    private val user: User
) {
    private val userId: String? = AccountManager
        .get(context)
        .getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID)

    private val selection = FileSelection(files, storageManager)

    @IdRes
    fun getToHide(inSingleFileFragment: Boolean): List<Int> {
        if (files.isEmpty()) return emptyList()

        val synchronizing = FileSynchronizationChecker(files, componentsGetter, user).isAnyFileSynchronizing()
        val capability = storageManager.getCapability(user.accountName)

        return buildList {
            addAll(getHiddenEditActions(capability, synchronizing))
            addAll(getHiddenDownloadActions(synchronizing))
            addAll(getHiddenSelectionActions(inSingleFileFragment))
            addAll(getHiddenSyncActions(synchronizing))
            addAll(getHiddenShareActions(capability, inSingleFileFragment))
            addAll(getHiddenFavoriteActions(synchronizing))
            addAll(getHiddenEncryptionActions(capability.endToEndEncryption.isTrue))
            addAll(getHiddenMediaActions())
            addAll(getHiddenLockActions(capability.filesLockingVersion != null))
            addAll(FileAction.getActionsToHide(files.toHashSet()))
            if (selection.containsReadOnlyFile()) addAll(FileAction.READ_ONLY_HIDDEN_ACTIONS)
        }
    }

    private fun getHiddenEditActions(capability: OCCapability, synchronizing: Boolean) = buildList {
        val containsEncryptedOrLocked = selection.containsEncrypted || selection.containsLocked

        if (isEditHidden(capability)) add(R.id.action_edit)
        if (!selection.isSingleSelection || synchronizing || containsEncryptedOrLocked) add(R.id.action_rename_file)
        if (synchronizing || containsEncryptedOrLocked) add(R.id.action_move_or_copy)
        if (synchronizing || selection.containsLocked || selection.containsEncryptedFolderContent()) {
            add(R.id.action_remove_file)
        }
    }

    private fun getHiddenDownloadActions(synchronizing: Boolean) = buildList {
        if (selection.containsFolder || selection.anyDown || synchronizing) add(R.id.action_download_file)
        if (selection.containsFolder) add(R.id.action_export_file)
        if (!selection.isSingleFile || !selection.anyDown || synchronizing) add(R.id.action_open_file_with)
        if (isOpenInWebEditorHidden()) add(R.id.action_open_in_web_editor)
    }

    private fun getHiddenSelectionActions(inSingleFileFragment: Boolean) = buildList {
        val allFilesSelected = files.size >= numberOfAllFiles

        if (inSingleFileFragment || allFilesSelected || overflowMenu) add(R.id.action_select_all_action_menu)
        if (inSingleFileFragment || overflowMenu) add(R.id.action_deselect_all_action_menu)
        if (!selection.isSingleSelection) add(R.id.action_see_details)
        if (isPinToHomeHidden()) add(R.id.action_pin_to_homescreen)
    }

    private fun getHiddenSyncActions(synchronizing: Boolean) = buildList {
        val nothingToSync = !selection.anyDown && !selection.containsFolder

        if (!synchronizing) add(R.id.action_cancel_sync)
        if (!selection.firstFile.isOfflineOperation) add(R.id.action_retry)
        if (nothingToSync || synchronizing || selection.containsEncrypted) {
            add(R.id.action_sync_file)
            add(R.id.action_sync_all_files)
        }
    }

    private fun getHiddenShareActions(capability: OCCapability, inSingleFileFragment: Boolean) = buildList {
        val shareFileHidden = isShareFileHidden(capability)

        if (shareFileHidden) add(R.id.action_send_share_file)
        if (isSendFileHidden(inSingleFileFragment, shareFileHidden)) add(R.id.action_send_file)
    }

    private fun getHiddenFavoriteActions(synchronizing: Boolean) = buildList {
        if (synchronizing || selection.allFavorites) add(R.id.action_favorite)
        if (synchronizing || selection.noneFavorite) add(R.id.action_unset_favorite)
    }

    private fun getHiddenEncryptionActions(endToEndEncryptionEnabled: Boolean) = buildList {
        if (isEncryptHidden(endToEndEncryptionEnabled)) add(R.id.action_encrypted)
        if (isUnsetEncryptedHidden(endToEndEncryptionEnabled)) add(R.id.action_unset_encrypted)
    }

    private fun getHiddenMediaActions() = buildList {
        val firstFile = selection.firstFile

        if (!selection.isSingleImage || MimeTypeUtil.isSVG(firstFile)) add(R.id.action_set_as_wallpaper)
        if (!selection.isSingleFile || !selection.isSingleMedia || selection.containsEncryptedFile) {
            add(R.id.action_stream_media)
        }
        if (selection.containsEncryptedFile || !MimeTypeUtil.isImageOrVideo(firstFile)) add(R.id.action_add_to_album)
    }

    private fun getHiddenLockActions(fileLockingEnabled: Boolean) = buildList {
        if (isLockHidden(fileLockingEnabled)) add(R.id.action_lock_file)
        if (!selection.isSingleSelection || !fileLockingEnabled || !canUserUnlock()) add(R.id.action_unlock_file)
    }

    private fun isEditHidden(capability: OCCapability): Boolean {
        val file = selection.firstFile
        if (file.isEncrypted) return true

        val mimeType = file.mimeType
        if (isRichDocumentEditingSupported(capability, mimeType) || editorUtils.isEditorAvailable(user, mimeType)) {
            return false
        }

        return !(selection.isSingleImage && canBePreviewed(file))
    }

    @NextcloudServer(max = 18)
    private fun isRichDocumentEditingSupported(capability: OCCapability, mimeType: String?): Boolean {
        if (!selection.isSingleFile) return false

        val isSupportedMimeType = capability.richDocumentsMimeTypeList?.contains(mimeType) == true ||
            capability.richDocumentsOptionalMimeTypeList?.contains(mimeType) == true

        return isSupportedMimeType && capability.richDocumentsDirectEditing.isTrue
    }

    private fun isOpenInWebEditorHidden(): Boolean {
        if (!selection.isSingleFile) return true

        val file = selection.firstFile
        val canOpenLocally = file.isDown && !file.isEncrypted

        return !canOpenLocally || !editorUtils.isOfficeEditorAvailable(user, file.mimeType)
    }

    private fun isPinToHomeHidden(): Boolean =
        !selection.isSingleSelection || !ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    private fun isShareFileHidden(capability: OCCapability): Boolean {
        if (!selection.isSingleSelection || selection.containsEncryptedFile || selection.hasEncryptedParent()) {
            return true
        }

        return !isShareAllowed() || !isShareApiEnabled(capability) || !selection.firstFile.canReshare()
    }

    private fun isShareAllowed(): Boolean = shareViaLink(context) || shareViaUser(context)

    private fun isShareApiEnabled(capability: OCCapability): Boolean {
        val sharingApi = capability.filesSharingApiEnabled

        return sharingApi.isTrue || sharingApi.isUnknown
    }

    private fun isSendFileHidden(inSingleFileFragment: Boolean, shareFileHidden: Boolean): Boolean {
        if (!sendFilesSupport(context) || overflowMenu || selection.containsEncryptedFile) return true

        val hiddenInListing = !inSingleFileFragment && (selection.isSingleSelection || !selection.allDown)

        return hiddenInListing || !shareFileHidden
    }

    private fun isEncryptHidden(endToEndEncryptionEnabled: Boolean): Boolean {
        if (!endToEndEncryptionEnabled || !selection.isSingleFolder || selection.isEncryptedFolder) return true
        if (selection.isGroupFolder || selection.containsShared) return true

        return !selection.isEmptyFolder()
    }

    private fun isUnsetEncryptedHidden(endToEndEncryptionEnabled: Boolean): Boolean {
        if (!endToEndEncryptionEnabled || !selection.isEncryptedFolder) return true
        if (selection.hasEncryptedParent() || !selection.isEmptyFolder()) return true

        return !isEndToEndEncryptionSetup(context, user)
    }

    private fun isLockHidden(fileLockingEnabled: Boolean): Boolean {
        if (!selection.isSingleSelection || !fileLockingEnabled || selection.containsEncrypted) return true

        val file = selection.firstFile

        return file.isLocked || file.isFolder
    }

    private fun canUserUnlock(): Boolean {
        val userId = userId ?: return false

        return canUserUnlockFile(userId, selection.firstFile)
    }
}
