/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * @author TSI-mc
 * Copyright (C) 2018 Andy Scherzinger
 * Copyright (C) 2021 TSI-mc
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.fragment.util;

import android.content.Context;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.quickPermission.QuickPermissionType;
import com.owncloud.android.lib.resources.shares.OCShare;

import static com.owncloud.android.lib.resources.shares.OCShare.CREATE_PERMISSION_FLAG;
import static com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FILE;
import static com.owncloud.android.lib.resources.shares.OCShare.MAXIMUM_PERMISSIONS_FOR_FOLDER;
import static com.owncloud.android.lib.resources.shares.OCShare.NO_PERMISSION;
import static com.owncloud.android.lib.resources.shares.OCShare.READ_PERMISSION_FLAG;
import static com.owncloud.android.lib.resources.shares.OCShare.SHARE_PERMISSION_FLAG;

/**
 * Helper calls for visibility logic of the sharing menu.
 */
public final class SharingMenuHelper {

    private static final SharePermissionManager sharePermissionManager = new SharePermissionManager();

    private SharingMenuHelper() {
        // utility class -> private constructor
    }

    public static boolean canEdit(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & (share.isFolder() ? MAXIMUM_PERMISSIONS_FOR_FOLDER :
            MAXIMUM_PERMISSIONS_FOR_FILE)) == (share.isFolder() ? MAXIMUM_PERMISSIONS_FOR_FOLDER :
            MAXIMUM_PERMISSIONS_FOR_FILE);
    }

    public static boolean isViewOnly(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & ~SHARE_PERMISSION_FLAG) == READ_PERMISSION_FLAG;
    }

    public static boolean isFileRequest(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & ~SHARE_PERMISSION_FLAG) == CREATE_PERMISSION_FLAG;
    }

    public static boolean isSecureFileDrop(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & ~SHARE_PERMISSION_FLAG) == CREATE_PERMISSION_FLAG + READ_PERMISSION_FLAG;
    }

    public static String getPermissionName(Context context, OCShare share) {
        final var res = context.getResources();

        if (SharingMenuHelper.canEdit(share)) {
            return res.getString(R.string.share_permission_can_edit);
        } else if (SharingMenuHelper.isViewOnly(share)) {
            return res.getString(R.string.share_permission_view_only);
        } else if (sharePermissionManager.isCustomPermission(share)) {
            return res.getString(R.string.share_custom_permission);
        } else if (SharingMenuHelper.isSecureFileDrop(share)) {
            return res.getString(R.string.share_permission_secure_file_drop);
        } else if (SharingMenuHelper.isFileRequest(share)) {
            return res.getString(R.string.link_share_file_request);
        }

        return null;
    }

    public static QuickPermissionType getSelectedType(OCShare share) {
        if (SharingMenuHelper.isViewOnly(share)) {
            return QuickPermissionType.VIEW_ONLY;
        } else if (SharingMenuHelper.canEdit(share)) {
            return QuickPermissionType.CAN_EDIT;
        } else if (SharingMenuHelper.isFileRequest(share)) {
            return QuickPermissionType.FILE_REQUEST;
        } else if (sharePermissionManager.isCustomPermission(share)) {
            return QuickPermissionType.CUSTOM_PERMISSIONS;
        }

        return QuickPermissionType.NONE;
    }

    public static boolean canReshare(OCShare share) {
        return (share.getPermissions() & SHARE_PERMISSION_FLAG) > 0;
    }
}
