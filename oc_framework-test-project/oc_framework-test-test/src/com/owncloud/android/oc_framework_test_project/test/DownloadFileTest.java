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

import com.owncloud.android.oc_framework.operations.RemoteFile;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

/**
 * Class to test Download File Operation
 * @author masensio
 *
 */

public class DownloadFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

	private final String TAG = DownloadFileTest.class.getSimpleName();
	
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
