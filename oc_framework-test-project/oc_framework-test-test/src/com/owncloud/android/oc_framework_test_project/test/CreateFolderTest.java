package com.owncloud.android.oc_framework_test_project.test;

import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework_test_project.TestActivity;

import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class CreateFolderTest extends ActivityInstrumentationTestCase2<TestActivity> {

	private TestActivity mActivity;
	
	public CreateFolderTest() {
	    super(TestActivity.class);
	   
	}
	
	@Override
	  protected void setUp() throws Exception {
	    super.setUp();
	    setActivityInitialTouchMode(false);
	    mActivity = getActivity();
	}
	
	public void testCreateFolder() {
		
		String remotePath = "beta.owncloud.com/owncloud/testCreateFolder";
		boolean createFullPath = true;
		
		RemoteOperationResult result =  mActivity.createFolder(remotePath, createFullPath);
		Log.d("test CreateFolder", "-----------------------" + result.getCode().name());
		Log.d("test CreateFolder", "-----------------------" + result.getLogMessage());
		assertTrue(result.isSuccess());
	}
	
	public void testCreateFolderSpecialCharacters() {
		String remotePath = "beta.owncloud.com/owncloud/test^^SpecialCharacters";
		boolean createFullPath = true;
		
		RemoteOperationResult result =  mActivity.createFolder(remotePath, createFullPath);
		assertTrue(result.isSuccess());
	}


}
