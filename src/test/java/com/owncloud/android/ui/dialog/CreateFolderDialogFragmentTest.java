package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.support.v4.app.FragmentHostCallback;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.helpers.FileOperationsHelper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Created by daniel on 22.03.18.
 */

@RunWith(MockitoJUnitRunner.class)
public class CreateFolderDialogFragmentTest {

    @Mock
    FragmentHostCallback mHost;

    @Mock
    Dialog dialog;
    @Mock
    TextView textView;
    @Mock
    FileActivity componentsGetter;
    @Mock
    FileOperationsHelper fileOperationsHelper;
    @Mock
    OCFile parentFolder;

    @Test
    public void testSuccess() {
        // setup
        CreateFolderDialogFragment sut = new CreateFolderDialogFragment();
        sut.localDialog = dialog;
        Mockito.when(dialog.findViewById(R.id.user_input)).thenReturn(textView);
        Mockito.when(textView.getText()).thenReturn("TestOrdner");

        sut.activity = componentsGetter;

        Mockito.when(componentsGetter.getFileOperationsHelper()).thenReturn(fileOperationsHelper);
        Mockito.when(fileOperationsHelper.isVersionWithForbiddenCharacters()).thenReturn(false);

        sut.mParentFolder = parentFolder;
        Mockito.when(parentFolder.getRemotePath()).thenReturn("/123/");

        // test
        sut.onClick(null, AlertDialog.BUTTON_POSITIVE);

        // verify
        Mockito.verify(fileOperationsHelper).createFolder(Mockito.eq("/123/TestOrdner/"), Mockito.eq(false));
    }
}
