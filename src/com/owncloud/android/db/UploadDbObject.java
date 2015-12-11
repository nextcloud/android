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

package com.owncloud.android.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.accounts.Account;
import android.content.Context;
import android.util.Base64;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.UploadDbHandler.UploadStatus;
import com.owncloud.android.files.services.FileUploadService.LocalBehaviour;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Stores all information in order to start upload operations. PersistentUploadObject can
 * be stored persistently by {@link UploadDbHandler}.
 * 
 */
public class UploadDbObject implements Serializable {

    /** Generated - should be refreshed every time the class changes!! */
    private static final long serialVersionUID = -2306246191385279928L;

    private static final String TAG = UploadDbObject.class.getSimpleName();
    
    public UploadDbObject(OCFile ocFile) {
        this.mFile = ocFile;
    }


    OCFile mFile;
    
    public OCFile getOCFile() {
        return mFile;
    }
    
    
    /**
     * Local action for upload.
     */
    LocalBehaviour mLocalAction;

    /**
     * Date and time when this upload was first requested.
     */
    Calendar mUploadTime = new GregorianCalendar();

    public Calendar getUploadTime() {
        return mUploadTime;
    }

    /**
     * @return the uploadStatus
     */
    public UploadStatus getUploadStatus() {
        return mUploadStatus;
    }

    /**
     * Sets uploadStatus AND SETS lastResult = null;
     * @param uploadStatus the uploadStatus to set
     */
    public void setUploadStatus(UploadStatus uploadStatus) {
        this.mUploadStatus = uploadStatus;
        setLastResult(null);
    }

    /**
     * @return the lastResult
     */
    public RemoteOperationResult getLastResult() {
        return mLastResult;
    }

    /**
     * @param lastResult the lastResult to set
     */
    public void setLastResult(RemoteOperationResult lastResult) {
        this.mLastResult = lastResult;
    }

    /**
     * Overwrite destination file?
     */
    boolean mForceOverwrite;
    /**
     * Create destination folder?
     */
    boolean mIsCreateRemoteFolder;
    /**
     * Upload only via wifi?
     */
    boolean mIsUseWifiOnly;
    /**
     * Upload only if phone being charged?
     */
    boolean mIsWhileChargingOnly;
    /**
     * Earliest time when upload may be started. Negative if not set.
     */
    long mUploadTimestamp;

    /**
     * Name of Owncloud account to upload file to.
     */
    String mAccountName;

    /**
     * Status of upload (later, in_progress, ...).
     */
    UploadStatus mUploadStatus;

    /**
     * Result from last upload operation. Can be null.
     */
    RemoteOperationResult mLastResult;

    /**
     * @return the localPath
     */
    public String getLocalPath() {
        return mFile.getStoragePath();
    }

    /**
     * @return the remotePath
     */
    public String getRemotePath() {
        return mFile.getRemotePath();
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mFile.getMimetype();
    }


    /**
     * @return the localAction
     */
    public LocalBehaviour getLocalAction() {
        // return null;
        return mLocalAction;
    }

    /**
     * @param localAction the localAction to set
     */
    public void setLocalAction(LocalBehaviour localAction) {
        this.mLocalAction = localAction;
    }

    /**
     * @return the forceOverwrite
     */
    public boolean isForceOverwrite() {
        return mForceOverwrite;
    }

    /**
     * @param forceOverwrite the forceOverwrite to set
     */
    public void setForceOverwrite(boolean forceOverwrite) {
        this.mForceOverwrite = forceOverwrite;
    }

    /**
     * @return the isCreateRemoteFolder
     */
    public boolean isCreateRemoteFolder() {
        return mIsCreateRemoteFolder;
    }

    /**
     * @param isCreateRemoteFolder the isCreateRemoteFolder to set
     */
    public void setCreateRemoteFolder(boolean isCreateRemoteFolder) {
        this.mIsCreateRemoteFolder = isCreateRemoteFolder;
    }

    /**
     * @return the isUseWifiOnly
     */
    public boolean isUseWifiOnly() {
        return mIsUseWifiOnly;
    }

