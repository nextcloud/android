/*
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2018 Andy Scherzinger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.fragment;


import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;

/**
 * Actions interface to be implemented by any class that makes use of {@link FileDetailSharingMenuBottomSheetDialog}.
 */
public interface FileDetailsSharingMenuBottomSheetActions {
    /**
     * open sharing options only applicable for files
     */
    void openIn(OCShare share);

    /**
     * open advanced permission for selected share
     */
    void advancedPermissions(OCShare share);

    /**
     * open note screen to send new email
     */
    void sendNewEmail(OCShare share);

    /**
     * unshare the current share
     */
    void unShare(OCShare share);

    /**
     * send created link only valid for {@link ShareType#PUBLIC_LINK}
     */
    void sendLink(OCShare share);

}
