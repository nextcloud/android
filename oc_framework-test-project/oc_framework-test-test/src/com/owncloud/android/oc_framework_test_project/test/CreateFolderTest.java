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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Class to test Create Folder Operation
 * @author masensio
 *
 */
public class CreateFolderTest extends ActivityInstrumentationTestCase2<TestActivity> {

	private TestActivity mActivity;
	private String mCurrentDate;
	
	public CreateFolderTest() {
	    super(TestActivity.class);
	   
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
		mCurrentDate = sdf.format(new Date());
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	}
	
	/**
	 * Test Create Folder
	 */
	public void testCreateFolder() {

		String remotePath = "/testCreateFolder" + mCurrentDate;
		boolean createFullPath = true;
		
		RemoteOperationResult result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.isSuccess() || result.getCode() == ResultCode.TIMEOUT);
		
		// Create Subfolder
		remotePath = "/testCreateFolder" + mCurrentDate + "/" + "testCreateFolder" + mCurrentDate;
		createFullPath = true;
		
		result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.isSuccess() || result.getCode() == ResultCode.TIMEOUT);
	}
	
	
	/**
	 * Test to Create Folder with special characters: /  \  < >  :  "  |  ?  *
	 */
	public void testCreateFolderSpecialCharacters() {		
		boolean createFullPath = true;
		
		String remotePath = "/testSpecialCharacters_\\" + mCurrentDate;
		RemoteOperationResult result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		remotePath = "/testSpecialCharacters_<" + mCurrentDate;		
		result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		remotePath = "/testSpecialCharacters_>" + mCurrentDate;		
		result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		remotePath = "/testSpecialCharacters_:" + mCurrentDate;		
		result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		remotePath = "/testSpecialCharacters_\"" + mCurrentDate;		
		result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		remotePath = "/testSpecialCharacters_|" + mCurrentDate;		
		result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		remotePath = "/testSpecialCharacters_?" + mCurrentDate;		
		result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
		
		remotePath = "/testSpecialCharacters_*" + mCurrentDate;		
		result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.getCode() == ResultCode.INVALID_CHARACTER_IN_NAME);
	}


}
