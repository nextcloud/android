/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.test_project.test;

import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Class to test Rename File Operation
 * @author masensio
 *
 */

public class RenameFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

	/* Folder data to rename. This folder must exist on the account */
	private final String mOldFolderName = "folderToRename";
	private final String mOldFolderPath = "/folderToRename";
	private final String mNewFolderName = "renamedFolder"; 
	private final String mNewFolderPath = "/renamedFolder";
	
	/* File data to rename. This file must exist on the account */
	private final String mOldFileName = "fileToRename.png";
	private final String mOldFilePath = "/fileToRename.png";
	private final String mNewFileName = "renamedFile";
	private final String mFileExtension = ".png";
	private final String mNewFilePath ="/renamedFile.png";
	
	
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

		RemoteOperationResult result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName, true);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Rename Folder with forbidden characters : \  < >  :  "  |  ?  *
	 */
	public void testRenameFolderForbiddenChars() {
		
		RemoteOperationResult result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName + "\\", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName + "<", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName + ">", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName + ":", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName + "\"", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName + "|", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName + "?", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFolderName, mOldFolderPath, 
				mNewFolderName + "*", true);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
	}
	
	/**
	 * Test Rename File
	 */
	public void testRenameFile() {
		RemoteOperationResult result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + mFileExtension, false);
		assertTrue(result.isSuccess());
	}
	
	
	/**
	 * Test Rename Folder with forbidden characters: \  < >  :  "  |  ?  *
	 */
	public void testRenameFileForbiddenChars() {		
		RemoteOperationResult result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + "\\" + mFileExtension, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + "<" + mFileExtension, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + ">" + mFileExtension, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + ":" + mFileExtension, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + "\"" + mFileExtension, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + "|" + mFileExtension, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + "?" + mFileExtension, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		result = mActivity.renameFile(mOldFileName, mOldFilePath, 
				mNewFileName + "*" + mFileExtension, false);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
	}
	
	
	/**
	 * Restore initial conditions
	 */
	public void testRestoreInitialConditions() {
		RemoteOperationResult result = mActivity.renameFile(mNewFolderName, mNewFolderPath, mOldFolderName, true);
		assertTrue(result.isSuccess());
		
		result = mActivity.renameFile(mNewFileName + mFileExtension, mNewFilePath, mOldFileName, false);
		assertTrue(result.isSuccess());
	}
	
}
