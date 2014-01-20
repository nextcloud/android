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

package com.owncloud.android.lib.test_project;

import java.io.File;

import com.owncloud.android.lib.network.OwnCloudClientFactory;
import com.owncloud.android.lib.network.OwnCloudClient;
import com.owncloud.android.lib.operations.common.RemoteFile;
import com.owncloud.android.lib.operations.common.RemoteOperationResult;
import com.owncloud.android.lib.operations.remote.ChunkedUploadRemoteFileOperation;
import com.owncloud.android.lib.operations.remote.CreateRemoteFolderOperation;
import com.owncloud.android.lib.operations.remote.DownloadRemoteFileOperation;
import com.owncloud.android.lib.operations.remote.ReadRemoteFolderOperation;
import com.owncloud.android.lib.operations.remote.RemoveRemoteFileOperation;
import com.owncloud.android.lib.operations.remote.RenameRemoteFileOperation;
import com.owncloud.android.lib.operations.remote.UploadRemoteFileOperation;
import com.owncloud.android.lib.test_project.R;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
	private static final boolean mChunked = true;
	
	//private Account mAccount = null;
	private OwnCloudClient mClient;
	
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
	 * @param remotePath            Full path to the new directory to create in the remote server.
     * @param createFullPath        'True' means that all the ancestor folders should be created if don't exist yet.
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
	 * Access to the library method to Remove a File or Folder
	 * 
	 * @param remotePath	Remote path of the file or folder in the server.
	 * @return
	 */
	public RemoteOperationResult removeFile(String remotePath) {
		
		RemoveRemoteFileOperation removeOperation = new RemoveRemoteFileOperation(remotePath);
		RemoteOperationResult result = removeOperation.execute(mClient);
		
		return result;
	}
	
	/**
	 * Access to the library method to Read a Folder (PROPFIND DEPTH 1)
	 * @param remotePath
	 * 
	 * @return
	 */
	public RemoteOperationResult readFile(String remotePath) {
		
		ReadRemoteFolderOperation readOperation= new ReadRemoteFolderOperation(remotePath);
		RemoteOperationResult result = readOperation.execute(mClient);

		return result;
	}
	
	/**
	 * Access to the library method to Download a File
	 * @param remotePath
	 * 
	 * @return
	 */
	public RemoteOperationResult downloadFile(RemoteFile remoteFile, String temporalFolder) {
		// Create folder 
		String path =  "/owncloud/tmp/" + temporalFolder;
		File sdCard = Environment.getExternalStorageDirectory();
		File folder = new File(sdCard.getAbsolutePath() + "/" + path);
		folder.mkdirs();
		
		DownloadRemoteFileOperation downloadOperation = new DownloadRemoteFileOperation(remoteFile.getRemotePath(), folder.getAbsolutePath());
		RemoteOperationResult result = downloadOperation.execute(mClient);

		return result;
	}
	
	/** Access to the library method to Upload a File 
	 * @param storagePath
	 * @param remotePath
	 * @param mimeType
	 * 
	 * @return
	 */
	public RemoteOperationResult uploadFile(String storagePath, String remotePath, String mimeType) {

		UploadRemoteFileOperation uploadOperation;
		if ( mChunked && (new File(storagePath)).length() > ChunkedUploadRemoteFileOperation.CHUNK_SIZE ) {
            uploadOperation = new ChunkedUploadRemoteFileOperation(storagePath, remotePath, mimeType);
        } else {
            uploadOperation = new UploadRemoteFileOperation(storagePath, remotePath, mimeType);
        }
		
		RemoteOperationResult result = uploadOperation.execute(mClient);
		
		return result;
	}
}
