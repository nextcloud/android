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

import java.text.SimpleDateFormat;
import java.util.Date;

import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.common.RemoteOperationResult.ResultCode;
import com.owncloud.android.lib.test_project.TestActivity;

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
