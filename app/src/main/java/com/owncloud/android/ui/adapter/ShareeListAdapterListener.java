/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author TSI-mc
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 * Copyright (C) 2021 TSI-mc
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.owncloud.android.ui.adapter;

import com.nextcloud.client.account.User;
import com.owncloud.android.lib.resources.shares.OCShare;

public interface ShareeListAdapterListener {
    void copyLink(OCShare share);

    void showSharingMenuActionSheet(OCShare share);

    void copyInternalLink();

    void createPublicShareLink();

    void createSecureFileDrop();

    void requestPasswordForShare(OCShare share, boolean askForPassword);

    void showPermissionsDialog(OCShare share);

    void showProfileBottomSheet(User user, String shareWith);
}
