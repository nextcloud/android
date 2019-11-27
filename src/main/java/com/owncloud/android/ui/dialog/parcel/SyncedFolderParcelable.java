/*
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

import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.files.services.FileUploader;

import lombok.NoArgsConstructor;

/**
 * Parcelable for {@link SyncedFolderDisplayItem} objects to transport them from/to dialog fragments.
 */
@NoArgsConstructor
public class SyncedFolderParcelable implements Parcelable {
    private String folderName;
    private String localPath;
    private String remotePath;
    private Boolean wifiOnly = false;
    private Boolean chargingOnly = false;
    private Boolean enabled = false;
    private Boolean subfolderByDate = false;
    private Integer uploadAction;
    private MediaFolderType type;
    private Boolean hidden = false;
    private long id;
    private String account;
    private int section;

    public SyncedFolderParcelable(SyncedFolderDisplayItem syncedFolderDisplayItem, int section) {
        id = syncedFolderDisplayItem.getId();
        folderName = syncedFolderDisplayItem.getFolderName();
        localPath = syncedFolderDisplayItem.getLocalPath();
        remotePath = syncedFolderDisplayItem.getRemotePath();
        wifiOnly = syncedFolderDisplayItem.getWifiOnly();
        chargingOnly = syncedFolderDisplayItem.getChargingOnly();
        enabled = syncedFolderDisplayItem.isEnabled();
        subfolderByDate = syncedFolderDisplayItem.getSubfolderByDate();
        type = syncedFolderDisplayItem.getType();
        account = syncedFolderDisplayItem.getAccount();
        uploadAction = syncedFolderDisplayItem.getUploadAction();
        this.section = section;
        hidden = syncedFolderDisplayItem.getHidden();
    }

    private SyncedFolderParcelable(Parcel read) {
        id = read.readLong();
        folderName = read.readString();
        localPath = read.readString();
        remotePath = read.readString();
        wifiOnly = read.readInt()!= 0;
        chargingOnly = read.readInt() != 0;
        enabled = read.readInt() != 0;
        subfolderByDate = read.readInt() != 0;
        type = MediaFolderType.getById(read.readInt());
        account = read.readString();
        uploadAction = read.readInt();
        section = read.readInt();
        hidden = read.readInt() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(folderName);
        dest.writeString(localPath);
        dest.writeString(remotePath);
        dest.writeInt(wifiOnly ? 1 : 0);
        dest.writeInt(chargingOnly ? 1 : 0);
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(subfolderByDate ? 1 : 0);
        dest.writeInt(type.getId());
        dest.writeString(account);
        dest.writeInt(uploadAction);
        dest.writeInt(section);
        dest.writeInt(hidden ? 1 : 0);
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
        return folderName;
    }

    public void setFolderName(String mFolderName) {
        this.folderName = mFolderName;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String mLocalPath) {
        this.localPath = mLocalPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String mRemotePath) {
        this.remotePath = mRemotePath;
    }

    public Boolean getWifiOnly() {
        return wifiOnly;
    }

    public void setWifiOnly(Boolean mWifiOnly) {
        this.wifiOnly = mWifiOnly;
    }

    public Boolean getChargingOnly() {
        return chargingOnly;
    }

    public void setChargingOnly(Boolean mChargingOnly) {
        this.chargingOnly = mChargingOnly;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean mEnabled) {
        this.enabled = mEnabled;
    }

    public Boolean getSubfolderByDate() {
        return subfolderByDate;
    }

    public void setSubfolderByDate(Boolean mSubfolderByDate) {
        this.subfolderByDate = mSubfolderByDate;
    }

    public MediaFolderType getType() {
        return type;
    }

    public void setType(MediaFolderType mType) {
        this.type = mType;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(boolean mHidden) {
        this.hidden = mHidden;
    }

    public Integer getUploadAction() {
        return uploadAction;
    }

    public Integer getUploadActionInteger() {
        switch (uploadAction) {
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
                this.uploadAction = FileUploader.LOCAL_BEHAVIOUR_FORGET;
                break;
            case "LOCAL_BEHAVIOUR_MOVE":
                this.uploadAction = FileUploader.LOCAL_BEHAVIOUR_MOVE;
                break;
            case "LOCAL_BEHAVIOUR_DELETE":
                this.uploadAction = FileUploader.LOCAL_BEHAVIOUR_DELETE;
                break;
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public int getSection() {
        return section;
    }

    public void setSection(int section) {
        this.section = section;
    }
}
