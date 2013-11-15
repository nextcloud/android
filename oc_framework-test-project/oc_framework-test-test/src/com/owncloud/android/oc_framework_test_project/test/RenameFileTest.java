package com.owncloud.android.oc_framework_test_project.test;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

public class RenameFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

	/* Folder data to rename. This folder must exist on the account */
	private final String mOldFolderName = "folderToRename";
	private final String mOldFolderPath = "/folderToRename/";
	private final String mNewFolderName = "renamedFolder"; 
	private final String mNewFolderPath = "/renameFolder/";
	private final String mNewFolderNameInvalidChars = "renamedFolder:";
	private final String mNewFolderPathInvalidChars = "/renamedFolder:/";
	
	/* File data to rename. This file must exist on the account */
	private final String mOldFileName = "fileToRename.png";
	private final String mOldFilePath = "/fileToRename.png";
	private final String mNewFileName = "renamedFile.png";
	private final String mNewFilePath = "/renamedFile.png";
	private final String mNewFileNameInvalidChars = "renamedFile:.png";
	private final String mNewFilePathInvalidChars = "/renamedFile:.png";
	
	
	private TestActivity mActivity;
	
	public RenameFileTest() {
	    super(TestActivity.class);
	   
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	}
	
	/**
	 * Test Rename Folder
	 */
	public void testRenameFolder() {

		RemoteOperationResult result = mActivity.renameFile(mOldFolderName, mOldFolderPath, mNewFolderName, mNewFolderPath);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Rename Folder with forbidden characters
	 */
	public void testRenameFolderForbiddenChars() {

		RemoteOperationResult result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderNameInvalidChars, mNewFolderPathInvalidChars);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
	}
	
	/**
	 * Test Rename File
	 */
	public void testRenameFile() {
		RemoteOperationResult result = mActivity.renameFile(mOldFileName, mOldFilePath, mNewFileName, mNewFilePath);
		assertTrue(result.isSuccess());
	}
	
	
	/**
	 * Test Rename Folder with forbidden characters
	 */
	public void testRenameFileForbiddenChars() {
		RemoteOperationResult result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileNameInvalidChars, mNewFilePathInvalidChars);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
	}
	
	
	/**
	 * Restore initial conditions
	 */
	public void testRestoreInitialConditions() {
		RemoteOperationResult result = mActivity.renameFile(mNewFolderName, mNewFolderPath, mOldFolderName, mOldFolderPath);
		assertTrue(result.isSuccess());
		
		result = mActivity.renameFile(mNewFileName, mNewFilePath, mOldFileName, mOldFilePath);
		assertTrue(result.isSuccess());
	}
	
}
