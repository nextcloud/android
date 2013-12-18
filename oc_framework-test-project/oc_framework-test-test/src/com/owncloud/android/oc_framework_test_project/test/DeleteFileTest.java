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
 * Class to test Delete a File Operation
 * @author masensio
 *
 */

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
