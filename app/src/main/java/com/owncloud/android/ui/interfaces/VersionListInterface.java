/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2018 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.interfaces;

import com.owncloud.android.lib.resources.files.model.FileVersion;

public interface VersionListInterface {

    interface View {
        void onRestoreClicked(FileVersion fileVersion);
    }

    interface CommentCallback {
        void onSuccess();

        void onError(int error);
    }
}
