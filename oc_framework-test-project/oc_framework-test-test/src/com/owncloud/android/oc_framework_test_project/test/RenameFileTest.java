/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.oc_framework_test_project.test;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.oc_framework_test_project.TestActivity;

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
