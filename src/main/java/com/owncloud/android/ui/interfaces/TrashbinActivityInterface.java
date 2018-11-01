/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
