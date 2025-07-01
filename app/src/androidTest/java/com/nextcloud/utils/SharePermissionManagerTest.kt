/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import com.owncloud.android.datamodel.quickPermission.QuickPermissionType
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.extensions.isAllowDownloadAndSyncEnabled
import com.owncloud.android.lib.resources.shares.extensions.toggleAllowDownloadAndSync
import com.owncloud.android.ui.fragment.util.SharePermissionManager
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

@Suppress("TooManyFunctions")
class SharePermissionManagerTest {

    private fun createShare(sharePermission: Int, isFolder: Boolean = false, attributesJson: String? = null): OCShare {
        return if (isFolder) {
            OCShare("/test")
                .apply {
                    permissions = sharePermission
                    attributes = attributesJson
                    shareType = ShareType.INTERNAL
                    sharedDate = 1188206955
                    shareWith = "User 1"
                    sharedWithDisplayName = "User 1"
                }
        } else {
            OCShare("/test.png")
                .apply {
                    permissions = sharePermission
                    attributes = attributesJson
                    shareType = ShareType.INTERNAL
                    sharedDate = 1188206955
                    shareWith = "User 1"
                    sharedWithDisplayName = "User 1"
                }
        }.apply {
            this.isFolder = isFolder
        }
    }

    // region Permission change tests
    @Test
    fun testTogglePermissionShouldAddPermissionFlagWhenChecked() {
        val initialPermission = OCShare.READ_PERMISSION_FLAG
        val updatedPermission =
            SharePermissionManager.togglePermission(true, initialPermission, OCShare.UPDATE_PERMISSION_FLAG)
        val updatedShare = createShare(updatedPermission)
        assertTrue(SharePermissionManager.isCustomPermission(updatedShare))
    }

    @Test
    fun testTogglePermissionShouldRemovePermissionFlagWhenUnchecked() {
        val initialPermission = OCShare.READ_PERMISSION_FLAG + OCShare.UPDATE_PERMISSION_FLAG
        val updatedPermission =
            SharePermissionManager.togglePermission(false, initialPermission, OCShare.UPDATE_PERMISSION_FLAG)
        val updatedShare = createShare(updatedPermission)
        assertTrue(SharePermissionManager.isViewOnly(updatedShare))
    }
    // endregion

