/**
 *   ownCloud Android client application
 *
 *   @author LukeOwncloud
 *   Copyright (C) 2015 ownCloud Inc.
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
package com.owncloud.android.ui.activity;

import java.io.File;

import android.accounts.Account;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;


import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.UploadDbHandler;
import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.files.services.FileUploadService;
import com.owncloud.android.files.services.FileUploadService.FileUploaderBinder;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.errorhandling.ExceptionHandler;
import com.owncloud.android.ui.fragment.UploadListFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;

/**
 * Activity listing pending, active, and completed uploads. User can delete
 * completed uploads from view. Content of this list of coming from
 * {@link UploadDbHandler}.
 *
 */
public class UploadListActivity extends FileActivity implements UploadListFragment.ContainerActivity {

    private static final String TAG = UploadListActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler(this));
        setContentView(R.layout.upload_list_layout);
    }

    // ////////////////////////////////////////
    // UploadListFragment.ContainerActivity
    // ////////////////////////////////////////
    @Override
    public boolean onUploadItemClick(UploadDbObject file) {
        File f = new File(file.getLocalPath());
        if(!f.exists()) {
            Toast.makeText(this, "Cannot open. Local file does not exist.",
                    Toast.LENGTH_SHORT).show();
        } else {
            openFileWithDefault(file.getLocalPath());
        }
        return true;
    }

    /**
     * Open file with app associates with its mimetype. If mimetype unknown, show list with all apps.  
     */
    private void openFileWithDefault(String localPath) {
        Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW);
        File file = new File(localPath);
        String extension = android.webkit.MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(file).toString());
        String mimetype = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimetype == null)
            mimetype = "*/*";
        myIntent.setDataAndType(Uri.fromFile(file), mimetype);
        try {
            startActivity(myIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Found no app to open this file.", Toast.LENGTH_LONG).show();
            Log_OC.i(TAG, "Could not find app for sending log history.");

        }        
    }
    
    /**
     * Same as openFileWithDefault() but user cannot save default app.
     * @param ocFile
     */
    @SuppressWarnings("unused")
    private void openFileWithDefaultNoDefault(OCFile ocFile) {
        getFileOperationsHelper().openFile(ocFile);
    }

    /**
     * WARNING! This opens the local copy inside owncloud directory. If file not uploaded yet,
     * there is none.
     */
    @SuppressWarnings("unused")
    private void openPreview(UploadDbObject file) {
     // preview image
        Intent showDetailsIntent = new Intent(this, PreviewImageActivity.class);
        showDetailsIntent.putExtra(EXTRA_FILE, (Parcelable)file.getOCFile());
        showDetailsIntent.putExtra(EXTRA_ACCOUNT, getAccount());
        startActivity(showDetailsIntent);  
    }
    
    @SuppressWarnings("unused")
    private void openDetails(UploadDbObject file) {
        OCFile ocFile = file.getOCFile();
        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, (Parcelable) ocFile);
        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, file.getAccount(this));
        startActivity(showDetailsIntent);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case R.id.action_retry_uploads: {
                Log_OC.d(TAG, "FileUploadService.retry() called by onMenuItemSelected()");
                FileUploadService.retry(this);
                break;
            }
            case R.id.action_clear_failed_uploads: {
                UploadDbHandler db = UploadDbHandler.getInstance(this);
                db.clearFailedUploads();
                break;
            }
            case R.id.action_clear_finished_uploads: {
                UploadDbHandler db = UploadDbHandler.getInstance(this);
                db.clearFinishedUploads();
                break;
            }
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.upload_list_menu, menu);
        return true;
    }
    
    @Override
    protected ServiceConnection newTransferenceServiceConnection() {
        return new UploadListServiceConnection();
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private class UploadListServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName component, IBinder service) {
            if (service instanceof FileUploaderBinder) {
                if(mUploaderBinder == null)
                {
                    mUploaderBinder = (FileUploaderBinder) service;
                    Log_OC.d(TAG, "UploadListActivity connected to Upload service. component: " +
                            component + " service: "
                            + service);
                } else {
                    Log_OC.d(TAG, "mUploaderBinder already set. mUploaderBinder: " +
                            mUploaderBinder + " service:" + service);
                }
            } else {
                Log_OC.d(TAG, "UploadListActivity not connected to Upload service. component: " +
                        component + " service: " + service);
                return;
            }            
        }

        @Override
        public void onServiceDisconnected(ComponentName component) {
            if (component.equals(new ComponentName(UploadListActivity.this, FileUploadService.class))) {
                Log_OC.d(TAG, "UploadListActivity suddenly disconnected from Upload service");
                mUploaderBinder = null;
            }
        }
    };  

}
