/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment.util;

import android.content.res.Resources;
import android.view.MenuItem;

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.shares.OCShare;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helper calls for visibility logic of the sharing menu.
 */
public final class SharingMenuHelper {

    private SharingMenuHelper() {
        // utility class -> private constructor
    }

    /**
     * Sets checked/visiblity state on the given {@link MenuItem} based on the given criteria.
     *
     * @param fileListing            the {@link MenuItem} to be setup
     * @param isFolder               flag if it is a folder
     * @param isEditingAllowed       flag if editing is allowed
     * @param publicSharePermissions share permissions of the link
     */
    public static void setupHideFileListingMenuItem(MenuItem fileListing,
                                                    boolean isFolder,
                                                    boolean isEditingAllowed,
                                                    int publicSharePermissions) {
        if (!isFolder) {
            fileListing.setVisible(false);
        } else {
            if (isEditingAllowed) {
                boolean readOnly = (publicSharePermissions & OCShare.READ_PERMISSION_FLAG) != 0;
                fileListing.setChecked(!readOnly);
            } else {
                fileListing.setVisible(false);
            }
        }
    }

    /**
     * Sets checked/visibility state on the given {@link MenuItem} based on the given criteria.
     *
     * @param menuItem the {@link MenuItem} to be setup
     */
    public static void setupHideFileDownload(MenuItem menuItem,
                                             boolean hideFileDownload,
                                             boolean isFileDrop) {
        if (isFileDrop) {
            menuItem.setVisible(false);
        } else {
            menuItem.setVisible(true);
            menuItem.setChecked(hideFileDownload);
        }
    }

    /**
     * sets up the password {@link MenuItem}'s title based on the fact if a password is present.
     *
     * @param password            the password {@link MenuItem}
     * @param isPasswordProtected flag is a password is present
     */
    public static void setupPasswordMenuItem(MenuItem password, boolean isPasswordProtected) {
        if (isPasswordProtected) {
            password.setTitle(R.string.share_password_title);
        } else {
            password.setTitle(R.string.share_no_password_title);
        }
    }

    /**
     * sets up the expiration date {@link MenuItem}'s title based on the fact if an expiration date is present.
     *
     * @param expirationDate      the expiration date {@link MenuItem}
     * @param expirationDateValue the expiration date
     * @param res                 Resources to load the corresponding strings.
     */
    public static void setupExpirationDateMenuItem(MenuItem expirationDate, long expirationDateValue, Resources res) {
        if (expirationDateValue > 0) {
            expirationDate.setTitle(res.getString(
                    R.string.share_expiration_date_label,
                    SimpleDateFormat.getDateInstance().format(new Date(expirationDateValue))
            ));
        } else {
            expirationDate.setTitle(R.string.share_no_expiration_date_label);
        }
    }
}
