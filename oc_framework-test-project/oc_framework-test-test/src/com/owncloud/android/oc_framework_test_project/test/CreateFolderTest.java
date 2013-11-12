package com.owncloud.android.oc_framework_test_project.test;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

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
	
	public void testCreateFolder() {

		String remotePath = "/testCreateFolder" + mCurrentDate;
		boolean createFullPath = true;
		
		RemoteOperationResult result =  mActivity.createFolder(remotePath, createFullPath);
		Log.d("test CreateFolder", "-----------------------" + result.getCode().name());
		Log.d("test CreateFolder", "-----------------------" + result.getLogMessage());
		assertTrue(result.isSuccess());
	}
	
	public void testCreateFolderSpecialCharacters() {
		String remotePath = "/test^^SpecialCharacters" + mCurrentDate;
		boolean createFullPath = true;
		
		RemoteOperationResult result =  mActivity.createFolder(remotePath, createFullPath);
		assertFalse(result.isSuccess());
	}


}
