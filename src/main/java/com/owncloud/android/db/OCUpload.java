/*
 * ownCloud Android client application
 *
 * @author LukeOwncloud
 * @author masensio
 * @author David A. Velasco
 * @author Tobias Kaminsky
 * Copyright (C) 2016 ownCloud Inc.
 * Copyright (C) 2018 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.db;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;

import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.UploadsStorageManager;
import com.owncloud.android.datamodel.UploadsStorageManager.UploadStatus;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.MimeTypeUtil;

import java.io.File;

import lombok.Getter;
import lombok.Setter;

/**
 * Stores all information in order to start upload operations. PersistentUploadObject can
 * be stored persistently by {@link UploadsStorageManager}.
 */
public class OCUpload implements Parcelable {

    private static final String TAG = OCUpload.class.getSimpleName();

    @Getter @Setter private long uploadId;

    /**
     * Absolute path in the local file system to the file to be uploaded.
     */
    @Getter @Setter private String localPath;

    /**
     * Absolute path in the remote account to set to the uploaded file (not for its parent folder!)
     */
    @Getter @Setter private String remotePath;

    /**
     * Name of Owncloud account to upload file to.
     */
    @Getter private String accountName;

    /**
     * File size.
     */
    @Getter @Setter private long fileSize;

    /**
     * Local action for upload. (0 - COPY, 1 - MOVE, 2 - FORGET)
     */
    @Getter @Setter private int localAction;

    /**
     * Overwrite destination file?
     */
    @Getter @Setter private boolean forceOverwrite;

    /**
     * Create destination folder?
     */
    @Getter @Setter private boolean createRemoteFolder;

    /**
     * Status of upload (later, in_progress, ...).
     */
    @Getter private UploadStatus uploadStatus;

    /**
     * Result from last upload operation. Can be null.
     */
    @Getter private UploadResult lastResult;

    /**
     * Defines the origin of the upload; see constants CREATED_ in {@link UploadFileOperation}
     */
    @Getter @Setter private int createdBy;

    /**
     * When the upload ended
     */
    @Getter @Setter private long uploadEndTimestamp;

    /**
     * Upload only via wifi?
     */
    @Getter @Setter private boolean useWifiOnly;

    /**
     * Upload only if phone being charged?
     */
    @Getter @Setter private boolean whileChargingOnly;

    /**
     * Token to unlock E2E folder
     */
    @Getter @Setter private String folderUnlockToken;

    /**
     * temporary values, used for sorting
     */
    @Getter private UploadStatus fixedUploadStatus;
    @Getter private boolean fixedUploadingNow;
    @Getter private long fixedUploadEndTimeStamp;
    @Getter private long fixedUploadId;

    /**
     * Main constructor.
     *
     * @param localPath         Absolute path in the local file system to the file to be uploaded.
     * @param remotePath        Absolute path in the remote account to set to the uploaded file.
     * @param accountName       Name of an ownCloud account to update the file to.
     */
    public OCUpload(String localPath, String remotePath, String accountName) {
        if (localPath == null || !localPath.startsWith(File.separator)) {
            throw new IllegalArgumentException("Local path must be an absolute path in the local file system");
        }
        if (remotePath == null || !remotePath.startsWith(OCFile.PATH_SEPARATOR)) {
            throw new IllegalArgumentException("Remote path must be an absolute path in the local file system");
        }
        if (accountName == null || accountName.length() < 1) {
            throw new IllegalArgumentException("Invalid account name");
        }
        resetData();
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.accountName = accountName;
    }

    /**
     * Convenience constructor to re-upload already existing {@link OCFile}s.
     *
     * @param  ocFile           {@link OCFile} instance to update in the remote server.
     * @param  account          ownCloud {@link Account} where ocFile is contained.
     */
    public OCUpload(OCFile ocFile, Account account) {
        this(ocFile.getStoragePath(), ocFile.getRemotePath(), account.name);
    }

    /**
     * Reset all the fields to default values.
     */
    private void resetData() {
        remotePath = "";
        localPath = "";
        accountName = "";
        fileSize = -1;
        uploadId = -1;
        localAction = FileUploader.LOCAL_BEHAVIOUR_COPY;
        forceOverwrite = false;
        createRemoteFolder = false;
        uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS;
        lastResult = UploadResult.UNKNOWN;
        createdBy = UploadFileOperation.CREATED_BY_USER;
        useWifiOnly = true;
        whileChargingOnly = false;
        folderUnlockToken = "";
    }

    public void setDataFixed(FileUploader.FileUploaderBinder binder) {
        fixedUploadStatus = uploadStatus;
        fixedUploadingNow = binder != null && binder.isUploadingNow(this);
        fixedUploadEndTimeStamp = uploadEndTimestamp;
        fixedUploadId = uploadId;
    }

    // custom Getters & Setters
    /**
     * Sets uploadStatus AND SETS lastResult = null;
     * @param uploadStatus the uploadStatus to set
     */
    public void setUploadStatus(UploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
        setLastResult(UploadResult.UNKNOWN);
    }

    /**
     * @param lastResult the lastResult to set
     */
    public void setLastResult(UploadResult lastResult) {
        this.lastResult = lastResult != null ? lastResult : UploadResult.UNKNOWN;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return MimeTypeUtil.getBestMimeTypeByFilename(localPath);
    }

    /**
     * Returns owncloud account as {@link Account} object.
     */
    public Account getAccount(UserAccountManager accountManager) {
        return accountManager.getAccountByName(getAccountName());
    }

    /**
     * For debugging purposes only.
     */
    public String toFormattedString() {
        try {
            String localPath = getLocalPath() != null ? getLocalPath() : "";
            return localPath + " status:" + getUploadStatus() + " result:" +
                    (getLastResult() == null ? "null" : getLastResult().getValue());
        } catch (NullPointerException e) {
            Log_OC.d(TAG, "Exception " + e.toString());
            return e.toString();
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
    private OCUpload(Parcel source) {
        readFromParcel(source);
    }

    private void readFromParcel(Parcel source) {
        uploadId = source.readLong();
        localPath = source.readString();
        remotePath = source.readString();
        accountName = source.readString();
        localAction = source.readInt();
        forceOverwrite = source.readInt() == 1;
        createRemoteFolder = source.readInt() == 1;
        try {
            uploadStatus = UploadStatus.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            uploadStatus = UploadStatus.UPLOAD_IN_PROGRESS;
        }
        uploadEndTimestamp = source.readLong();
        try {
            lastResult = UploadResult.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            lastResult = UploadResult.UNKNOWN;
        }
        createdBy = source.readInt();
        useWifiOnly = source.readInt() == 1;
        whileChargingOnly = source.readInt() == 1;
        folderUnlockToken = source.readString();
    }

    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(uploadId);
        dest.writeString(localPath);
        dest.writeString(remotePath);
        dest.writeString(accountName);
        dest.writeInt(localAction);
        dest.writeInt(forceOverwrite ? 1 : 0);
        dest.writeInt(createRemoteFolder ? 1 : 0);
        dest.writeString(uploadStatus.name());
        dest.writeLong(uploadEndTimestamp);
        dest.writeString(lastResult == null ? "" : lastResult.name());
        dest.writeInt(createdBy);
        dest.writeInt(useWifiOnly ? 1 : 0);
        dest.writeInt(whileChargingOnly ? 1 : 0);
        dest.writeString(folderUnlockToken);
    }

    enum CanUploadFileNowStatus {NOW, LATER, FILE_GONE, ERROR}
}
