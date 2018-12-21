package com.owncloud.android.ui.dialog;

import com.owncloud.android.datamodel.OCFile;
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
public class ControllerTest {

    @Mock
    FileOperationsHelper fileOperationsHelper;

    @Test
    public void testSuccess() {
        // setup
        Controller sut = new Controller(fileOperationsHelper);

        // test
        sut.verifyAndCreateFolder(new OCFile("/123/"), "TestOrdner");

        // verify
        Mockito.verify(fileOperationsHelper).createFolder(Mockito.eq("/123/TestOrdner/"), Mockito.eq(false));
    }

    @Test(expected = RuntimeException.class)
    public void testEmpty() {
        // setup
        Controller sut = new Controller(fileOperationsHelper);

        // test
        sut.verifyAndCreateFolder(new OCFile("/123/"), "");
    }
}
