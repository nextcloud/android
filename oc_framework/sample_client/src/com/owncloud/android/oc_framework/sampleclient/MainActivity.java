package com.owncloud.android.oc_framework.sampleclient;

import java.util.List;

import com.owncloud.android.oc_framework.accounts.AccountUtils;
import com.owncloud.android.oc_framework.network.webdav.OwnCloudClientFactory;
import com.owncloud.android.oc_framework.network.webdav.WebdavClient;
import com.owncloud.android.oc_framework.operations.OnRemoteOperationListener;
import com.owncloud.android.oc_framework.operations.RemoteFile;
import com.owncloud.android.oc_framework.operations.RemoteOperation;
import com.owncloud.android.oc_framework.operations.RemoteOperationResult;
import com.owncloud.android.oc_framework.operations.remote.ReadRemoteFolderOperation;
import com.owncloud.android.oc_framework.utils.FileUtils;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnRemoteOperationListener {
	
	private Handler mHandler;
	
	private WebdavClient mClient; 
	
	private FilesArrayAdapter mFilesAdapter;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mHandler = new Handler();
        
    	Uri serverUri = Uri.parse(getString(R.string.server_base_url) + AccountUtils.WEBDAV_PATH_4_0);
    	mClient = OwnCloudClientFactory.createOwnCloudClient(serverUri, this, true);
    	mClient.setBasicCredentials(getString(R.string.username), getString(R.string.password));
    	
    	mFilesAdapter = new FilesArrayAdapter(this, R.layout.file_in_list);
    	((ListView)findViewById(R.id.list_view)).setAdapter(mFilesAdapter);
    }
    
    public void onClickHandler(View button) {
    	switch (button.getId())	{
	    	case R.id.button_refresh:
	    		startRefresh();
	    		break;
	    	case R.id.button_upload:
	    		startUpload();
	    		break;
	    	case R.id.button_delete_remote:
	    		startRemoteDeletion();
	    		break;
	    	case R.id.button_download:
	    		startDownload();
	    		break;
	    	case R.id.button_delete_local:
	    		startLocalDeletion();
	    		break;
			default:
	    		Toast.makeText(this, R.string.youre_doing_it_wrong, Toast.LENGTH_SHORT).show();
    	}
    }
    
    private void startRefresh() {
    	RemoteOperation refreshOperation = new ReadRemoteFolderOperation(FileUtils.PATH_SEPARATOR);
    	refreshOperation.execute(mClient, this, mHandler);
    }
    
    private void startUpload() {
    	Toast.makeText(this, R.string.todo_start_upload, Toast.LENGTH_SHORT).show();
    }
    
    private void startRemoteDeletion() {
    	Toast.makeText(this, R.string.todo_start_remote_deletion, Toast.LENGTH_SHORT).show();
    }
    
    private void startDownload() {
    	Toast.makeText(this, R.string.todo_start_download, Toast.LENGTH_SHORT).show();
    }
    
    private void startLocalDeletion() {
    	Toast.makeText(this, R.string.todo_start_local_deletion, Toast.LENGTH_SHORT).show();
    }

	@Override
	public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
		if (!result.isSuccess()) {
			Toast.makeText(this, R.string.todo_operation_finished_in_fail, Toast.LENGTH_SHORT).show();
			
		} else if (operation instanceof ReadRemoteFolderOperation) {
			onSuccessfulRefresh((ReadRemoteFolderOperation)operation, result);
			
		} else {
			Toast.makeText(this, R.string.todo_operation_finished_in_success, Toast.LENGTH_SHORT).show();
		}
	}

	private void onSuccessfulRefresh(ReadRemoteFolderOperation operation, RemoteOperationResult result) {
		mFilesAdapter.clear();
		List<RemoteFile> files = result.getData();
		if (files != null && files.size() > 0) {
			mFilesAdapter.addAll(files);
			mFilesAdapter.remove(mFilesAdapter.getItem(0));
		}
		mFilesAdapter.notifyDataSetChanged();
	}
	
}
