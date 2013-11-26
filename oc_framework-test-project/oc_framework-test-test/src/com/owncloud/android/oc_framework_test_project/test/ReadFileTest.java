package com.owncloud.android.oc_framework_test_project.test;


import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

public class ReadFileTest extends	ActivityInstrumentationTestCase2<TestActivity> {
	

	/* Folder data to read. This folder must exist on the account */
	private final String mRemoteFolderPath = "/folderToRead";
	
	/* File data to rename. This file must exist on the account */
	private final String mRemoteFilePath = "/fileToRead.txt";
	
	private TestActivity mActivity;
	
	public ReadFileTest() {
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
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Read File
	 */
	public void testReadFile() {

		RemoteOperationResult result = mActivity.readFile(mRemoteFilePath);
		assertTrue(result.isSuccess());
	}
}
