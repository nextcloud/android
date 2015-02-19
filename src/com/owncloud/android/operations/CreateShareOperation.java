/* ownCloud Android client application
 *   Copyright (C) 2014 ownCloud Inc.
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

package com.owncloud.android.operations;

/**
 * Creates a new share from a given file
 * 
 * @author masensio
 *
 */

import android.content.Context;
import android.content.Intent;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.operations.common.SyncOperation;

public class CreateShareOperation extends SyncOperation {

    private static final String TAG = CreateShareOperation.class.getSimpleName();
    

    protected FileDataStorageManager mStorageManager;

    private Context mContext;
    private String mPath;
    private ShareType mShareType;
    private String mShareWith;
    private boolean mPublicUpload;
    private String mPassword;
    private int mPermissions;
    private Intent mSendIntent;

    /**
     * Constructor
     * @param context       The context that the share is coming from.
     * @param path          Full path of the file/folder being shared. Mandatory argument
     * @param shareType     0 = user, 1 = group, 3 = Public link. Mandatory argument
     * @param shareWith     User/group ID with who the file should be shared.  This is mandatory for shareType of 0 or 1
     * @param publicUpload  If false (default) public cannot upload to a public shared folder. 
     *                      If true public can upload to a shared folder. Only available for public link shares
     * @param password      Password to protect a public link share. Only available for public link shares
     * @param permissions   1 - Read only - Default for public shares
     *                      2 - Update
     *                      4 - Create
     *                      8 - Delete
     *                      16- Re-share
     *                      31- All above - Default for private shares
     *                      For user or group shares.
     *                      To obtain combinations, add the desired values together.  
     *                      For instance, for Re-Share, delete, read, update, add 16+8+2+1 = 27.
     */
    public CreateShareOperation(Context context, String path, ShareType shareType, String shareWith, boolean publicUpload,
            String password, int permissions, Intent sendIntent) {

        mContext = context;
        mPath = path;
        mShareType = shareType;
        mShareWith = shareWith;
        mPublicUpload = publicUpload;
        mPassword = password;
        mPermissions = permissions;
        mSendIntent = sendIntent;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        RemoteOperation operation = null;
        
        // Check if the share link already exists
        operation = new GetRemoteSharesForFileOperation(mPath, false, false);
        RemoteOperationResult result = ((GetRemoteSharesForFileOperation)operation).execute(client);

        if (!result.isSuccess() || result.getData().size() <= 0) {
            operation = new CreateRemoteShareOperation(mPath, mShareType, mShareWith, mPublicUpload, mPassword, mPermissions);
            result = ((CreateRemoteShareOperation)operation).execute(client);
        }
        
        if (result.isSuccess()) {
            if (result.getData().size() > 0) {
                OCShare share = (OCShare) result.getData().get(0);
                updateData(share);
            } 
        }
        
        return result;
    }
    
    
    public Intent getSendIntent() {
        return mSendIntent;
    }
    
    private void updateData(OCShare share) {
        // Update DB with the response
        share.setPath(mPath);
        if (mPath.endsWith(FileUtils.PATH_SEPARATOR)) {
            share.setIsFolder(true);
        } else {
            share.setIsFolder(false);
        }
        share.setPermissions(mPermissions);
        
        getStorageManager().saveShare(share);
        
        // Update OCFile with data from share: ShareByLink  and publicLink
        OCFile file = getStorageManager().getFileByPath(mPath);
        if (file!=null) {
            mSendIntent.putExtra(Intent.EXTRA_TEXT, share.getShareLink());
            mSendIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(mContext.getString(R.string.subject_token),
                    getClient().getCredentials().getUsername(), file.getFileName()));
            file.setPublicLink(share.getShareLink());
            file.setShareByLink(true);
            getStorageManager().saveFile(file);
            Log_OC.d(TAG, "Public Link = " + file.getPublicLink());

        }
    }

}