    /**
     * @param isUseWifiOnly the isUseWifiOnly to set
     */
    public void setUseWifiOnly(boolean isUseWifiOnly) {
        this.mIsUseWifiOnly = isUseWifiOnly;
    }

    /**
     * @return the accountName
     */
    public String getAccountName() {
        return mAccountName;
    }

    /**
     * @param accountName the accountName to set
     */
    public void setAccountName(String accountName) {
        this.mAccountName = accountName;
    }

    /**
     * Returns a base64 encoded serialized string of this object.
     */
    @Override
    public String toString() {
        // serialize the object
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(this);
            so.flush();
            String serializedObjectBase64 = Base64.encodeToString(bo.toByteArray(), Base64.DEFAULT);
            so.close();
            bo.close();
            return serializedObjectBase64;
        } catch (Exception e) {
            Log_OC.e(TAG, "Cannot serialize UploadDbObject with localPath:" + getLocalPath(), e);
        }
        return null;
    }

    /**
     * Accepts a base64 encoded serialized string of an {@link UploadDbObject}
     * and instantiates and returns an according object.
     * 
     * @param serializedObjectBase64
     * @return
     */
    static public UploadDbObject fromString(String serializedObjectBase64) {
        // deserialize the object
        try {
            byte[] b = Base64.decode(serializedObjectBase64, Base64.DEFAULT);
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            UploadDbObject obj = (UploadDbObject) si.readObject();
            return obj;
        } catch (Exception e) {
            Log_OC.e(TAG, "Cannot deserialize UploadDbObject " + serializedObjectBase64, e);
        }
        return null;
    }

    /**
     * Returns owncloud account as {@link Account} object.  
     */
    public Account getAccount(Context context) {
        return AccountUtils.getOwnCloudAccountByName(context, getAccountName());
    }

    public void setWhileChargingOnly(boolean isWhileChargingOnly) {
        this.mIsWhileChargingOnly = isWhileChargingOnly;
    }
    
    public boolean isWhileChargingOnly() {
        return mIsWhileChargingOnly;
    }

    /**
     * Earliest time when upload may be started. Negative if not set.
     * @return the uploadTimestamp
     */
    public long getUploadTimestamp() {
        return mUploadTimestamp;
    }

    /**
     * Earliest time when upload may be started. Set to negative value for immediate upload.
     * @param uploadTimestamp the uploadTimestamp to set
     */
    public void setUploadTimestamp(long uploadTimestamp) {
        this.mUploadTimestamp = uploadTimestamp;
    }
    
    /**
     * For debugging purposes only.
     */
    public String toFormattedString() {
        return getLocalPath() + " status:" + getUploadStatus() + " result:" +
                (getLastResult() == null?"null" : getLastResult().getCode());
    }

    /**
     * Removes all uploads restrictions. After calling this function upload is performed immediately if requested.
     */
    public void removeAllUploadRestrictions() {
        setUseWifiOnly(false);
        setWhileChargingOnly(false);
        setUploadTimestamp(0);
    }

    /**
     * Returns true when user is able to cancel this upload. That is, when
     * upload is currently in progress or scheduled for upload.
     */
    public  boolean userCanCancelUpload() {
        switch (this.getUploadStatus()) {
            case UPLOAD_IN_PROGRESS:
            case UPLOAD_LATER:
            case UPLOAD_FAILED_RETRY:
                return true;
            default:
                return false;
        }
    }

    /**
     * Returns true when user can choose to retry this upload. That is, when
     * user cancelled upload before or when upload has failed.
     */
    public boolean userCanRetryUpload() {
        switch (this.getUploadStatus()) {
            case UPLOAD_CANCELLED:
            case UPLOAD_FAILED_RETRY://automatically retried. no need for user option.
            case UPLOAD_FAILED_GIVE_UP: //TODO this case needs to be handled as described by
                // https://github.com/owncloud/android/issues/765#issuecomment-66490312
            case UPLOAD_LATER: //upload is already schedule but allow user to increase priority
            case UPLOAD_SUCCEEDED: // if user wants let him to re-upload (maybe
                // remote file was deleted...)
                return true;
            default:
                return false;
        }
    }
}
