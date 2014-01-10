package com.owncloud.android.oc_framework.sampleclient;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
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
    	Toast.makeText(this, R.string.todo_start_refresh, Toast.LENGTH_SHORT).show();
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
    
}
