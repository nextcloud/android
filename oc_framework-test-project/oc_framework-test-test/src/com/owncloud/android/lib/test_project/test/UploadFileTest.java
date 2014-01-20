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

import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.test_project.TestActivity;

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
