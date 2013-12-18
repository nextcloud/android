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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.res.AssetManager;
import android.os.Environment;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework_test_project.TestActivity;

/**
 * Class to test Update File Operation
 * @author masensio
 *
 */

public class UploadFileTest extends ActivityInstrumentationTestCase2<TestActivity> {

	/* Files to upload. These files must exists on the device */	
	private final String mFileToUpload = "fileToUpload.png";
	private final String mMimeType = "image/png";
	
	private final String mFileToUploadWithChunks = "fileToUploadChunks.MP4";
	private final String mMimeTypeWithChunks = "video/mp4";
	
	private final String mFileNotFound = "fileNotFound.png";
	
	private final String mStoragePath = "/owncloud/tmp/uploadTest";
	private String mPath;
	
	private String mCurrentDate;
	
	private TestActivity mActivity;
	
	public UploadFileTest() {
	    super(TestActivity.class);
	   
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	    
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
	    mCurrentDate = sdf.format(new Date());
	    
	    File sdCard = Environment.getExternalStorageDirectory();
        mPath =  sdCard.getAbsolutePath() + "/" + mStoragePath + mCurrentDate;
        
		//mActivity.createFolder(mPath, true);
        
	    copyAssets();
	}

	/**
	 * Copy Files to ulpload to SdCard
	 */
	private void copyAssets() {
		AssetManager assetManager = getActivity().getAssets();
		String[] files = { mFileToUpload, mFileToUploadWithChunks }; 
	    
	    // Folder with contents
        File folder = new File(mPath);
        folder.mkdirs();
        
        
	    for(String filename : files) {
	        InputStream in = null;
	        OutputStream out = null;
	        try {
	          in = assetManager.open(filename);
	          File outFile = new File(folder, filename);
	          out = new FileOutputStream(outFile);
	          copyFile(in, out);
	          in.close();
	          in = null;
	          out.flush();
	          out.close();
	          out = null;
	        } catch(IOException e) {
	            Log.e("tag", "Failed to copy asset file: " + filename, e);
	        }       
	    }
	}
	
	private void copyFile(InputStream in, OutputStream out) throws IOException {
	    byte[] buffer = new byte[1024];
	    int read;
	    while((read = in.read(buffer)) != -1){
	      out.write(buffer, 0, read);
	    }
	}
	
	
	/**
	 * Test Upload File without chunks
	 */
	public void testUploadFile() {

		String storagePath = mPath + "/" + mFileToUpload;
		//String remotePath = "/uploadTest" + mCurrentDate + "/" + mFileToUpload;
		String remotePath = "/" + mFileToUpload;
		
		RemoteOperationResult result = mActivity.uploadFile(storagePath, remotePath, mMimeType);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Upload File with chunks
	 */
	public void testUploadFileWithChunks() {

		String storagePath = mPath + "/" + mFileToUploadWithChunks;
		//String remotePath = "/uploadTest" + mCurrentDate + "/" +mFileToUploadWithChunks;
		String remotePath = "/" + mFileToUploadWithChunks;
		
		RemoteOperationResult result = mActivity.uploadFile(storagePath, remotePath, mMimeTypeWithChunks);
		assertTrue(result.isSuccess());
	}
	
	/**
	 * Test Upload Not Found File
	 */
	public void testUploadFileNotFound() {

		String storagePath = mPath + "/" + mFileNotFound;
		//String remotePath = "/uploadTest" + mCurrentDate + "/" + mFileToUpload;
		String remotePath = "/" + mFileNotFound;
		
		RemoteOperationResult result = mActivity.uploadFile(storagePath, remotePath, mMimeType);
		assertFalse(result.isSuccess());
	}
	
}
