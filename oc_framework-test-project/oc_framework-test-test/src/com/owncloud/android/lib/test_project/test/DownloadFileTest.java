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

import com.owncloud.android.lib.operations.common.RemoteFile;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;

/**
 * Class to test Download File Operation
 * @author masensio
 *
 */

public class DownloadFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

	
	/* Files to download. These files must exist on the account */
	private final String mRemoteFilePng = "/fileToDownload.png";
	private final String mRemoteFileChunks = "/fileToDownload.mp4";
	private final String mRemoteFileSpecialChars = "/@file@download.png";
	private final String mRemoteFileSpecialCharsChunks = "/@file@download.mp4";
	private final String mRemoteFileNotFound = "/fileNotFound.png"; /* This file mustn't exist on the account */
	
	private String mCurrentDate;
	
	
	private TestActivity mActivity;
	
	public DownloadFileTest() {
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
	 * Test Download a File
	 */
	public void testDownloadFile() {
		String temporalFolder = "/download" + mCurrentDate;
		
		RemoteFile remoteFile= new RemoteFile(mRemoteFilePng);

		RemoteOperationResult result = mActivity.downloadFile(remoteFile, temporalFolder);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Download a File with chunks
	 */
	public void testDownloadFileChunks() {
		String temporalFolder = "/download" + mCurrentDate;
		
		RemoteFile remoteFile= new RemoteFile(mRemoteFileChunks);

		RemoteOperationResult result = mActivity.downloadFile(remoteFile, temporalFolder);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Download a File with special chars
	 */
	public void testDownloadFileSpecialChars() {
		String temporalFolder = "/download" + mCurrentDate;
		
		RemoteFile remoteFile= new RemoteFile(mRemoteFileSpecialChars);

		RemoteOperationResult result = mActivity.downloadFile(remoteFile, temporalFolder);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Download a File with special chars and chunks
	 */
	public void testDownloadFileSpecialCharsChunks() {
		String temporalFolder = "/download" + mCurrentDate;
		
		RemoteFile remoteFile= new RemoteFile(mRemoteFileSpecialCharsChunks);

		RemoteOperationResult result = mActivity.downloadFile(remoteFile, temporalFolder);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Download a Not Found File 
	 */
	public void testDownloadFileNotFound() {
		String temporalFolder = "/download" + mCurrentDate;

		RemoteFile remoteFile = new RemoteFile(mRemoteFileNotFound);

		RemoteOperationResult result = mActivity.downloadFile(remoteFile, temporalFolder);
		assertFalse(result.isSuccess());
	}
}
