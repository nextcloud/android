package com.owncloud.android.oc_framework_test_project.test;


import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Class to test Read Folder Operation
 * @author masensio
 *
 */

public class ReadFolderTest extends	ActivityInstrumentationTestCase2<TestActivity> {
	

	/* Folder data to read. This folder must exist on the account */
	private final String mRemoteFolderPath = "/folderToRead";
	
	
	private TestActivity mActivity;
	
	public ReadFolderTest() {
	    super(TestActivity.class);
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	}

	/**
	 * Test Read Folder
	 */
	public void testReadFolder() {

		RemoteOperationResult result = mActivity.readFile(mRemoteFolderPath);
		assertTrue(result.getData().size() > 1);
		assertTrue(result.getData().size() == 4);
		assertTrue(result.isSuccess());
	}
	
}
