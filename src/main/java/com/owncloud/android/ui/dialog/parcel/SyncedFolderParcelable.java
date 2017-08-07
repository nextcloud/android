/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 Andy Scherzinger
 * Copyright (C) 2016 Nextcloud
 * <p>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.dialog.parcel;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.files.services.FileUploader;

/**
 * Parcelable for {@link SyncedFolderDisplayItem} objects to transport them from/to dialog fragments.
 */
public class SyncedFolderParcelable implements Parcelable {
    private String mFolderName;
    private String mLocalPath;
    private String mRemotePath;
    private Boolean mWifiOnly = false;
    private Boolean mChargingOnly = false;
    private Boolean mEnabled = false;
    private Boolean mSubfolderByDate = false;
    private Integer mUploadAction;
    private int mType;
    private long mId;
    private String mAccount;
    private int mSection;

    public SyncedFolderParcelable() {
    }

    public SyncedFolderParcelable(SyncedFolderDisplayItem syncedFolderDisplayItem, int section) {
        mId = syncedFolderDisplayItem.getId();
        mFolderName = syncedFolderDisplayItem.getFolderName();
        mLocalPath = syncedFolderDisplayItem.getLocalPath();
        mRemotePath = syncedFolderDisplayItem.getRemotePath();
        mWifiOnly = syncedFolderDisplayItem.getWifiOnly();
        mChargingOnly = syncedFolderDisplayItem.getChargingOnly();
        mEnabled = syncedFolderDisplayItem.isEnabled();
        mSubfolderByDate = syncedFolderDisplayItem.getSubfolderByDate();
        mType = syncedFolderDisplayItem.getType();
        mAccount = syncedFolderDisplayItem.getAccount();
        mUploadAction = syncedFolderDisplayItem.getUploadAction();
        mSection = section;
    }

    private SyncedFolderParcelable(Parcel read) {
        mId = read.readLong();
        mFolderName = read.readString();
        mLocalPath = read.readString();
        mRemotePath = read.readString();
        mWifiOnly = read.readInt()!= 0;
        mChargingOnly = read.readInt() != 0;
        mEnabled = read.readInt() != 0;
        mSubfolderByDate = read.readInt() != 0;
        mType = read.readInt();
        mAccount = read.readString();
        mUploadAction = read.readInt();
        mSection = read.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeString(mFolderName);
        dest.writeString(mLocalPath);
        dest.writeString(mRemotePath);
        dest.writeInt(mWifiOnly ? 1 : 0);
        dest.writeInt(mChargingOnly ? 1 : 0);
        dest.writeInt(mEnabled ? 1 : 0);
        dest.writeInt(mSubfolderByDate ? 1 : 0);
        dest.writeInt(mType);
        dest.writeString(mAccount);
        dest.writeInt(mUploadAction);
        dest.writeInt(mSection);
    }

    public static final Creator<SyncedFolderParcelable> CREATOR =
            new Creator<SyncedFolderParcelable>() {

                @Override
                public SyncedFolderParcelable createFromParcel(Parcel source) {
                    return new SyncedFolderParcelable(source);
                }

                @Override
                public SyncedFolderParcelable[] newArray(int size) {
                    return new SyncedFolderParcelable[size];
                }
            };

    @Override
    public int describeContents() {
        return 0;
    }

    public String getFolderName() {
        return mFolderName;
    }

    public void setFolderName(String mFolderName) {
        this.mFolderName = mFolderName;
    }

    public String getLocalPath() {
        return mLocalPath;
    }

    public void setLocalPath(String mLocalPath) {
        this.mLocalPath = mLocalPath;
    }

    public String getRemotePath() {
        return mRemotePath;
    }

    public void setRemotePath(String mRemotePath) {
        this.mRemotePath = mRemotePath;
    }

    public Boolean getWifiOnly() {
        return mWifiOnly;
    }

    public void setWifiOnly(Boolean mWifiOnly) {
        this.mWifiOnly = mWifiOnly;
    }

    public Boolean getChargingOnly() {
        return mChargingOnly;
    }

    public void setChargingOnly(Boolean mChargingOnly) {
        this.mChargingOnly = mChargingOnly;
    }

    public Boolean getEnabled() {
        return mEnabled;
    }

    public void setEnabled(boolean mEnabled) {
        this.mEnabled = mEnabled;
    }

    public Boolean getSubfolderByDate() {
        return mSubfolderByDate;
    }

    public void setSubfolderByDate(Boolean mSubfolderByDate) {
        this.mSubfolderByDate = mSubfolderByDate;
    }

    public int getType() {
        return mType;
    }

    public void setType(int mType) {
        this.mType = mType;
    }

    public Integer getUploadAction() {
        return mUploadAction;
    }

    public Integer getUploadActionInteger() {
        switch (mUploadAction) {
            case FileUploader.LOCAL_BEHAVIOUR_FORGET:
                return 0;
            case FileUploader.LOCAL_BEHAVIOUR_MOVE:
                return 1;
            case FileUploader.LOCAL_BEHAVIOUR_DELETE:
                return 2;
        }
        return 0;
    }

    public void setUploadAction(String mUploadAction) {
        switch (mUploadAction) {
            case "LOCAL_BEHAVIOUR_FORGET":
                this.mUploadAction = FileUploader.LOCAL_BEHAVIOUR_FORGET;
                break;
            case "LOCAL_BEHAVIOUR_MOVE":
                this.mUploadAction = FileUploader.LOCAL_BEHAVIOUR_MOVE;
                break;
            case "LOCAL_BEHAVIOUR_DELETE":
                this.mUploadAction = FileUploader.LOCAL_BEHAVIOUR_DELETE;
                break;
        }
    }

    public long getId() {
        return mId;
    }

    public void setId(long mId) {
        this.mId = mId;
    }

    public String getAccount() {
        return mAccount;
    }

    public void setAccount(String mAccount) {
        this.mAccount = mAccount;
    }

    public int getSection() {
        return mSection;
    }

    public void setSection(int mSection) {
        this.mSection = mSection;
    }
}
