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
 * Contract between view (TrashbinActivity) and presenter (TrashbinPresenter)
 */
public interface TrashbinContract {

    interface View {
        void showTrashbinFolder(List<Object> trashbinFiles);

        void showSnackbarError(int message, TrashbinFile file);

        void showError(int message);

        void removeFile(TrashbinFile file);

        void removeAllFiles();

        void close();

        void setDrawerIndicatorEnabled(boolean bool);
    }

    interface Presenter {

        boolean isRoot();

        void loadFolder();

        void navigateUp();

        void enterFolder(String folder);

        void restoreTrashbinFile(TrashbinFile file);

        void removeTrashbinFile(TrashbinFile file);

        void emptyTrashbin();
    }
}
