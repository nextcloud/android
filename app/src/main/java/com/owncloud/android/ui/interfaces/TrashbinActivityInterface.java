/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.interfaces;

import android.view.View;

import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;

/**
 * Interface for communication between {@link com.owncloud.android.ui.trashbin.TrashbinActivity}
 * and {@link com.owncloud.android.ui.adapter.TrashbinListAdapter}
 */

public interface TrashbinActivityInterface {
    void onOverflowIconClicked(TrashbinFile file, View view);

    void onItemClicked(TrashbinFile file);

    boolean onLongItemClicked(TrashbinFile file);

    void onRestoreIconClicked(TrashbinFile file);
}
