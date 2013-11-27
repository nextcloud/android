package com.owncloud.android.oc_framework_test_project.test;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

public class DeleteFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

	/* Folder data to delete. */
	private final String mFolderPath = "/folderToDelete";
	
	/* File to delete. */
	private final String mFilePath = "fileToDelete.png";

	private TestActivity mActivity;
	
	public DeleteFileTest() {
	    super(TestActivity.class);
	   
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	}
	
	/**
	 * Test Remove Folder
	 */
	public void testRemoveFolder() {

		RemoteOperationResult result = mActivity.removeFile(mFolderPath);
		assertTrue(result.isSuccess() || result.getCode() == ResultCode.FILE_NOT_FOUND);
	}
	
	/**
	 * Test Remove File
	 */
	public void testRemoveFile() {
		
		RemoteOperationResult result = mActivity.removeFile(mFilePath);
		assertTrue(result.isSuccess() || result.getCode() == ResultCode.FILE_NOT_FOUND);
	}

	/**
	 * Restore initial conditions
	 */
	public void testRestoreInitialConditions() {
		RemoteOperationResult result = mActivity.createFolder(mFolderPath, true);
		assertTrue(result.isSuccess());
		
	}
}
