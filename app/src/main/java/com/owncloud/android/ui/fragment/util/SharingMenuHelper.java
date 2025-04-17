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

    public static boolean isUploadAndEditingAllowed(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & (share.isFolder() ? MAXIMUM_PERMISSIONS_FOR_FOLDER :
            MAXIMUM_PERMISSIONS_FOR_FILE)) == (share.isFolder() ? MAXIMUM_PERMISSIONS_FOR_FOLDER :
            MAXIMUM_PERMISSIONS_FOR_FILE);
    }

    public static boolean isReadOnly(OCShare share) {
        if (share.getPermissions() == NO_PERMISSION) {
            return false;
        }

        return (share.getPermissions() & ~SHARE_PERMISSION_FLAG) == READ_PERMISSION_FLAG;
    }

    public static boolean isFileDrop(OCShare share) {
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

        if (SharingMenuHelper.isUploadAndEditingAllowed(share)) {
            return res.getString(R.string.share_permission_can_edit);
        } else if (SharingMenuHelper.isReadOnly(share)) {
            return res.getString(R.string.share_permission_view_only);
        } else if (sharePermissionManager.isCustomPermission(share)) {
            return res.getString(R.string.share_custom_permission);
        } else if (SharingMenuHelper.isSecureFileDrop(share)) {
            return res.getString(R.string.share_permission_secure_file_drop);
        } else if (SharingMenuHelper.isFileDrop(share)) {
            return res.getString(R.string.share_permission_file_drop);
        }

        return null;
    }

    /**
     * method to get the current checked index from the list of permissions
     *
     */
    public static int getPermissionCheckedItem(Context context, OCShare share, String[] permissionArray) {
        int permissionName;

        if (SharingMenuHelper.isUploadAndEditingAllowed(share)) {
            permissionName = share.isFolder() ? R.string.share_permission_can_edit : R.string.link_share_editing;
        } else if (SharingMenuHelper.isReadOnly(share)) {
            permissionName = R.string.link_share_view_only;
        } else if (SharingMenuHelper.isFileDrop(share)) {
            permissionName = R.string.link_share_file_request;
        } else if (sharePermissionManager.isCustomPermission(share)) {
            permissionName = R.string.share_custom_permission;
        } else {
            return 0;
        }

        return getPermissionIndexFromArray(context, permissionArray, permissionName);
    }

    private static int getPermissionIndexFromArray(Context context, String[] permissionArray, int permissionName) {
        for (int i = 0; i < permissionArray.length; i++) {
            if (permissionArray[i].equalsIgnoreCase(context.getResources().getString(permissionName))) {
                return i;
            }
        }
        return 0;
    }

    public static boolean canReshare(OCShare share) {
        return (share.getPermissions() & SHARE_PERMISSION_FLAG) > 0;
    }
}
