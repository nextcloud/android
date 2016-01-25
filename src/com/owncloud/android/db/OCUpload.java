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

import android.accounts.Account;
import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus;
import com.owncloud.android.files.services.FileUploadService;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Stores all information in order to start upload operations. PersistentUploadObject can
 * be stored persistently by {@link UploadsStorageManager}.
 * 
 */
public class OCUpload implements Parcelable{

    /** Generated - should be refreshed every time the class changes!! */
//    private static final long serialVersionUID = 2647551318657321611L;

    private static final String TAG = OCUpload.class.getSimpleName();

    private long mId;

    private OCFile mFile;
    /**
     * Local action for upload. (0 - COPY, 1 - MOVE, 2 - FORGET)
     */
    private int mLocalAction;
    /**
     * Date and time when this upload was first requested.
     */
    private Calendar mUploadTime = new GregorianCalendar();  //TODO needed??
    /**
     * Overwrite destination file?
     */
    private boolean mForceOverwrite;
    /**
     * Create destination folder?
     */
    private boolean mIsCreateRemoteFolder;
    /**
     * Upload only via wifi?
     */
    private boolean mIsUseWifiOnly;
    /**
     * Upload only if phone being charged?
     */
    private boolean mIsWhileChargingOnly;
    /**
     * Earliest time when upload may be started. Negative if not set.
     */
    private long mUploadTimestamp;
    /**
     * Name of Owncloud account to upload file to.
     */
    private String mAccountName;
    /**
     * Status of upload (later, in_progress, ...).
     */
    private UploadStatus mUploadStatus;
    /**
     * Result from last upload operation. Can be null.
     */
    private UploadResult mLastResult;

    // Constructor
    public OCUpload(OCFile ocFile) {
        this.mFile = ocFile;
        resetData();     // TODO needed???

    }

    // TODO needed???
    private void resetData(){
        mId = -1;
        mLocalAction = FileUploadService.LOCAL_BEHAVIOUR_COPY;
        mUploadTime = new GregorianCalendar();
        mForceOverwrite = false;
        mIsCreateRemoteFolder = false;
        mIsUseWifiOnly = true;
        mIsWhileChargingOnly = false;
        mUploadTimestamp = -1;
        mAccountName = "";
        mUploadStatus = UploadStatus.UPLOAD_LATER;
        mLastResult = UploadResult.UNKNOWN;
    }

    // Getters & Setters
    public void setUploadId(long id) {
        mId = id;
    }
    public long getUploadId() {
        return mId;
    }

    public OCFile getOCFile() {
        return mFile;
    }

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
        setLastResult(UploadResult.UNKNOWN);
    }

    /**
     * @return the lastResult
     */
    public UploadResult getLastResult() {
        return mLastResult;
    }

    /**
     * @param lastResult the lastResult to set
     */
    public void setLastResult(UploadResult lastResult) {
        this.mLastResult = lastResult;
    }


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
    public int getLocalAction() {
        return mLocalAction;
    }

    /**
     * @param localAction the localAction to set
     */
    public void setLocalAction(int localAction) {
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
        try {
            String localPath = getLocalPath() != null ? getLocalPath() : "";
            return localPath + " status:" + getUploadStatus() + " result:" +
                    (getLastResult() == null ? "null" : getLastResult().getValue());
        } catch (NullPointerException e){
            Log_OC.d(TAG, "Exception " + e.toString() );
            return (e.toString());
        }
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


    /****
     *
     */
    public static final Parcelable.Creator<OCUpload> CREATOR = new Parcelable.Creator<OCUpload>() {

        @Override
        public OCUpload createFromParcel(Parcel source) {
            return new OCUpload(source);
        }

        @Override
        public OCUpload[] newArray(int size) {
            return new OCUpload[size];
        }
    };

    /**
     * Reconstruct from parcel
     *
     * @param source The source parcel
     */
    protected OCUpload(Parcel source) {
        readFromParcel(source);
    }

    public void readFromParcel(Parcel source) {
        mId = source.readLong();
        mFile = source.readParcelable(OCFile.class.getClassLoader());
        mLocalAction = source.readInt();
        mForceOverwrite = source.readInt() == 0;
        mIsCreateRemoteFolder = source.readInt() == 0;
        mIsUseWifiOnly = source.readInt() == 0;
        mIsWhileChargingOnly = source.readInt() == 0;
        mUploadTimestamp = source.readLong();
        mAccountName = source.readString();
        try {
            mUploadStatus = UploadStatus.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            mUploadStatus = UploadStatus.UPLOAD_LATER;
        }
        try {
            mLastResult = UploadResult.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            mLastResult = UploadResult.UNKNOWN;
        }
    }


    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeParcelable(mFile, flags);
        dest.writeInt(mLocalAction);
        //dest.writeLong(mUploadTime);
        dest.writeInt(mForceOverwrite ? 1 : 0);
        dest.writeInt(mIsCreateRemoteFolder ? 1 : 0);
        dest.writeInt(mIsUseWifiOnly ? 1 : 0);
        dest.writeInt(mIsWhileChargingOnly ? 1 : 0);
        dest.writeLong(mUploadTimestamp);
        dest.writeString(mAccountName);
        dest.writeString(mUploadStatus.name());
        dest.writeString(((mLastResult == null) ? "" : mLastResult.name()));
    }
}
