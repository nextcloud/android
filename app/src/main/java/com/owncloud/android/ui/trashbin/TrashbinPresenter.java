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

import com.owncloud.android.R;
import com.owncloud.android.lib.resources.trashbin.model.TrashbinFile;

import java.io.File;
import java.util.List;

import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

/**
 * Coordinates between model and view: querying model, updating view, react to UI input
 */
public class TrashbinPresenter implements TrashbinContract.Presenter {

    private TrashbinContract.View trashbinView;
    private TrashbinRepository trashbinRepository;
    private String currentPath = ROOT_PATH;

    public TrashbinPresenter(TrashbinRepository trashbinRepository, TrashbinContract.View trashbinView) {
        this.trashbinRepository = trashbinRepository;
        this.trashbinView = trashbinView;
    }

    @Override
    public void enterFolder(String folder) {
        currentPath = folder;
        loadFolder();
    }

    @Override
    public boolean isRoot() {
        return !ROOT_PATH.equals(currentPath);
    }

    @Override
    public void navigateUp() {
        if (ROOT_PATH.equals(currentPath)) {
            trashbinView.close();
        } else {
            currentPath = new File(currentPath).getParent();

            loadFolder();
        }

        trashbinView.setDrawerIndicatorEnabled(ROOT_PATH.equals(currentPath));
    }

    @Override
    public void loadFolder() {
        trashbinRepository.getFolder(currentPath, new TrashbinRepository.LoadFolderCallback() {
            @Override
            public void onSuccess(List<Object> files) {
                trashbinView.showTrashbinFolder(files);
            }

            @Override
            public void onError(int error) {
                trashbinView.showError(error);
            }
        });
    }

    @Override
    public void restoreTrashbinFile(TrashbinFile file) {
        trashbinRepository.restoreFile(file, success -> {
            if (success) {
                trashbinView.removeFile(file);
            } else {
                trashbinView.showSnackbarError(R.string.trashbin_file_not_restored, file);
            }
        });
    }

    @Override
    public void removeTrashbinFile(TrashbinFile file) {
        trashbinRepository.removeTrashbinFile(file, success -> {
            if (success) {
                trashbinView.removeFile(file);
            } else {
                trashbinView.showSnackbarError(R.string.trashbin_file_not_deleted, file);
            }
        });
    }

    @Override
    public void emptyTrashbin() {
        trashbinRepository.emptyTrashbin(success -> {
            if (success) {
                trashbinView.removeAllFiles();
            } else {
                trashbinView.showError(R.string.trashbin_not_emptied);
            }
        });
    }
}