    // region HasPermissions tests
    @Test
    fun testHasPermissionShouldReturnTrueIfPermissionPresent() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.UPDATE_PERMISSION_FLAG
        assertTrue(SharePermissionManager.hasPermission(permission, OCShare.UPDATE_PERMISSION_FLAG))
    }

    @Test
    fun testHasPermissionShouldReturnFalseIfPermissionNotPresent() {
        val permission = OCShare.READ_PERMISSION_FLAG
        assertFalse(SharePermissionManager.hasPermission(permission, OCShare.UPDATE_PERMISSION_FLAG))
    }
    // endregion

    // region Helper Method Tests
    @Test
    fun testCanEditShouldReturnTrueIfAllPermissionsPresent() {
        val share = createShare(OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER, isFolder = true)
        assertTrue(SharePermissionManager.canEdit(share))
    }

    @Test
    fun testCanEditShouldReturnFalseIfPermissionsAreInsufficient() {
        val share = createShare(OCShare.READ_PERMISSION_FLAG)
        assertFalse(SharePermissionManager.canEdit(share))
    }

    @Test
    fun testIsViewOnlyShouldReturnTrueIfOnlyReadPermissionSet() {
        val share = createShare(OCShare.READ_PERMISSION_FLAG)
        assertTrue(SharePermissionManager.isViewOnly(share))
    }

    @Test
    fun testIsFileRequestShouldReturnTrueIfOnlyCreatePermissionSetOnFolder() {
        val share = createShare(OCShare.CREATE_PERMISSION_FLAG, isFolder = true)
        assertTrue(SharePermissionManager.isFileRequest(share))
    }

    @Test
    fun testIsFileRequestShouldReturnFalseIfOnlyCreatePermissionSetOnFile() {
        val share = createShare(OCShare.CREATE_PERMISSION_FLAG)
        assertFalse(SharePermissionManager.isFileRequest(share))
    }

    @Test
    fun testIsSecureFileDropShouldReturnTrueIfReadAndCreatePermissionsPresent() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.CREATE_PERMISSION_FLAG
        val share = createShare(permission)
        assertTrue(SharePermissionManager.isSecureFileDrop(share))
    }

    @Test
    fun testCanReshareShouldReturnTrueIfSharePermissionIsPresent() {
        val share = createShare(OCShare.SHARE_PERMISSION_FLAG)
        assertTrue(SharePermissionManager.canReshare(share))
    }

    @Test
    fun testGetMaximumPermissionForFolder() {
        assertEquals(
            OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER,
            SharePermissionManager.getMaximumPermission(isFolder = true)
        )
    }

    @Test
    fun testGetMaximumPermissionForFile() {
        assertEquals(
            OCShare.MAXIMUM_PERMISSIONS_FOR_FILE,
            SharePermissionManager.getMaximumPermission(isFolder = false)
        )
    }
    // endregion

    // region GetSelectedTypeTests
    @Test
    fun testGetSelectedTypeShouldReturnCanEditWhenFullPermissionsGiven() {
        val share = createShare(OCShare.MAXIMUM_PERMISSIONS_FOR_FILE)
        assertEquals(QuickPermissionType.CAN_EDIT, SharePermissionManager.getSelectedType(share, encrypted = false))
    }

    @Test
    fun testGetSelectedTypeShouldReturnSecureFileDropWhenEncryptedAndReadCreateGiven() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.CREATE_PERMISSION_FLAG
        val share = createShare(permission)
        assertEquals(
            QuickPermissionType.SECURE_FILE_DROP,
            SharePermissionManager.getSelectedType(share, encrypted = true)
        )
    }

    @Test
    fun testGetSelectedTypeShouldReturnFileRequestWhenCreatePermissionGiven() {
        val share = createShare(OCShare.CREATE_PERMISSION_FLAG, isFolder = true)
        assertEquals(QuickPermissionType.FILE_REQUEST, SharePermissionManager.getSelectedType(share, encrypted = false))
    }

    @Test
    fun testGetSelectedTypeShouldReturnViewOnlyWhenReadPermissionGiven() {
        val share = createShare(OCShare.READ_PERMISSION_FLAG)
        assertEquals(QuickPermissionType.VIEW_ONLY, SharePermissionManager.getSelectedType(share, encrypted = false))
    }

    @Test
    fun testGetSelectedTypeShouldReturnCustomPermissionOnlyWhenCustomPermissionGiven() {
        val share = createShare(OCShare.READ_PERMISSION_FLAG + OCShare.UPDATE_PERMISSION_FLAG)
        assertEquals(
            QuickPermissionType.CUSTOM_PERMISSIONS,
            SharePermissionManager.getSelectedType(share, encrypted = false)
        )
    }

    @Test
    fun testGetSelectedTypeShouldReturnNoneOnlyWhenNoPermissionGiven() {
        val share = createShare(OCShare.NO_PERMISSION)
        assertEquals(
            QuickPermissionType.NONE,
            SharePermissionManager.getSelectedType(share, encrypted = false)
        )
    }
    // endregion

    // region CustomPermissions Tests
    @Test
    fun testIsCustomPermissionShouldReturnFalseWhenNoPermissionsGiven() {
        val permission = OCShare.NO_PERMISSION
        val share = createShare(permission, isFolder = false)
        assertFalse(SharePermissionManager.isCustomPermission(share))
    }

    @Test
    fun testIsCustomPermissionShouldReturnFalseWhenNoReadPermissionsGiven() {
        val permission = OCShare.SHARE_PERMISSION_FLAG + OCShare.UPDATE_PERMISSION_FLAG
        val share = createShare(permission, isFolder = false)
        assertFalse(SharePermissionManager.isCustomPermission(share))
    }

    @Test
    fun testIsCustomPermissionShouldReturnTrueWhenUpdatePermissionsGivenOnFile() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.UPDATE_PERMISSION_FLAG
        val share = createShare(permission, isFolder = false)
        assertTrue(SharePermissionManager.isCustomPermission(share))
    }

    @Test
    fun testIsCustomPermissionShouldReturnTrueWhenUpdateAndSharePermissionsGivenOnFile() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.UPDATE_PERMISSION_FLAG + OCShare.SHARE_PERMISSION_FLAG
        val share = createShare(permission, isFolder = false)
        assertTrue(SharePermissionManager.isCustomPermission(share))
    }

    @Test
    fun testIsCustomPermissionShouldReturnFalseWhenCreatePermissionsGivenOnFile() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.CREATE_PERMISSION_FLAG
        val share = createShare(permission, isFolder = false)
        assertFalse(SharePermissionManager.isCustomPermission(share))
    }

    @Test
    fun testIsCustomPermissionShouldReturnFalseWhenDeletePermissionsGivenOnFile() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.DELETE_PERMISSION_FLAG
        val share = createShare(permission, isFolder = false)
        assertFalse(SharePermissionManager.isCustomPermission(share))
    }

    @Test
    fun testIsCustomPermissionShouldReturnTrueWhenCreatePermissionsGivenOnFolder() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.CREATE_PERMISSION_FLAG
        val share = createShare(permission, isFolder = true)
        assertTrue(SharePermissionManager.isCustomPermission(share))
    }

    @Test
    fun testIsCustomPermissionShouldReturnTrueWhenMixedPermissionsOnFile() {
        val permission = OCShare.READ_PERMISSION_FLAG + OCShare.UPDATE_PERMISSION_FLAG
        val share = createShare(permission, isFolder = false)
        assertTrue(SharePermissionManager.isCustomPermission(share))
    }
    // endregion

    // region Attributes Tests
    @Test
    fun testToggleAllowDownloadAndSyncShouldCreateAttributeJsonIfNoneExists() {
        val ocShare = OCShare().apply {
            isFolder = true
            shareType = ShareType.USER
            permissions = 17
        }
        ocShare.attributes = ocShare.toggleAllowDownloadAndSync(
            isChecked = true,
            useV2DownloadAttributes = false
        )
        assertTrue(ocShare.isAllowDownloadAndSyncEnabled(false))
    }

    @Test
    fun testIsAllowDownloadAndSyncEnabledShouldReturnFalseIfAttributeIsMissing() {
        val share = createShare(OCShare.READ_PERMISSION_FLAG, attributesJson = null)
        assertFalse(share.isAllowDownloadAndSyncEnabled(false))
    }
    // endregion
}
