package com.owncloud.android.ui.dialog;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.helpers.FileOperationsHelper;

class Controller {
    FileOperationsHelper fileOperationsHelper;

    Controller(FileOperationsHelper fileOperationsHelper) {
        this.fileOperationsHelper = fileOperationsHelper;
    }

    public void verifyAndCreateFolder(OCFile mParentFolder, String newFolderName) {
        if (newFolderName.length() <= 0) {
            throw new RuntimeException(String.valueOf(R.string.filename_empty));
        }
        boolean serverWithForbiddenChars = fileOperationsHelper.isVersionWithForbiddenCharacters();

        if (!FileUtils.isValidName(newFolderName, serverWithForbiddenChars)) {

            if (serverWithForbiddenChars) {
                //DisplayUtils.showSnackMessage(getActivity(), R.string.filename_forbidden_charaters_from_server);
            } else {
                //DisplayUtils.showSnackMessage(getActivity(), R.string.filename_forbidden_characters);
            }

            return;
        }

        String path = mParentFolder.getRemotePath();
        path += newFolderName + OCFile.PATH_SEPARATOR;

        fileOperationsHelper.createFolder(path, false);
    }
}
