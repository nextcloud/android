/**
 *   ownCloud Android client application
 *
 *   @author masensio
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

package com.owncloud.android.operations;

/**
 * Creates a new share from a given file
 */


import android.content.Context;
import android.content.Intent;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.lib.resources.shares.CreateRemoteShareOperation;
import com.owncloud.android.lib.resources.shares.GetRemoteSharesForFileOperation;
import com.owncloud.android.lib.resources.shares.OCShare;
import com.owncloud.android.lib.resources.shares.ShareType;
import com.owncloud.android.operations.common.SyncOperation;

public class CreateShareOperation extends SyncOperation {

    private static final String TAG = CreateShareOperation.class.getSimpleName();

    protected FileDataStorageManager mStorageManager;

    private String mPath;
    private ShareType mShareType;
    private String mShareWith;
    private boolean mPublicUpload;
    private String mPassword;
    private int mPermissions;
    private Intent mSendIntent;
    private String mFileName;

    /**
     * Constructor
     * @param path          Full path of the file/folder being shared. Mandatory argument
     * @param shareType     0 = user, 1 = group, 3 = Public link. Mandatory argument
     * @param shareWith     User/group ID with who the file should be shared.
     *                      This is mandatory for shareType of 0 or 1
     * @param publicUpload  If false (default) public cannot upload to a public shared folder. 
     *                      If true public can upload to a shared folder.
     *                      Only available for public link shares
     * @param password      Password to protect a public link share.
     *                      Only available for public link shares
     * @param permissions   1 - Read only - Default for public shares
     *                      2 - Update
     *                      4 - Create
     *                      8 - Delete
     *                      16- Re-share
     *                      31- All above - Default for private shares
     *                      For user or group shares.
     *                      To obtain combinations, add the desired values together.  
     *                      For instance, for Re-Share, delete, read, update, add 16+8+2+1 = 27.
     *  @param sendIntent   Optional Intent with the information of an app where the link to the new share (if public)
     *                      should be posted later.
     */
    public CreateShareOperation(String path, ShareType shareType, String shareWith,
                                boolean publicUpload, String password, int permissions, Intent sendIntent) {

        mPath = path;
        mShareType = shareType;
        mShareWith = shareWith != null ? shareWith : "";
        mPublicUpload = publicUpload;
        mPassword = password;
        mPermissions = permissions;
        mSendIntent = sendIntent;
        mFileName = null;
    }

    @Override
    protected RemoteOperationResult run(OwnCloudClient client) {
        // Check if the share link already exists
        RemoteOperation operation = new GetRemoteSharesForFileOperation(mPath, false, false);
        RemoteOperationResult result = operation.execute(client);

        if (!result.isSuccess() || result.getData().size() <= 0) {
            operation = new CreateRemoteShareOperation(
                    mPath, mShareType, mShareWith,
                    mPublicUpload, mPassword, mPermissions
            );
            result = operation.execute(client);
        }
        
        if (result.isSuccess()) {
            if (result.getData().size() > 0) {
                OCShare share = (OCShare) result.getData().get(0);
                updateData(share);
            } 
        }
        
        return result;
    }
    
    public String getPath() {
        return mPath;
    }

    public ShareType getShareType() {
        return mShareType;
    }

    public String getShareWith() {
        return mShareWith;
    }

    public boolean getPublicUpload() {
        return mPublicUpload;
    }

    public String getPassword() {
        return mPassword;
    }

    public int getPermissions() {
        return mPermissions;
    }

    public Intent getSendIntent() {
        return mSendIntent;
    }

    public Intent getSendIntentWithSubject(Context context) {
        if (context != null && mSendIntent != null && mSendIntent.getStringExtra(Intent.EXTRA_SUBJECT) != null) {
            if (getClient() == null || getClient().getCredentials().getUsername() == null) {
                mSendIntent.putExtra(
                        Intent.EXTRA_SUBJECT,
                        context.getString(R.string.subject_shared_with_you, mFileName)
                );
            } else {
                mSendIntent.putExtra(
                        Intent.EXTRA_SUBJECT,
                        context.getString(
                                R.string.subject_user_shared_with_you,
                                getClient().getCredentials().getUsername(),
                                mFileName
                        )
                );
            }
        }
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
            file.setPublicLink(share.getShareLink());
            file.setShareViaLink(true);
            getStorageManager().saveFile(file);
        }
    }

}
