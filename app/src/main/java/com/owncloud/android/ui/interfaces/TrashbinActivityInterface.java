/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.interfaces;

import android.view.View;

import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;
import com.owncloud.android.ui.adapter.OCFileListAdapter;

/**
 * Interface for communication between {@link com.owncloud.android.ui.fragment.OCFileListFragment}
 * and {@link OCFileListAdapter}
 */

public interface TrashbinActivityInterface {
    void onOverflowIconClicked(TrashbinFile file, View view);

    void onItemClicked(TrashbinFile file);

    void onRestoreIconClicked(TrashbinFile file, View view);
}
