/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016 Andy Scherzinger
 * SPDX-FileCopyrightText: 2016 Nextcloud
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog.parcel;

import android.os.Parcel;
import android.os.Parcelable;

import com.nextcloud.client.jobs.upload.FileUploadWorker;
import com.nextcloud.client.preferences.SubFolderRule;
import com.owncloud.android.datamodel.MediaFolderType;
import com.owncloud.android.datamodel.SyncedFolderDisplayItem;
import com.owncloud.android.files.services.NameCollisionPolicy;

/**
 * Parcelable for {@link SyncedFolderDisplayItem} objects to transport them from/to dialog fragments.
 */
public class SyncedFolderParcelable implements Parcelable {
    private String folderName;
    private String localPath;
    private String remotePath;
    private boolean wifiOnly = false;
    private boolean chargingOnly = false;
    private boolean existing = true;
    private boolean enabled = false;
    private boolean subfolderByDate = false;
    private Integer uploadAction;
    private NameCollisionPolicy nameCollisionPolicy = NameCollisionPolicy.ASK_USER;
    private MediaFolderType type;
    private boolean hidden = false;
    private long id;
    private String account;
    private int section;
    private SubFolderRule subFolderRule;
    private boolean excludeHidden;
    private long uploadMinFileAgeMs;

    public SyncedFolderParcelable(SyncedFolderDisplayItem syncedFolderDisplayItem, int section) {
        id = syncedFolderDisplayItem.getId();
        folderName = syncedFolderDisplayItem.getFolderName();
        localPath = syncedFolderDisplayItem.getLocalPath();
        remotePath = syncedFolderDisplayItem.getRemotePath();
        wifiOnly = syncedFolderDisplayItem.isWifiOnly();
        chargingOnly = syncedFolderDisplayItem.isChargingOnly();
        existing = syncedFolderDisplayItem.isExisting();
        enabled = syncedFolderDisplayItem.isEnabled();
        subfolderByDate = syncedFolderDisplayItem.isSubfolderByDate();
        type = syncedFolderDisplayItem.getType();
        account = syncedFolderDisplayItem.getAccount();
        uploadAction = syncedFolderDisplayItem.getUploadAction();
        nameCollisionPolicy = NameCollisionPolicy.deserialize(
            syncedFolderDisplayItem.getNameCollisionPolicyInt());
        uploadMinFileAgeMs = syncedFolderDisplayItem.getUploadMinFileAgeMs();
        this.section = section;
        hidden = syncedFolderDisplayItem.isHidden();
        subFolderRule = syncedFolderDisplayItem.getSubfolderRule();
        excludeHidden = syncedFolderDisplayItem.isExcludeHidden();
    }

    private SyncedFolderParcelable(Parcel read) {
        id = read.readLong();
        folderName = read.readString();
        localPath = read.readString();
        remotePath = read.readString();
        wifiOnly = read.readInt()!= 0;
        chargingOnly = read.readInt() != 0;
        existing = read.readInt() != 0;
        enabled = read.readInt() != 0;
        subfolderByDate = read.readInt() != 0;
        type = MediaFolderType.getById(read.readInt());
        account = read.readString();
        uploadAction = read.readInt();
        nameCollisionPolicy = NameCollisionPolicy.deserialize(read.readInt());
        section = read.readInt();
        hidden = read.readInt() != 0;
        subFolderRule = SubFolderRule.values()[read.readInt()];
        excludeHidden = read.readInt() != 0;
        uploadMinFileAgeMs = read.readLong();
    }

    public SyncedFolderParcelable() {
        // empty constructor
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(folderName);
        dest.writeString(localPath);
        dest.writeString(remotePath);
        dest.writeInt(wifiOnly ? 1 : 0);
        dest.writeInt(chargingOnly ? 1 : 0);
        dest.writeInt(existing ? 1 : 0);
        dest.writeInt(enabled ? 1 : 0);
        dest.writeInt(subfolderByDate ? 1 : 0);
        dest.writeInt(type.id);
        dest.writeString(account);
        dest.writeInt(uploadAction);
        dest.writeInt(nameCollisionPolicy.serialize());
        dest.writeInt(section);
        dest.writeInt(hidden ? 1 : 0);
        dest.writeInt(subFolderRule.ordinal());
        dest.writeInt(excludeHidden ? 1 : 0);
        dest.writeLong(uploadMinFileAgeMs);
    }

    public static final Creator<SyncedFolderParcelable> CREATOR =
            new Creator<>() {

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

    public Integer getUploadActionInteger() {
        return switch (uploadAction) {
            case FileUploadWorker.LOCAL_BEHAVIOUR_FORGET -> 0;
            case FileUploadWorker.LOCAL_BEHAVIOUR_MOVE -> 1;
            case FileUploadWorker.LOCAL_BEHAVIOUR_DELETE -> 2;
            default -> 0;
        };
    }

    public void setUploadAction(String uploadAction) {
        switch (uploadAction) {
            case "LOCAL_BEHAVIOUR_FORGET":
                this.uploadAction = FileUploadWorker.LOCAL_BEHAVIOUR_FORGET;
                break;
            case "LOCAL_BEHAVIOUR_MOVE":
                this.uploadAction = FileUploadWorker.LOCAL_BEHAVIOUR_MOVE;
                break;
            case "LOCAL_BEHAVIOUR_DELETE":
                this.uploadAction = FileUploadWorker.LOCAL_BEHAVIOUR_DELETE;
                break;
            default:
                // do nothing
                break;
        }
    }

    public String getFolderName() {
        return this.folderName;
    }

    public String getLocalPath() {
        return this.localPath;
    }

    public String getRemotePath() {
        return this.remotePath;
    }

    public boolean isWifiOnly() {
        return this.wifiOnly;
    }

    public boolean isChargingOnly() {
        return this.chargingOnly;
    }

    public boolean isExisting() {
        return this.existing;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isSubfolderByDate() {
        return this.subfolderByDate;
    }

    public long getUploadMinFileAgeMs() {
        return uploadMinFileAgeMs;
    }

    public Integer getUploadAction() {
        return this.uploadAction;
    }

    public NameCollisionPolicy getNameCollisionPolicy() {
        return this.nameCollisionPolicy;
    }

    public MediaFolderType getType() {
        return this.type;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public long getId() {
        return this.id;
    }

    public String getAccount() {
        return this.account;
    }

    public int getSection() {
        return this.section;
    }

    public SubFolderRule getSubFolderRule() { return this.subFolderRule; }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public void setWifiOnly(boolean wifiOnly) {
        this.wifiOnly = wifiOnly;
    }

    public void setChargingOnly(boolean chargingOnly) {
        this.chargingOnly = chargingOnly;
    }

    public void setExisting(boolean existing) {
        this.existing = existing;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setSubfolderByDate(boolean subfolderByDate) {
        this.subfolderByDate = subfolderByDate;
    }

    public void setUploadMinFileAgeMs(long uploadMinFileAgeMs) {
        this.uploadMinFileAgeMs = uploadMinFileAgeMs;
    }

    public void setNameCollisionPolicy(NameCollisionPolicy nameCollisionPolicy) {
        this.nameCollisionPolicy = nameCollisionPolicy;
    }

    public void setType(MediaFolderType type) {
        this.type = type;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public void setSection(int section) {
        this.section = section;
    }
    public void setSubFolderRule(SubFolderRule subFolderRule) { this.subFolderRule = subFolderRule; }

    public boolean isExcludeHidden() {
        return excludeHidden;
    }

    public void setExcludeHidden(boolean excludeHidden) {
        this.excludeHidden = excludeHidden;
    }
}
