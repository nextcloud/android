package com.owncloud.android.oc_framework_test_project;

import com.owncloud.android.oc_framework.network.webdav.OwnCloudClientFactory;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.remote.CreateRemoteFolderOperation;
import com.owncloud.android.oc_framework.operations.remote.ReadRemoteFileOperation;
import com.owncloud.android.oc_framework.operations.remote.RenameRemoteFileOperation;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

/**
 * Activity to test OC framework
 * @author masensio
 * @author David A. Velasco
 */
public class TestActivity extends Activity {
	
	// This account must exists on the simulator / device
	private static final String mServerUri = "https://beta.owncloud.com/owncloud/remote.php/webdav";
	private static final String mUser = "testandroid";
	private static final String mPass = "testandroid";
	
	//private Account mAccount = null;
	private WebdavClient mClient;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_test);
    	Uri uri = Uri.parse(mServerUri);
    	mClient = OwnCloudClientFactory.createOwnCloudClient(uri ,getApplicationContext(), true);
    	mClient.setBasicCredentials(mUser, mPass);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.test, menu);
		return true;
	}

	/**
	 * Access to the library method to Create a Folder
	 * @param remotePath
	 * @param createFullPath
	 * 
	 * @return
	 */
	public RemoteOperationResult createFolder(String remotePath, boolean createFullPath) {
		
		CreateRemoteFolderOperation createOperation = new CreateRemoteFolderOperation(remotePath, createFullPath);
		RemoteOperationResult result =  createOperation.execute(mClient);
		
		return result;
	}
	
	/**
	 * Access to the library method to Rename a File or Folder
	 * @param oldName			Old name of the file.
     * @param oldRemotePath		Old remote path of the file. For folders it starts and ends by "/"
     * @param newName			New name to set as the name of file.
     * @param isFolder			'true' for folder and 'false' for files
     * 
     * @return
     */

	public RemoteOperationResult renameFile(String oldName, String oldRemotePath, String newName, boolean isFolder) {
		
		RenameRemoteFileOperation renameOperation = new RenameRemoteFileOperation(oldName, oldRemotePath, newName, isFolder);
		RemoteOperationResult result = renameOperation.execute(mClient);
		
		return result;
	}
	
	/**
	 * Access to the library method to Read a File or Folder (PROPFIND DEPTH 1)
	 * @param remotePath
	 * 
	 * @return
	 */
	public RemoteOperationResult readFile(String remotePath) {
		
		ReadRemoteFileOperation readOperation= new ReadRemoteFileOperation(remotePath);
		RemoteOperationResult result = readOperation.execute(mClient);
		
		return result;
	}
	
}
