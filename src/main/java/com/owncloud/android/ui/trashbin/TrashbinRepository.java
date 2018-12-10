/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2018 Tobias Kaminsky
 * Copyright (C) 2018 Nextcloud GmbH.
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
package com.owncloud.android.ui.trashbin;

import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;

import java.util.List;

/**
 * Contract between presenter and model
 */
public interface TrashbinRepository {
    interface LoadFolderCallback {
        void onSuccess(List<Object> files);

        void onError(int error);
    }

    interface OperationCallback {
        void onResult(boolean success);
    }

    void getFolder(String remotePath, LoadFolderCallback callback);

    void restoreFile(TrashbinFile file, OperationCallback callback);

    void emptyTrashbin(OperationCallback callback);

    void removeTrashbinFile(TrashbinFile file, OperationCallback callback);
}
